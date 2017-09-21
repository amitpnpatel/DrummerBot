#include <Arduino.h>

/*  First initial sketch for Motor scheduling
    Timer 1 used for scheduling stepper step
    Beater Object represent stepper mottor
    Timer interupts updates beater object on evry interupt with modes are active
    after completion of every stepper beat object updates beater object for next beat
*/
#include<SoftwareSerial.h>
#define BUFFER_SIZE 60
boolean currentDeviceStatus = false;

////Beater 1 params////////
int timeInterval[BUFFER_SIZE] = {0};// stores time interval im millis for next beat
int beatAmplitude[BUFFER_SIZE] = {0};// stores accelearation value for next beat
int drumNumber[BUFFER_SIZE] = {0};// stores beater object number
int lastHalfSwingTime = 0;
int lastUpdatedIndex;
int lastRequestedIndex = 0;
boolean fillBuffer = false;
int SWING_TIME = 150;
int TIME_SCALE_FACTOR = 8;
int interruptCounter = 0;
int schedulerTimerCounter = 0;
int currentBeat = 0;// pointer to current beat

class Beater
{
  private:
    int mcurrentStepNumber = 0;///stores current step number for motor
    volatile uint8_t *beaterPort;

  public:
    boolean state = false;  //state "true" for active beat, "false" for motor spleep
    int numberOfStepInSwing = 18; //number of step in swing
    int numberOfStepInHalfSwing = numberOfStepInSwing / 2;
    int currentStepNumber = 0;   // current step count for swing
    int upSwingTime = 10;  // current steptime in interupt frequency, decided based on minimum time gape 90 milli, value corresponds to first step
    int currentbeatAmplitude = 5; // current amplitude in final step time to decide mottor speed, value corresponds to last step
    int currentTimeCounter = 0; // interupt counter
    int currentPlayingBeat = 0; //current beat number which is being played by this beater
    int drummerId = 0;
    int startStepTime = 15 * TIME_SCALE_FACTOR;
    int startStepNumber = 0 ;


    Beater(int id , volatile uint8_t *portAddress, volatile uint8_t *portAddressDDR, int offset) {
      drummerId = id;
      mcurrentStepNumber = offset;
      startStepNumber = offset;
      beaterPort = portAddress;
      *portAddressDDR |= 0b11111111 ;
    }

    void updatecounter() {
      if (state) {
        currentTimeCounter--;
        if (currentTimeCounter > 0) {
          return ;
        }

        if (currentStepNumber < numberOfStepInHalfSwing) {
          currentTimeCounter = startStepTime - (((startStepTime - currentbeatAmplitude) * (currentStepNumber % (numberOfStepInHalfSwing))) / (numberOfStepInHalfSwing));
          // currentTimeCounter =  startStepTime;
        }
        else {
          currentTimeCounter = upSwingTime ; // TODO add acceleration value
        }
        if (currentTimeCounter < 2) {
          currentTimeCounter = 2;
        }
        currentStepNumber++;
        if (currentStepNumber > numberOfStepInSwing) {
          currentStepNumber = 0;
          mcurrentStepNumber = startStepNumber;
          state = false;
        } else {
          if (currentStepNumber <= numberOfStepInHalfSwing) {
            halfstep(1);
          }
          else {
            halfstep(-1);
          }
        }
      }
    }

    void disablemotor() {
      *beaterPort = 0b00000000;
    }

    void halfstep(int dir) {
      mcurrentStepNumber = (mcurrentStepNumber + dir + 8) % 8;
      switch (mcurrentStepNumber) {
        case 0:
          *beaterPort = 0b00001000;
          break;
        case 1:
          *beaterPort = 0b00001010;
          break;
        case 2:
          *beaterPort = 0b00000010;
          break;
        case 3:
          *beaterPort = 0b00000110;
          break;
        case 4:
          *beaterPort = 0b00000100;
          break;
        case 5:
          *beaterPort = 0b00000101;
          break;
        case 6:
          *beaterPort = 0b00000001;
          break;
        case 7:
          *beaterPort = 0b00001001;
          break;
        default:
          break;
      }
    }
    void setBeat(int beatToLoad) {
      if (state) {
        if (currentStepNumber > ((numberOfStepInHalfSwing) + 2)) {
          currentStepNumber = numberOfStepInSwing - currentStepNumber;
          currentbeatAmplitude = 2 + ( (100 - (beatAmplitude[beatToLoad % BUFFER_SIZE])) * (startStepTime - 2) / 100); ///TODO: amplitude to motor steptime map
          upSwingTime = TIME_SCALE_FACTOR * (SWING_TIME / (numberOfStepInHalfSwing));
          currentTimeCounter = startStepTime;
          currentPlayingBeat = beatToLoad;
          state = true;
        }
        return;
      }
      //Serial.print("LB ");
      //      Serial.println(currentBeat);

      state = true;

      currentStepNumber = 0;
      currentbeatAmplitude = 2 + ( (100 - (beatAmplitude[beatToLoad % BUFFER_SIZE])) * (startStepTime - 2) / 100); ///TODO: amplitude to motor steptime map

      upSwingTime = TIME_SCALE_FACTOR * (SWING_TIME / (numberOfStepInHalfSwing));
      currentTimeCounter = startStepTime;
      currentPlayingBeat = beatToLoad;
    }
};

int calibrationForBeaterOne = 2;
int calibrationForBeaterTwo = 2;
int drumIdBeaterOne = 0;
int drumIdBeaterTwo = 1;

Beater beater1 = Beater(drumIdBeaterOne, &PORTB, &DDRB, calibrationForBeaterOne); //define beater
Beater beater2 = Beater(drumIdBeaterTwo, &PORTC, &DDRC, calibrationForBeaterTwo);

Beater beaters[] = {beater1, beater2};

int lastUpdatedBufferIndex = 0;
boolean waitForData = false;
SoftwareSerial softwareSerial = SoftwareSerial(2, 3);

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
  OCR1A = ((16000 / TIME_SCALE_FACTOR) - 1);

  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS10 and CS11 bits for 1 prescaler
  TCCR1B |= (1 << CS10);
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);
  sei();
}

void setup() {
  for (byte bufferIndexCounter = 0; bufferIndexCounter < BUFFER_SIZE; bufferIndexCounter++) {
    timeInterval[bufferIndexCounter] = 1000 + (bufferIndexCounter % 5) * 0;
    beatAmplitude[bufferIndexCounter] = 2;
    drumNumber[bufferIndexCounter] = 0;
  }
  Serial.begin(9600);
  softwareSerial.begin(9600);
  setupTimmerForInterrupts();
}

void requestBeat(int index) {
  softwareSerial.print('G');
  softwareSerial.print('B');
  byte resultZero = (byte) (index % 128);
  byte resultOne = (byte) (index / 128);
  softwareSerial.write(resultZero);
  softwareSerial.write(resultOne);
  softwareSerial.print('\n');
  return;
}

char readNextCharacterFromSerial() {
  while (softwareSerial.available() == 0) {}
  return (char)softwareSerial.read();
}

byte readNextByteFromSerial() {
  while (softwareSerial.available() == 0) {}
  return softwareSerial.read();
}

int readNextIntegerFromSerial() {
  byte readIndexByteOne = readNextByteFromSerial();
  byte readIndexByteTwo = readNextByteFromSerial();

  return getIntegerFromTwoBytes(readIndexByteOne, readIndexByteTwo);
}

void sendCommandToSerial(char commandOne, char commandTwo, byte byteToSend) {
  softwareSerial.print(commandOne);
  softwareSerial.print(commandTwo);
  softwareSerial.write(byteToSend);
  softwareSerial.print('\n');

  sendDebug("Serial out: ", "");
  sendDebug("", "" + commandOne);
  sendDebug("", "" + commandTwo);
  sendDebug("", "" + byteToSend);
}

void sendCommandToSerial(char commandOne, char commandTwo, byte byteOneToSend, byte byteTwoToSend) {
  softwareSerial.print(commandOne);
  softwareSerial.print(commandTwo);
  softwareSerial.write(byteOneToSend);
  softwareSerial.write(byteTwoToSend);
  softwareSerial.print('\n');

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
  if (currentDeviceStatus == true) {
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

  while (softwareSerial.available()) {
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
void sendDebug(String tag, String message) {
  //Serial.print(tag);
  //Serial.println(message);
}

ISR(TIMER1_COMPA_vect) {
  executeInteruptRoutin();
}
void executeInteruptRoutin() {
  interruptCounter++;
  updateBeatertimes();
  if (interruptCounter == TIME_SCALE_FACTOR) {
    updateSchedulerTime();
    interruptCounter = 0;
  }
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
  beaters[drumNumber[(currentBeat) % BUFFER_SIZE]].setBeat(currentBeat);
  currentBeat++;
  schedulerTimerCounter = timeInterval[(currentBeat) % BUFFER_SIZE];

}

void updateBeatertimes() {
  int numberOfBeaters = (sizeof(beaters) / sizeof(Beater));
  for (int beaterCounter = 0; beaterCounter < numberOfBeaters; beaterCounter++)
  {
    beaters[beaterCounter].updatecounter();
  }
}
