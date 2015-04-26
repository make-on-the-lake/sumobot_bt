# Sumobot BT

## Attribution

This project is based heavily on the wonderful [sumobot-jr](https://github.com/makenai/sumobot-jr) project. Thanks to the maintainers for some awesome resources!

## Communication Protocol

All messages are 16 bytes, which is under the maximum size of 20 for a single message payload over Bluetooth LE.

They are in this format:

**Bytes** | **Section**
----------|------------
1         | Start Byte
1         | Message ID
12        | Message Data
1         | Parity Byte
1         | End Byte

### Start and End Bytes
Start byte is 0xAB
End byte is 0xEF

### Messages

#### Drive
Message ID: 1 (0x01)
Controls the motors individually.

**Message Format**

**Bytes** | **Section**
----------|------------
1         | Start (0xAB)
1         | Message ID (0x01)
4         | Left Motor
4         | Right Motor
4         | Padding (Zeroes)
1         | Parity Byte
1         | End (0xEF)

Left Motor and Right Motor are 4-byte unsigned ints, with a range from 0 to 1024. 
512 is resting, 0 is full reverse, and 1024 is full forward.

#### Button
Message ID: 2 (0x02)
Toggles a general usage GPIO pin high or low.

**Message Format**

**Bytes** | **Section**
----------|------------
1         | Start (0xAB)
1         | Message ID (0x02)
1         | Button ID
1         | GPIO Status
10        | Padding (Zeroes)
1         | Parity Byte
1         | End (0xEF)

GPIO Status is 0 if low, 1 if high.
