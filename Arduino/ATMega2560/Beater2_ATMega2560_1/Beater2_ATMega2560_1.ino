#include <Arduino.h>

/*  First initial sketch for Motor scheduling
    Timer 1 used for scheduling stepper step
    Beater Object represent stepper mottor
    Timer interupts updates beater object on evry interupt with modes are active
    after completion of every stepper beat object updates beater object for next beat
*/
#define BUFFER_SIZE 200
#define MAX_STEP_IN_SWING 100
boolean currentDeviceStatus = false;

int timeInterval[BUFFER_SIZE] = {0};// stores time interval im millis for next beat
int beatAmplitude[BUFFER_SIZE] = {0};// stores accelearation value for next beat
int drumNumber[BUFFER_SIZE] = {0};// stores beater object number
int FRAME_LENGTH = BUFFER_SIZE;
int STREAM_LENGTH = BUFFER_SIZE;
int lastHalfSwingTime = 0;
int lastUpdatedIndex;
int lastRequestedIndex = 0;
boolean fillBuffer = false;
int SWING_TIME = 70;
int interruptCounter = 0;
int schedulerTimerCounter = 0;
int currentBeat = 0;// pointer to current beat
boolean isStreaming = false;



class Beater
{
  private:
    int mcurrentStepNumber = 0;///stores current step number for motor
    volatile uint8_t *beaterPort;
    volatile uint16_t *timerCounterRegister;
    volatile uint16_t *timerCompareRegister;

  public:
    boolean state = false;  //state "true" for active beat, "false" for motor spleep
    int numberOfStepsInSwing = 16; //number of step in swing
    int numberOfStepsInHalfSwing = numberOfStepsInSwing / 2;
    int currentStepNumber = 0;   // current step count for swing
    int upSwingTime = 30;  // current steptime in interupt frequency, decided based on minimum time gape 90 milli, value corresponds to first step
    int currentbeatAmplitude = 5; // current amplitude in final step time to decide mottor speed, value corresponds to last step
    int currentTimeCounter = 0; // interupt counter
    int currentPlayingBeat = 0; //current beat number which is being played by this beater
    int drummerId = 0;
    int startStepTime = 3000 ; //micro seconds
    int reverseStepTime = 3000 ;
    int minimumStepTime = 10;
    int stepIntervalStriking[MAX_STEP_IN_SWING] = {10};// stores time interval im millis for next beat
    int stepIntervalNonStriking[MAX_STEP_IN_SWING] = {10};// stores accelearation value for next beat

    Beater(int id , volatile uint8_t *portAddress, volatile uint8_t *portAddressDDR, int numberOfSteps, volatile uint16_t *_timeCounterRegister, volatile uint16_t *_timeCompareRegister )  {
      drummerId = id;
      numberOfStepsInSwing = numberOfSteps;
      numberOfStepsInHalfSwing = numberOfStepsInSwing / 2;
      beaterPort = portAddress;
      *portAddressDDR |= 0b11111111 ;
      timerCounterRegister = _timeCounterRegister;
      timerCompareRegister = _timeCompareRegister;
    }

    void setStepIntervalForAmplitude(int currentBeatAmplitude)
    {
      for (int stepNumber = 0; stepNumber < numberOfStepsInHalfSwing; stepNumber++) {
        stepIntervalStriking[stepNumber] = startStepTime - ((double)(startStepTime - currentBeatAmplitude) * (double)(stepNumber)) / numberOfStepsInHalfSwing; // 1 equals to 4 micro second
        stepIntervalNonStriking[stepNumber] = reverseStepTime;
        // currentTimeCounter = startStepTime - (((startStepTime - currentbeatAmplitude) * (currentStepNumber % (numberOfStepInHalfSwing))) / (numberOfStepInHalfSwing));

        //        stepIntervalStriking[stepNumber] = startStepTime;
        //        stepIntervalNonStriking[stepNumber] = startStepTime;

      }
      return;
    }

    void updatecounter() {
      if (state) {
        currentTimeCounter--;
        if (currentTimeCounter > 0) {
          //          return ;
        }

        if (currentStepNumber < numberOfStepsInHalfSwing) {
          //          currentTimeCounter = startStepTime - (((startStepTime - currentbeatAmplitude) * (currentStepNumber % (numberOfStepInHalfSwing))) / (numberOfStepInHalfSwing));
          //currentTimeCounter =  startStepTime;
          currentTimeCounter = stepIntervalStriking[currentStepNumber % numberOfStepsInHalfSwing] ;

        }
        else {
          currentTimeCounter = stepIntervalNonStriking[currentStepNumber % numberOfStepsInHalfSwing] ; // TODO add acceleration value
        }
        if (currentTimeCounter < 2) {
          currentTimeCounter = 2;
        }

        setNextInterval(currentTimeCounter);
        currentStepNumber++;

        if (currentStepNumber > numberOfStepsInSwing + 1 ) {
          state = false;
          setNextInterval(10000);
          setReset();
        }
        else if (currentStepNumber == ( numberOfStepsInSwing + 1)  ) {
          clearReset();
        }
        else {
          if (currentStepNumber <= numberOfStepsInHalfSwing) {
            halfstep(1);
          }
          else {
            halfstep(0);
          }
        }
      }
    }

    void setReset() {
      *beaterPort |= 0b00000100;
      return;
    }

    void clearReset() {
      *beaterPort &= 0b11111011;
      return;
    }

    void setNextInterval(int nextStepInterval) {
      *timerCounterRegister  = 0;
      *timerCompareRegister = (nextStepInterval * 2) - 1;
      return;
    }

    void disablemotor() {
      *beaterPort = 0b00000000;
    }

    void halfstep(int dir) {
      switch (dir) {
        case 0:
          *beaterPort &= 0b11111101;
          *beaterPort ^= 0b00000001;
          break;
        case 1:
          *beaterPort |= 0b00000010;
          *beaterPort ^= 0b00000001;
          break;
        default:
          break;
      }
    }
    void setBeat(int beatToLoad) {

      if (state) {
        if (currentStepNumber > ((numberOfStepsInHalfSwing) + 2)) {
          currentStepNumber = numberOfStepsInSwing - currentStepNumber;
          currentbeatAmplitude = minimumStepTime + ( (double)(100 - (beatAmplitude[beatToLoad % FRAME_LENGTH])) * (double)(startStepTime - minimumStepTime) / 100); ///TODO: amplitude to motor steptime map
          setStepIntervalForAmplitude(currentbeatAmplitude);
          currentTimeCounter = stepIntervalStriking[currentStepNumber];
          setNextInterval(currentTimeCounter);
          currentPlayingBeat = beatToLoad;
          setReset();
          state = true;
        }
        return;
      }
      currentStepNumber = 0;
      currentbeatAmplitude = minimumStepTime + ( (double)(100 - beatAmplitude[beatToLoad % FRAME_LENGTH]) * (double)(startStepTime - minimumStepTime)) / 100; ///TODO: amplitude to motor steptime map
      setStepIntervalForAmplitude(currentbeatAmplitude);
      currentTimeCounter = stepIntervalStriking[currentStepNumber];
      setNextInterval(currentTimeCounter);
      currentPlayingBeat = beatToLoad;
      setReset();
      state = true;
    }
};

int numberofStepsForBeaterOne = 24;
int numberofStepsForBeaterTwo = 24;
int numberofStepsForBeaterThree = 32;

int drumIdBeaterOne = 0;
int drumIdBeaterTwo = 1;
int drumIdBeaterThree = 2;

Beater beater1 = Beater(drumIdBeaterOne, &PORTF, &DDRF, numberofStepsForBeaterOne, &TCNT3 , &OCR3A); //define beater
Beater beater2 = Beater(drumIdBeaterTwo, &PORTK, &DDRK, numberofStepsForBeaterTwo, &TCNT4 , &OCR4A);
Beater beater3 = Beater(drumIdBeaterThree, &PORTA, &DDRA, numberofStepsForBeaterThree, &TCNT5 , &OCR5A);

Beater beaters[] = {beater1, beater2, beater3};

void stopPlayingDevice() {
  int numberOfBeaters = (sizeof(beaters) / sizeof(Beater));
  for (int beaterCounter = 0; beaterCounter < numberOfBeaters; beaterCounter++)
  {
    beaters[beaterCounter].state = false;
    beaters[beaterCounter].disablemotor();
  }
}

void changeDeviceStatus(boolean deviceStatus) {
  currentDeviceStatus = deviceStatus;
  sendDebug("STATE: ", "" + currentDeviceStatus);
  if (currentDeviceStatus)
  {
    return;
  }
  stopPlayingDevice();
}

int getIntegerFromTwoBytes(byte readIndexByteOne, byte readIndexByteTwo) {
  return readIndexByteOne + readIndexByteTwo * 128;
}

void setupTimmerForInterrupts() {
  // initialize timer1 interupt at every 1000micro sec or 1 milli sec
  //set timer1 interrupt at 1000Hz
  cli();//stop interrupts
  TCCR1A = 0;// set entire TCCR1A register to 0
  TCCR1B = 0;// same for TCCR1B
  TCNT1  = 0;//initialize counter value to 0
  // set compare match register for 1000hz increments
  // calculation for 1024 prescalar 15.625-1
  // @ 256 62.5-1
  //@64 250-1
  OCR1A = (16000  - 1);

  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS10 and CS11 bits for 1 prescaler
  TCCR1B |= (1 << CS10);
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);

  // TIMER 3

  TCCR3A = 0;// set entire TCCR1A register to 0
  TCCR3B = 0;// same for TCCR1B
  TCNT3  = 0;//initialize counter value to 0
  // set compare match register for 1000hz increments
  // calculation for 1024 prescalar 15.625-1
  // @ 256 62.5-1
  //@64 250-1
  OCR3A = (((16000 / 8) * 10) - 1);//For 10 millisec and prescalar 8

  // turn on CTC mode
  TCCR3B |= (1 << WGM32);
  // Set CS10 and CS11 bits for 1 prescaler
  TCCR3B |= (1 << CS31); //Prescalar = 8
  // enable timer compare interrupt
  TIMSK3 |= (1 << OCIE3A);

  // TIMER 4

  TCCR4A = 0;// set entire TCCR1A register to 0
  TCCR4B = 0;// same for TCCR1B
  TCNT4  = 0;//initialize counter value to 0
  // set compare match register for 1000hz increments
  // calculation for 1024 prescalar 15.625-1
  // @ 256 62.5-1
  //@64 250-1
  OCR4A = (((16000 / 8) * 10) - 1);//For 10 millisec and prescalar 8

  // turn on CTC mode
  TCCR4B |= (1 << WGM42);
  // Set CS10 and CS11 bits for 1 prescaler
  TCCR4B |= (1 << CS41); //Prescalar = 8
  // enable timer compare interrupt
  TIMSK4 |= (1 << OCIE4A);

  // TIMER 5

  TCCR5A = 0;// set entire TCCR1A register to 0
  TCCR5B = 0;// same for TCCR1B
  TCNT5  = 0;//initialize counter value to 0
  // set compare match register for 1000hz increments
  // calculation for 1024 prescalar 15.625-1
  // @ 256 62.5-1
  //@64 250-1
  OCR5A = (((16000 / 8) * 10) - 1);//For 10 millisec and prescalar 8

  // turn on CTC mode
  TCCR5B |= (1 << WGM52);
  // Set CS10 and CS11 bits for 1 prescaler
  TCCR5B |= (1 << CS51); //Prescalar = 8
  // enable timer compare interrupt
  TIMSK5 |= (1 << OCIE5A);

  sei();
}

void setup() {
  for (byte bufferIndexCounter = 0; bufferIndexCounter < BUFFER_SIZE; bufferIndexCounter++) {
    timeInterval[bufferIndexCounter] = 1000 + (bufferIndexCounter % 5) * 0;
    beatAmplitude[bufferIndexCounter] = 2;
    drumNumber[bufferIndexCounter] = 0;
  }
  Serial.begin(9600);
  Serial1.begin(9600);
  setupTimmerForInterrupts();
}

void requestBeat(int index) {
  Serial1.print('G');
  Serial1.print('B');
  byte resultZero = (byte) (index % 128);
  byte resultOne = (byte) (index / 128);
  Serial1.write(resultZero);
  Serial1.write(resultOne);
  Serial1.print('\n');
  return;
}

char readNextCharacterFromSerial() {
  while (Serial1.available() == 0) {}
  return (char)Serial1.read();
}

byte readNextByteFromSerial() {
  while (Serial1.available() == 0) {}
  return Serial1.read();
}

int readNextIntegerFromSerial() {
  byte readIndexByteOne = readNextByteFromSerial();
  byte readIndexByteTwo = readNextByteFromSerial();

  return getIntegerFromTwoBytes(readIndexByteOne, readIndexByteTwo);
}

void sendCommandToSerial(char commandOne, char commandTwo, byte byteToSend) {
  Serial1.print(commandOne);
  Serial1.print(commandTwo);
  Serial1.write(byteToSend);
  Serial1.print('\n');

  sendDebug("Serial out: ", "");
  sendDebug("", "" + commandOne);
  sendDebug("", "" + commandTwo);
  sendDebug("", "" + byteToSend);
}

void sendCommandToSerial(char commandOne, char commandTwo, byte byteOneToSend, byte byteTwoToSend) {
  Serial1.print(commandOne);
  Serial1.print(commandTwo);
  Serial1.write(byteOneToSend);
  Serial1.write(byteTwoToSend);
  Serial1.print('\n');

  sendDebug("Serial out: ", "");
  sendDebug("", "" + commandOne);
  sendDebug("", "" + commandTwo);
  sendDebug("", "" + byteOneToSend);
  sendDebug("", "" + byteTwoToSend);
}

int updateBufferWithBeatFromSerial() {
  int index = readNextIntegerFromSerial();
  int timeForNextBeat = readNextIntegerFromSerial();
  int amplitudeForNextBeat = readNextIntegerFromSerial();
  int drumNumberForNextBeat = readNextIntegerFromSerial();
  timeInterval[index % FRAME_LENGTH] = timeForNextBeat;
  beatAmplitude[index % FRAME_LENGTH] = amplitudeForNextBeat;
  drumNumber[index % FRAME_LENGTH] = drumNumberForNextBeat;
  Serial.print("-");
  Serial.print(index);
  Serial.print(", ");
  Serial.println(drumNumberForNextBeat);
  lastUpdatedIndex = index;
  return index;
}

void performOperationsIfTheBufferIsNotFilled(int index) {
  if (fillBuffer)
  {
    if (index == FRAME_LENGTH - 1)
    {
      fillBuffer = false;
//      sendCommandToSerial('S', 'R', true);
      stopPlayingDevice();
      return;
    }
    int nextIndex = index + 1;
    byte resultZero = (byte) (nextIndex % 128);
    byte resultOne = (byte) (nextIndex / 128);
    sendCommandToSerial('G', 'B', resultZero, resultOne);
  }
}

void performOperationsIfStreaming() {
  if (lastUpdatedIndex - currentBeat < FRAME_LENGTH -2 )
  {
    if (lastRequestedIndex <= lastUpdatedIndex)
    {
      requestBeat(lastUpdatedIndex + 1) ;
      lastRequestedIndex = lastUpdatedIndex + 1;
    }
  }
}


void loop() {
  sei();

  if (isStreaming && !fillBuffer)
  {
    performOperationsIfStreaming();
  }

  while (Serial1.available()) {
    char inChar = readNextCharacterFromSerial();
    if (inChar == 'G') {
      char commandChar = readNextCharacterFromSerial();
      if (commandChar == 'B')
      {
        sendDebug("Serial IN: G B ", "");
        sendCommandToSerial('S', 'B', currentBeat);
      }
      else if (commandChar == 'S')
      {

        sendDebug("Serial IN: G S ", "");
        sendCommandToSerial('S', 'S', currentDeviceStatus);
      }
    }

    else if (inChar == 'S')
    {
      char commandChar = readNextCharacterFromSerial();
      if (commandChar == 'B')
      {
        sendDebug("Serial IN: S B ", "");
        int index = updateBufferWithBeatFromSerial();
        performOperationsIfTheBufferIsNotFilled(index);
        
      }
      else if (commandChar == 'S')
      {
        boolean desiredState;
        char inputChar = readNextCharacterFromSerial();
        sendDebug("inputChar :", "" + inputChar);
        if (inputChar == 'F')
        {
          desiredState = false;
        }
        else if ( inputChar == 'T')
        {
          desiredState = true;
        }
        sendDebug("Serial IN: S S desired :", "" + desiredState);
        changeDeviceStatus(desiredState);
        if (currentDeviceStatus)
        {
          sendCommandToSerial('S', 'S', 'T');

        }
        else {
          sendCommandToSerial('S', 'S', 'F');
        }

      }
      else if (commandChar == 'R')
      {
        int updatedFrameLength = readNextIntegerFromSerial();
        ifUpdatedFrameLengthIsGreaterThanBufferSize(updatedFrameLength);
        ifUpdatedFrameLengthIsLessThanOrEqualToBufferSize(updatedFrameLength);

        sendDebug("Serial IN: S R ", "");
        stopPlayingDevice();
        currentBeat = 0;
        lastRequestedIndex = 0;
        fillBuffer = true;
        int zero = 0;
        byte resultZero = (byte) (zero % 128);
        byte resultOne = (byte) (zero / 128);
        sendCommandToSerial('G', 'B', resultZero, resultOne);
      }
    }
  }
}

void ifUpdatedFrameLengthIsGreaterThanBufferSize(int updatedFrameLength) {
  if (updatedFrameLength > BUFFER_SIZE)
  {
    FRAME_LENGTH = BUFFER_SIZE;
    STREAM_LENGTH = updatedFrameLength;
    isStreaming = true;
  }
}

void ifUpdatedFrameLengthIsLessThanOrEqualToBufferSize(int updatedFrameLength) {
  if (updatedFrameLength <= BUFFER_SIZE)
  {
    FRAME_LENGTH = updatedFrameLength;
    STREAM_LENGTH = updatedFrameLength;
    isStreaming = false;
  }
}



void sendDebug(String tag, String message) {
  //Serial.print(tag);
  //Serial.println(message);
}

ISR(TIMER1_COMPA_vect) {
  executeInteruptRoutinForTimerOne();
}

ISR(TIMER3_COMPA_vect) {
  executeInteruptRoutinForTimerThree();
}

ISR(TIMER4_COMPA_vect) {
  executeInteruptRoutinForTimerFour();
}

ISR(TIMER5_COMPA_vect) {
  executeInteruptRoutinForTimerFive();
}


void executeInteruptRoutinForTimerOne() {
  updateSchedulerTime();
}

void executeInteruptRoutinForTimerThree() {
  beaters[0].updatecounter();
}

void executeInteruptRoutinForTimerFour() {
  beaters[1].updatecounter();
}

void executeInteruptRoutinForTimerFive() {
  beaters[2].updatecounter();
}

boolean isStreamingEndAchievedByDevice()
{
//  if (isStreaming) {
    if (currentBeat >= STREAM_LENGTH)
    {
      return true;
    }
//  }
  return false;
}

void updateSchedulerTime() {
  if (!currentDeviceStatus) {
    return;
  }

  schedulerTimerCounter--;
  if (schedulerTimerCounter > 0)
  {
    return;
  }
  if (isStreamingEndAchievedByDevice())
  {
    isStreaming = false;
    changeDeviceStatus(false);
    sendCommandToSerial('S', 'S', 'F');
//    currentBeat = 0;
    return;
  }
  Serial.print(drumNumber[currentBeat % FRAME_LENGTH]);
  Serial.print(" ");
  Serial.println(currentBeat);
  beaters[drumNumber[(currentBeat) % FRAME_LENGTH]].setBeat(currentBeat);
  currentBeat++;
  schedulerTimerCounter = timeInterval[(currentBeat) % FRAME_LENGTH];
}

