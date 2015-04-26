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

union u_tag {
    byte buffer[4];
    unsigned int value;
} u;
void setup() {
  Serial.begin(57600);
  softSerial.begin(57600);

  leftWheel.attach(LEFT_WHEEL_PIN, SERVO_OFFSET, SERVO_FULL_SPEED);
  rightWheel.attach(RIGHT_WHEEL_PIN, SERVO_OFFSET, SERVO_FULL_SPEED);

  leftWheel.writeMicroseconds(SERVO_IDLE);
  rightWheel.writeMicroseconds(SERVO_IDLE);
}

void processMessage(const char *command) {

  u.buffer[0] = command[2];
  u.buffer[1] = command[3];
  u.buffer[2] = command[4];
  u.buffer[3] = command[5];
  unsigned int leftSpeed = u.value;
  
  u.buffer[0] = command[6];
  u.buffer[1] = command[7];
  u.buffer[2] = command[8];
  u.buffer[3] = command[9];
  unsigned int rightSpeed = u.value;
  
  Serial.print("l:");
  Serial.print(leftSpeed);
  Serial.print(" r:");
  Serial.println(rightSpeed);

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

