#include <Arduino.h>

#include <ESP8266WiFi.h>

const char* ssid = "twguest";
const char* password = "novo torch cycad ellen aqua";

int ledPin = 13; // GPIO13
int counter = 0;
int portNumber = 5001;
byte ipToConnect[] = {10, 131, 124, 110};

void setup() {
  Serial.begin(115200);
  delay(10);

  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);

  // Connect to WiFi network


  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");

}

void loop() {
  // Check if a client has connected
  WiFiClient client;

  Serial.print("Inside loop");

  if (client.connect(ipToConnect, portNumber)) {
    while (1) {
      client.print("Hey!");
      while(client.available() > 0){
        Serial.print((char)client.read());
      }
      delay(2000);
    }
  }
  else {
    Serial.println("Client didn't connect");
  }
}

