#include <Arduino.h>

int led = 13;
long counter=0;
void setup() {
 pinMode(led, OUTPUT);
  // put your setup code here, to run once:
cli();//stop interrupts

////set timer0 interrupt at 2kHz
//  TCCR0A = 0;// set entire TCCR0A register to 0
//  TCCR0B = 0;// same for TCCR0B
//  TCNT0  = 0;//initialize counter value to 0
//  // set compare match register for 2khz increments
//  OCR0A = 124;// = (16*10^6) / (2000*64) - 1 (must be <256)
//  // turn on CTC mode
//  TCCR0A |= (1 << WGM01);
//  // Set CS01 and CS00 bits for 64 prescaler
//  TCCR0B |= (1 << CS01) | (1 << CS00);   
//  // enable timer compare interrupt
//  TIMSK0 |= (1 << OCIE0A);
//
////set timer1 interrupt at 1Hz
//  TCCR1A = 0;// set entire TCCR1A register to 0
//  TCCR1B = 0;// same for TCCR1B
//  TCNT1  = 0;//initialize counter value to 0
//  // set compare match register for 1hz increments
//  OCR1A = 15624;// = (16*10^6) / (1*1024) - 1 (must be <65536)
//  // turn on CTC mode
//  TCCR1B |= (1 << WGM12);
//  // Set CS10 and CS12 bits for 1024 prescaler
//  TCCR1B |= (1 << CS12) | (1 << CS10);  
//  // enable timer compare interrupt
//  TIMSK1 |= (1 << OCIE1A);

//set timer2 interrupt at 8kHz
  TCCR2A = 0;// set entire TCCR2A register to 0
  TCCR2B = 0;// same for TCCR2B
  TCNT2  = 0;//initialize counter value to 0
  // set compare match register for 8khz increments
  OCR2A = 250;// = (16*10^6) / (8000*8) - 1 (must be <256)
  // turn on CTC mode
  TCCR2A |= (1 << WGM21);
  // Set CS21 bit for 64 prescaler
  TCCR2B |= (1 << CS22);   
  // enable timer compare interrupt
  TIMSK2 |= (1 << OCIE2A);


sei();//allow interrupts
}

ISR(TIMER2_COMPA_vect){//timer1 interrupt 8kHz toggles pin 9
//generates pulse wave of frequency 8kHz/2 = 4kHz (takes two cycles for full wave- toggle high then toggle low)
  counter++;
  if(counter==1000){
    digitalWrite(led,HIGH);
    //counter=0;
  }
  if(counter==2000){
    digitalWrite(led,LOW);
    counter=0;
  }
}
ISR(TIMER0_COMPA_vect){//timer1 interrupt 8kHz toggles pin 9
//generates pulse wave of frequency 8kHz/2 = 4kHz (takes two cycles for full wave- toggle high then toggle low)
  counter++;
  if(counter==1000){
    digitalWrite(led,HIGH);
    //counter=0;
  }
  if(counter==2000){
    digitalWrite(led,LOW);
    counter=0;
  }
}
ISR(TIMER1_COMPA_vect){//timer1 interrupt 8kHz toggles pin 9
//generates pulse wave of frequency 8kHz/2 = 4kHz (takes two cycles for full wave- toggle high then toggle low)
  counter++;
  if(counter==1000){
    digitalWrite(led,HIGH);
    //counter=0;
  }
  if(counter==2000){
    digitalWrite(led,LOW);
    counter=0;
  }
}
void loop() {
  // put your main code here, to run repeatedly:

}
