#include <Servo.h>

const int SERVO_OFFSET = 800;
const int SERVO_IDLE = 1500;
const int SERVO_FULL_SPEED = 2200;
const int SERVO_RANGE = SERVO_FULL_SPEED - SERVO_OFFSET;
const int COMMAND_FULL_SPEED = 1024;
const int LEFT_WHEEL_PIN = 4;
const int RIGHT_WHEEL_PIN = 3;
const char START_CODE = '0xAB';
const char END_CODE = '0xEF';

Servo leftWheel;
Servo rightWheel;
String currentCommand = "";

void setup() {
  Serial.begin(57600);
  
  leftWheel.attach(LEFT_WHEEL_PIN);
  rightWheel.attach(RIGHT_WHEEL_PIN);
  
  leftWheel.writeMicroseconds(SERVO_IDLE);
  rightWheel.writeMicroseconds(SERVO_IDLE);
}

void loop() {
  String command = readCommand();
  processMotorCommand(command);
}

void processMotorCommand(String command) {
  unsigned int leftSpeed = 0;
  leftSpeed += ((unsigned int)command[2]) << 8;
  leftSpeed += ((unsigned int)command[3]);
  float leftSpeedRatio = (float)leftSpeed / (float)COMMAND_FULL_SPEED;
  
  unsigned int rightSpeed = 0;
  rightSpeed += ((unsigned int)command[4]) << 8;
  rightSpeed += ((unsigned int)command[5]);
  float rightSpeedRatio = (float)rightSpeed / (float)COMMAND_FULL_SPEED;
  
  float leftMotorSpeed = (leftSpeedRatio * SERVO_RANGE) + SERVO_OFFSET;
  float rightMotorSpeed = (rightSpeedRatio * SERVO_RANGE) + SERVO_OFFSET;
  
  leftWheel.writeMicroseconds((int)leftMotorSpeed);
  rightWheel.writeMicroseconds((int)rightMotorSpeed);
}

String readCommand() {
  String command = "";
  while(Serial.available()) {
    char readChar = (char)Serial.read();
    
    if(currentCommand.length() > 0 || readChar == START_CODE) {
      currentCommand += readChar;
    }
    
    if(readChar == END_CODE && currentCommand.length() == 16) {
      command = currentCommand;
    } else if(readChar == END_CODE) {
      break;
    }
  }
  
  return command;
}
