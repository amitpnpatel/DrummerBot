#include <Arduino.h>

/*  First initial sketch for Motor scheduling
    Timer 1 used for scheduling stepper step
    Beater Object represent stepper mottor
    Timer interupts updates beater object on evry interupt with modes are active
    after completion of every stepper beat object updates beater object for next beat
*/
#define BUFFER_SIZE 60
#define MAX_STEP_IN_SWING 100
boolean currentDeviceState = false;

void startTimer(int );
void stopTimer(int );
void setInterruptTimeInterval(int, int);

////Beater 1 params////////
int timeInterval[BUFFER_SIZE] = {0};// stores time interval im millis for next beat
int beatAmplitude[BUFFER_SIZE] = {0};// stores accelearation value for next beat
int drumNumber[BUFFER_SIZE] = {0};// stores beater object number
int lastUpdatedIndex;
int lastRequestedIndex = 0;
boolean fillBuffer = false;
int TIME_SCALE_FACTOR = 16;
int interruptCounter = 0;
int schedulerTimerCounter = 0;
int currentBeat = 0;// pointer to current beat

class Beater
{
  private:
    volatile uint8_t *beaterPort;

  public:
    boolean beaterState = false;  //state "true" for active beat, "false" for motor spleep
    int numberOfStepsInSwing = 6; //number of step in swing
    int numberOfStepsInHalfSwing = numberOfStepsInSwing / 2;
    int currentStepNumber = 0;   // current step count for swing
    int currentBeatAmplitude = 5; // current amplitude in final step time to decide mottor speed, value corresponds to last step
    int currentPlayingBeat = 0; //current beat number which is being played by this beater
    int drummerId = 0;
    int startStepTime = 15 * TIME_SCALE_FACTOR ;
    int reverseStepTime = 15 * TIME_SCALE_FACTOR;
    int stepIntervalStriking[MAX_STEP_IN_SWING] = {0};// stores time interval im millis for next beat
    int stepIntervalNonStriking[MAX_STEP_IN_SWING] = {0};// stores accelearation value for next beat
    int currentStepTimeInterval;
    int currentStepTimeCounter = 0;

    Beater(int id , volatile uint8_t *_beaterPort, volatile uint8_t *portAddressDDR) {
      drummerId = id;
      beaterPort = _beaterPort;
      // nextStepTimeInterval = _nextStepTimeInterval;
      // timerControlRegister = _timerControlRegister;
      *portAddressDDR |= 0b11111111 ;
    }

    void updateStepIfCurrentDirectionIsStriking() {

      Serial.print("D");
      Serial.print(drummerId);
      Serial.print("S");
      Serial.println(currentStepNumber);

      toggleStep();
      if (currentStepNumber == numberOfStepsInHalfSwing)
      {
        Serial.println("SF");

        setDirection(false);
        currentStepTimeInterval = stepIntervalNonStriking[currentStepNumber % numberOfStepsInHalfSwing];
        return;
      }
      currentStepTimeInterval =  stepIntervalStriking[currentStepNumber];
    }

    void updateStepIfCurrentDirectionIsNonStriking() {
      Serial.print(currentStepNumber);
      Serial.println("N");

      if (currentStepNumber == numberOfStepsInSwing)
      {
        toggleStep();
        clearReset();
      }
      else if (currentStepNumber > numberOfStepsInSwing)
      {
        stopBeater();
        setReset();
      }
      else
      {
        toggleStep();
        currentStepTimeInterval = stepIntervalNonStriking[currentStepNumber % numberOfStepsInHalfSwing];
      }
    }

    void stopBeater() {
      currentStepNumber = 0;
      beaterState = false;
      Serial.print("SB ");
      Serial.println(beaterState);
      return;
    }

    void setReset() {
      *beaterPort |= 0b00000100;
      return;
    }

    void clearReset() {
      *beaterPort &= 0b11111011;
      return;
    }

    void updateStep() {
      if(beaterState == false){
        
        return;
      }
      Serial.println("2:st=1");
      currentStepTimeCounter++;
      if (currentStepTimeCounter == currentStepTimeInterval)
      {
        currentStepTimeCounter = 0;
        currentStepNumber++;
        if (currentStepNumber <= numberOfStepsInHalfSwing)
        {
          updateStepIfCurrentDirectionIsStriking();
        }
        else {
          updateStepIfCurrentDirectionIsNonStriking();
        }
      }


      return;
    }

    void toggleStep() {
      *beaterPort ^= 0b00000001;
    }

    void setDirection(boolean strikingDirection) {
      if (strikingDirection)
      {
        *beaterPort |= 0b00000010;
        return;
      }
      *beaterPort &= 0b11111101;
      return;
    }

    void setBeat(int beatToLoad) {
      if (beaterState == true)
      {
        Serial.println("st=1");
          // return;
      }
      currentStepTimeCounter = 0;
      beaterState = true;
      setDirection(true);
      setReset();
      if (currentStepNumber > 0)
      {
        currentStepNumber = numberOfStepsInSwing - currentStepNumber;
      }

      currentBeatAmplitude = beatAmplitude[beatToLoad] * 15.624;
      setStepIntervalForAmplitude(currentBeatAmplitude);
      //      *nextStepTimeInterval = stepIntervalStriking[currentStepNumber];
      currentStepTimeInterval = stepIntervalStriking[currentStepNumber];
      return;
    }

    // void startStepTimerForBeater() {
    //   Serial.print(drummerId);
    //   Serial.print("d St ");
    //   startTimer(beaterTimerNumber);
    //   // *timerControlRegister |= 0b00000101;
    // }

    // void stopStepTimerForBeater() {
    //   stopTimer(beaterTimerNumber);
    //   // *timerControlRegister &= 0b11111000;
    //   Serial.print(drummerId);
    //   Serial.print(" b Stop");
    // }


    void setStepIntervalForAmplitude(int currentBeatAmplitude)
    {
      for (int stepNumber = 0; stepNumber < numberOfStepsInSwing; stepNumber++) {
        // stepIntervalStriking[stepNumber] = startStepTime - ((startStepTime - currentBeatAmplitude) * stepNumber) / numberOfStepsInSwing; // 1 equals to 4 micro second
        // stepIntervalNonStriking[stepNumber] = reverseStepTime;
        stepIntervalStriking[stepNumber] = 20 * TIME_SCALE_FACTOR;
        stepIntervalNonStriking[stepNumber] = 20 * TIME_SCALE_FACTOR;
      }
      return;
    }
};

int drumIdBeaterOne = 0;
int drumIdBeaterTwo = 1;

Beater beater1 = Beater(drumIdBeaterOne, &PORTF, &DDRF); //define beater
Beater beater2 = Beater(drumIdBeaterTwo, &PORTK, &DDRK);

Beater beaters[] = {beater1, beater2};

int lastUpdatedBufferIndex = 0;
boolean waitForData = false;


void changeDeviceState(boolean deviceState) {
  currentDeviceState = deviceState;
  sendDebug("STATE: ", "" + currentDeviceState);
  if (currentDeviceState)
  {
    loadBeat();
    startScheduleTimer();
    return;
  }
  stopScheduleTimer();
}

int getIntegerFromTwoBytes(byte readIndexByteOne, byte readIndexByteTwo) {
  return readIndexByteOne + readIndexByteTwo * 128;
}

void setupScheduleTimer()
{
  //set timer 3 for beat scheduling
  TCCR1A = 0;// set entire TCCR1A register to 0
  TCCR1B = 0;// same for TCCR1B
  TCNT1  = 0;//initialize counter value to 0
  // set compare match register for 1000hz increments
  // calculation for 1024 prescalar 15.625-1
  // @ 256 62.5-1
  //@64 250-1
  OCR1A = 5000;//15625-1 sec

  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS32 and CS30 bits for 1024 prescaler
  // TCCR3B |= (1 << CS32) | (1 << CS30);
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);
}

void setupTimer0() {
  //set timer 5 for step scheduling
  TCCR0A = 0;// set entire TCCR1A register to 0
  TCCR0B = 0;// same for TCCR1B
  TCNT0  = 0;//initialize counter value to 0
  // set compare match register for 1000hz increments
  // calculation for 1024 prescalar 15.625-1
  // @ 256 62.5-1
  //@64 250-1
  OCR0A = 249;//250000-1 sec

  // turn on CTC mode
  TCCR0B |= (1 << WGM01);
  // Set CS10 and CS11 bits for 64 prescaler
  TCCR0B |= (1 << CS01) | (1 << CS00);
  // enable timer compare interrupt
  TIMSK0 |= (1 << OCIE0A);
  return;
}

void setupTimmerForInterrupts() {
  // initialize timer1 interupt at every 1000micro sec or 1 milli sec
  //set timer1 interrupt at 1000Hz
  cli();//stop interrupts
  setupScheduleTimer();
  setupTimer0();
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
  timeInterval[index % BUFFER_SIZE] = timeForNextBeat;
  beatAmplitude[index % BUFFER_SIZE] = amplitudeForNextBeat;
  drumNumber[index % BUFFER_SIZE] = drumNumberForNextBeat;
  lastUpdatedIndex = index;
  return index;
}

void performOperationsIfTheBufferIsNotFilled(int index) {
  if (fillBuffer)
  {
    if (index == BUFFER_SIZE - 1)
    {
      fillBuffer = false;
      sendCommandToSerial('S', 'R', true);
    }
    int nextIndex = index + 1;
    byte resultZero = (byte) (nextIndex % 128);
    byte resultOne = (byte) (nextIndex / 128);
    sendCommandToSerial('G', 'B', resultZero, resultOne);
  }
}

void loop() {
  sei();
  if (currentDeviceState == true) {
    if (currentBeat > lastUpdatedIndex + 1 - BUFFER_SIZE)
    {

      if (lastRequestedIndex <= lastUpdatedIndex) {
        //        Serial.print("LR LU");
        //        Serial.println(lastUpdatedIndex);
        int oneMoreThanLastUpdatedIndex = lastUpdatedIndex + 1;
        lastRequestedIndex = oneMoreThanLastUpdatedIndex;
        requestBeat(oneMoreThanLastUpdatedIndex);
      }
    }
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
        sendCommandToSerial('S', 'S', currentDeviceState);
      }
    }

    else if (inChar == 'S')
    {
      char commandChar = readNextCharacterFromSerial();
      if (commandChar == 'B')
      {
        sendDebug("Serial IN: S B ", "");
        int index = updateBufferWithBeatFromSerial();
        //        Serial.print("RB ");
        //        Serial.println(index);
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
        changeDeviceState(desiredState);
        if (currentDeviceState)
        {
          sendCommandToSerial('S', 'S', 'T');

        }
        else {
          sendCommandToSerial('S', 'S', 'F');
        }

      }
      else if (commandChar == 'R')
      {
        sendDebug("Serial IN: S R ", "");
        stopScheduleTimer();
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
void sendDebug(String tag, String message) {
  //Serial.print(tag);
  //Serial.println(message);
}



ISR(TIMER1_COMPA_vect) {
  executeTimerInterrupt1A();
}

ISR(TIMER0_COMPA_vect) {
  executeTimerInterrupt0A();
}


void executeTimerInterrupt1A() {
  Serial.println("1A() ");
  loadBeat();
}

void executeTimerInterrupt0A() {
//  Serial.print("5A() ");
  beater1.updateStep();
  beater2.updateStep();
}

void loadBeat() {
  beaters[drumNumber[currentBeat]].setBeat(currentBeat);
  setTimeIntervalForNextBeat();
  currentBeat++;
  return;
}

void setTimeIntervalForNextBeat() {
  // OCR3A = timeInterval[currentBeat + 1] * 15.625;
  OCR1A = 50000;
}

void startScheduleTimer() {
  TCCR1B |= (1 << CS12) | (1 << CS10);
}

void stopScheduleTimer() {
  TCCR1B &= ~((1 << CS12) | (1 << CS11) | (1 << CS10));
}
