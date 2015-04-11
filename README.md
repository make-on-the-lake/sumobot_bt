# Sumobot BT

## Communication Protocol

All messages are 20 bytes, which is the maximum size for a single message payload over Bluetooth LE.
They are in this format:

**Bytes** | **Section**
----------|------------
2         | Message ID
17        | Message Data
1         | Parity Bit

### Messages

#### Drive
Message ID: 1
Controls the motors individually.

**Message Format**

**Bytes** | **Section**
----------|------------
2         | Message ID (1)
2         | Left Motor
2         | Right Motor
13        | Padding (Zeroes)
1         | Parity Bit

Left Motor and Right Motor are 2-byte ints, with a range from 0 to 1024. 
512 is resting, 0 is full reverse, and 1024 is full forward.

#### Button
Message ID: 2
Toggles a general usage GPIO pin high or low.

**Message Format**

**Bytes** | **Section**
----------|------------
2         | Message ID (2)
1         | GPIO Status
16        | Padding (Zeroes)
1         | Parity Bit

GPIO Status is 0 if low, 1 if high.
