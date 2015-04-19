#import <Foundation/Foundation.h>

@protocol BTLEServiceDelegate <NSObject>

-(void) onBluetoothServiceConnected;
-(void) onBluetoothServiceDisconnected;

@end