#! /usr/bin/env python

import sys
import time
import serial
import signal

def signal_handler(signal, frame):
    if(serial_device != None):
        serial_device.close()
    sys.exit(0)


serial_device = None

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
    global serial_device

    serial_port = sys.argv[1]
    initial_baud = 9600
    if len(sys.argv) > 2:
        initial_baud = sys.argv[2]

    while(True):

        raw_input("Press enter to start...")

        try:
            if(serial_device == None):
                serial_device = serial.Serial(serial_port, initial_baud, timeout=1)

            _verify_ok(serial_device)
        except: 
            print("Couldn't connect at {}! Trying again at {}.".format(initial_baud, BAUD_RATE))
            if(serial_device == None):
                serial_device.close()
            serial_device = serial.Serial(serial_port, BAUD_RATE, timeout=1)
            _verify_ok(serial_device)

        baud_rate = _get_baud_rate(serial_device)
        if(baud_rate != BAUD_RATE):
            _set_baud_rate(serial_device, BAUD_RATE)

        _get_name(serial_device)
        _set_name(serial_device, _get_name_from_user())

        _verify_ok(serial_device)

        print("Done!\n\n")

def _get_name_from_user():
    return raw_input("Enter name: ")

def _get_name(serial_device):
    print("Getting current name..."),
    response = _send_command_and_wait_for(serial_device, "AT+NAME?", "OK+NAME:")
    name = response.replace("OK+NAME:", "")
    print(name)
    return name

def _set_name(serial_device, name):
    print("Setting name to {}...".format(name)),
    response = _send_command_and_wait_for(serial_device, "AT+NAME{}".format(name), "OK+Set:")
    actual_name = response.replace("OK+Set:", "")
    if(actual_name != name):
        raise Exception("Failed to set name! Actual name returned as {}".format(actual_name))
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
    baud_code = response.replace("OK+Get:", "")
    baud_rate = BAUD_CODES[int(baud_code)]
    print(baud_rate)
    return baud_rate

def _set_baud_rate(serial_device, baud_rate):
    print("Setting baud rate to {}...".format(baud_rate)),
    baud_code = BAUD_CODES.keys()[BAUD_CODES.values().index(baud_rate)]
    response = _send_command_and_wait_for(serial_device, "AT+BAUD{}".format(baud_code), "OK+Set:")
    actual_baud_code = int(response.replace("OK+Set:", ""))
    if(baud_code != actual_baud_code):
        raise Exception("Failed to set baud rate!")
    print("OK!")


signal.signal(signal.SIGINT, signal_handler)
setup_btle()
