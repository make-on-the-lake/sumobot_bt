#! /usr/bin/env python

import sys
import time
import serial

BAUD_RATE = 57600
RX_BUFFER = 50
CMD_DELAY = 0.2

BAUD_CODES = {
        0:9600,
        1:19200,
        2:38400,
        3:57600,
        4:115200,
        5:4800,
        6:2400,
        7:1200,
        8:230400 }

def setup_btle():
    serial_device = None
    serial_port = sys.argv[1]

    while(True):

        raw_input("Press enter to start...")

        if(serial_device != None):
            serial_device.close()

        try:
            serial_device = serial.Serial(serial_port, 9600, timeout=1)
            _verify_ok(serial_device)
        except: 
            print("Couldn't connect at 9600! Trying again at {}.".format(BAUD_RATE))
            serial_device.close()
            serial_device = serial.Serial(serial_port, BAUD_RATE, timeout=1)
            _verify_ok(serial_device)

        baud_rate = _get_baud_rate(serial_device)
        if(baud_rate != BAUD_RATE):
            _set_baud_rate(serial_device, BAUD_RATE)
            _reset(serial_device)

        serial_device.close()
        serial_device = serial.Serial(serial_port, BAUD_RATE, timeout=1)

        _verify_ok(serial_device)
        name = _get_name()
        _set_name(serial_device, name)

        print("Done!\n\n")

def _get_name():
    return raw_input("Enter name: ")

def _set_name(serial_device, name):
    print("Setting name to {}".format(name)),
    response = _send_command_and_wait_for(serial_device, "AT+NAME{}".format(name), "OK+Set:")
    actual_name = response.strip("OK+Set:")
    if(actual_name != name):
        raise Exception("Failed to set name!")
    print("OK!")

def _send_command_and_wait_for(serial_device, command, expected_response):
    serial_device.write(command)
    time.sleep(CMD_DELAY)
    response = serial_device.read(RX_BUFFER)
    if expected_response not in response:
        raise Exception("Did not get {} response for {} command. Got: \"{}\"".format(expected_response, command, response))
    time.sleep(CMD_DELAY)
    return response

def _verify_ok(serial_device):
    print("Verifying serial connection..."),
    response = _send_command_and_wait_for(serial_device, "AT", "OK")
    print("OK!")

def _get_baud_rate(serial_device):
    print("Getting current baud rate..."),
    response = _send_command_and_wait_for(serial_device, "AT+BAUD?", "OK+Get:")
    baud_code = response.strip("OK+Get:")
    baud_rate = BAUD_CODES[int(baud_code)]
    print(baud_rate)
    return baud_rate

def _set_baud_rate(serial_device, baud_rate):
    print("Setting baud rate to {}...".format(baud_rate)),
    baud_code = BAUD_CODES.keys()[BAUD_CODES.values().index(baud_rate)]
    response = _send_command_and_wait_for(serial_device, "AT+BAUD{}".format(baud_code), "OK+Set:")
    actual_baud_code = int(response.strip("OK+Set:"))
    if(baud_code != actual_baud_code):
        raise Exception("Failed to set baud rate!")
    print("OK!")

def _reset(serial_device):
    serial_device.write("AT+RESET")


setup_btle()
