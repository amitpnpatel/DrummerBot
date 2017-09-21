#include <Arduino.h>

/*  First initial sketch for Motor scheduling
    Timer 1 used for scheduling stepper step
    Beater Object represent stepper mottor
    Timer interupts updates beater object on evry interupt with modes are active
    after completion of every stepper beat object updates beater object for next beat
*/
#include<SoftwareSerial.h>
#define BUFFER_SIZE 60
#define MAX_STEP_IN_SWING 100
boolean currentDeviceStatus = false;


int timeInterval[BUFFER_SIZE] = {0};// stores time interval im millis for next beat
int beatAmplitude[BUFFER_SIZE] = {0};// stores accelearation value for next beat
int drumNumber[BUFFER_SIZE] = {0};// stores beater object number
int lastHalfSwingTime = 0;
int lastUpdatedIndex;
int lastRequestedIndex = 0;
boolean fillBuffer = false;
int SWING_TIME = 70;
int interruptCounter = 0;
int schedulerTimerCounter = 0;
int currentBeat = 0;// pointer to current beat
int stepCounter = 0;

class Beater
{
  private:
    int mcurrentStepNumber = 0;///stores current step number for motor
    volatile uint8_t *beaterPort;
    volatile uint16_t *timerCounterRegister;
    volatile uint16_t *timerCompareRegister;

  public:
    boolean state = true;  //state "true" for active beat, "false" for motor spleep
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
      }
      return;
    }

    void updatecounter() {
      if (state) {
        if (currentStepNumber < numberOfStepsInHalfSwing) {
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
          state = true;
          currentStepNumber = 0;
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
          currentbeatAmplitude = minimumStepTime + ( (double)(100 - (beatAmplitude[beatToLoad % BUFFER_SIZE])) * (double)(startStepTime - minimumStepTime) / 100); ///TODO: amplitude to motor steptime map
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
      currentbeatAmplitude = minimumStepTime + ( (double)(100 - beatAmplitude[beatToLoad % BUFFER_SIZE]) * (double)(startStepTime - minimumStepTime)) / 100; ///TODO: amplitude to motor steptime map
      setStepIntervalForAmplitude(currentbeatAmplitude);
      currentTimeCounter = stepIntervalStriking[currentStepNumber];
      setNextInterval(currentTimeCounter);
      currentPlayingBeat = beatToLoad;
      setReset();
      Serial.println(currentbeatAmplitude);
      state = true;
    }
};

int numberofStepsForBeaterOne = 20;
int numberofStepsForBeaterTwo = 24;
int numberofStepsForBeaterThree = 32;

int drumIdBeaterOne = 0;
int drumIdBeaterTwo = 1;
int drumIdBeaterThree = 2;

Beater beater1 = Beater(drumIdBeaterOne, &PORTF, &DDRF, numberofStepsForBeaterOne, &TCNT3 , &OCR3A); //define beater
Beater beater2 = Beater(drumIdBeaterTwo, &PORTK, &DDRK, numberofStepsForBeaterTwo, &TCNT4 , &OCR4A);
Beater beater3 = Beater(drumIdBeaterThree, &PORTA, &DDRA, numberofStepsForBeaterThree, &TCNT5 , &OCR5A);

Beater beaters[] = {beater1, beater2, beater3};

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
  OCR1A = (32000  - 1);

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
  Serial1.begin(9600);
  setupTimmerForInterrupts();
}

void loop() {
}



ISR(TIMER1_COMPA_vect) {
  executeInteruptRoutinForTimerOne();
}

void executeInteruptRoutinForTimerOne() {
  stepCounter++;
  if (stepCounter > 200)
  {
    stepCounter = 0;
//    beaters[0].updatecounter();
//    beaters[1].updatecounter();
    beaters[2].updatecounter();

  }

}

