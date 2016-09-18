/*
isr4pi.c
D. Thiebaut
based on isr.c from the WiringPi library, authored by Gordon Henderson
https://github.com/WiringPi/WiringPi/blob/master/examples/isr.c

Compile as follows:

    gcc -o isr4pi isr4pi.c -lwiringPi

Run as follows:

    sudo ./isr4pi

 */
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <time.h>
#include <sys/time.h>
#include <wiringPi.h>


// Use GPIO Pin 108, which is Pin 23 for wiringPi library

#define BUTTON_PIN 23
#define READ_PIN 24


// the event counter
volatile int eventCounter = 0;
volatile int forwardCounter = 0;


// -------------------------------------------------------------------------
// myInterrupt:  called every time an event occurs
void myInterrupt(void) {
   eventCounter++;
   int p1 = digitalRead(BUTTON_PIN);
   int p2 = digitalRead(READ_PIN);
   if (p1 == p2) {
      forwardCounter++;
   }
}

char *timestamp(){
   char *timestamp = (char *)malloc(sizeof(char) * 16);
   struct timeval tv;
   gettimeofday(&tv,NULL);
   tv.tv_sec; // seconds
   tv.tv_usec; // microseconds
   time_t ltime;
   ltime=time(NULL);
   struct tm *tm;
   tm=localtime(&ltime);

   sprintf(timestamp,"%04d%02d%02dT%02d%02d%02d.%03ld", tm->tm_year+1900, tm->tm_mon+1,
   tm->tm_mday, tm->tm_hour, tm->tm_min, tm->tm_sec, tv.tv_usec/1000);
   return timestamp;
}

// -------------------------------------------------------------------------
// main
int main(void) {
  // sets up the wiringPi library
  if (wiringPiSetup () < 0) {
      fprintf (stderr, "Unable to setup wiringPi: %s\n", strerror (errno));
      return 1;
  }

  // set Pin 17/0 generate an interrupt on high-to-low transitions
  // and attach myInterrupt() to the interrupt
  if ( wiringPiISR (BUTTON_PIN, INT_EDGE_FALLING, &myInterrupt) < 0 ) {
      fprintf (stderr, "Unable to setup ISR: %s\n", strerror (errno));
      return 1;
  }

  // display counter value every second.
  while ( 1 ) {
    printf( "%s|%d|%d\n", timestamp(),eventCounter, forwardCounter );
    eventCounter = 0;
    forwardCounter = 0;
    delay( 100 ); // wait 1 second
  }

  return 0;
}

