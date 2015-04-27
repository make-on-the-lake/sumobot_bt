#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "BTLEServiceDelegate.h"

@interface BTLEService : NSObject <CBCentralManagerDelegate, CBPeripheralDelegate>

@property(nonatomic) NSTimeInterval scanTimeout;
@property(nonatomic) NSString *name;

- (id)initWithDelegate:(id <BTLEServiceDelegate>)delegate andServiceId:(NSString *)serviceId andWriteId:(NSString *)writeId;
- (void)startSearching;
- (void)write:(NSData *)data;
- (void)stop;
- (BOOL)isConnected;

@end