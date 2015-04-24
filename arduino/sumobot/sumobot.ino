#include <Servo.h>
#include <SoftwareSerial.h>

const int SERVO_OFFSET = 800;
const int SERVO_IDLE = 1500;
const int SERVO_FULL_SPEED = 2200;
const int SERVO_RANGE = SERVO_FULL_SPEED - SERVO_OFFSET;
const int COMMAND_FULL_SPEED = 1024;
const int LEFT_WHEEL_PIN = 4;
const int RIGHT_WHEEL_PIN = 5;
const char START_CODE = 0xAB;
const char END_CODE = 0xEF;
const int BUFSIZE = 17;

Servo leftWheel;
Servo rightWheel;
SoftwareSerial softSerial(6, 7);
char commandBuffer[BUFSIZE];
int bufferPos = 0;

void setup() {
  softSerial.begin(57600);
  Serial.begin(57600);

  leftWheel.attach(LEFT_WHEEL_PIN, SERVO_OFFSET, SERVO_FULL_SPEED);
  rightWheel.attach(RIGHT_WHEEL_PIN, SERVO_OFFSET, SERVO_FULL_SPEED);

  leftWheel.writeMicroseconds(SERVO_IDLE);
  rightWheel.writeMicroseconds(SERVO_IDLE);
}

void processMessage(const char *command) {
  // this is only a test, so we just
  // print out the message
  Serial.print("Received:");
  Serial.println(command);

  unsigned int leftSpeed = 0;
  leftSpeed += ((unsigned int)command[3]) << 8;
  leftSpeed += ((unsigned int)command[2]);
  leftSpeed = min(leftSpeed, COMMAND_FULL_SPEED);
  leftSpeed = max(leftSpeed, 0);

  unsigned int rightSpeed = 0;
  rightSpeed += ((unsigned int)command[5]) << 8;
  rightSpeed += ((unsigned int)command[4]);
  rightSpeed = min(rightSpeed, COMMAND_FULL_SPEED);
  rightSpeed = max(rightSpeed, 0);

  float leftSpeedRatio = ((float)leftSpeed) / ((float)COMMAND_FULL_SPEED);
  leftSpeedRatio = 1.0 - leftSpeedRatio;
  leftSpeedRatio = min(leftSpeedRatio, 1.0);
  leftSpeedRatio = max(leftSpeedRatio, 0.0);
  float rightSpeedRatio = ((float)rightSpeed) / ((float)COMMAND_FULL_SPEED);
  rightSpeedRatio = min(rightSpeedRatio, 1.0);
  rightSpeedRatio = max(rightSpeedRatio, 0.0);
 
  unsigned int leftMotorSpeed = (leftSpeedRatio * (float)SERVO_RANGE) + (float)SERVO_OFFSET;
  leftMotorSpeed = min(leftMotorSpeed, SERVO_FULL_SPEED);
  leftMotorSpeed = max(leftMotorSpeed, SERVO_OFFSET);
  unsigned int rightMotorSpeed = (rightSpeedRatio * (float)SERVO_RANGE) + (float)SERVO_OFFSET;
  rightMotorSpeed = min(rightMotorSpeed, SERVO_FULL_SPEED);
  rightMotorSpeed = max(rightMotorSpeed, SERVO_OFFSET);
  
  leftWheel.writeMicroseconds(leftMotorSpeed);
  rightWheel.writeMicroseconds(rightMotorSpeed);
}

void loop () {
  char ch;

  while (softSerial.available() > 0) {
    ch = softSerial.read();

    if (ch == END_CODE) {
      commandBuffer[bufferPos] = 0;
      processMessage(commandBuffer);
      bufferPos = 0;
    }
    else {
      if (bufferPos < (BUFSIZE - 1)) {
        commandBuffer[bufferPos] = ch;
        bufferPos++;
      }
      else {
        Serial.println("Lost byte!");
      }
    }
  }
  
  delay(10);
}

