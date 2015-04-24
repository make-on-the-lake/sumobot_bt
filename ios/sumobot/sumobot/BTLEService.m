#import "BTLEService.h"

@interface BTLEService ()

@property(nonatomic) id <BTLEServiceDelegate> delegate;
@property(nonatomic) CBUUID *serviceId;
@property(nonatomic) CBUUID *writeId;
@property(nonatomic) CBCentralManager *bluetoothManager;
@property(nonatomic) CBPeripheral *connectedPeripheral;

@end

@implementation BTLEService

- (id)initWithDelegate:(id <BTLEServiceDelegate>)delegate andServiceId:(NSString *)serviceId andWriteId:(NSString *)writeId {
    if (self = [super init]) {
        self.serviceId = [CBUUID UUIDWithString:serviceId];
        self.writeId = [CBUUID UUIDWithString:writeId];
        self.delegate = delegate;
    }
    return self;
}

- (void)startSearching {
    self.bluetoothManager = [[CBCentralManager alloc] initWithDelegate:self queue:dispatch_queue_create("com.makeonthelake.sumobot.bluetooth", NULL)];
}

- (BOOL)isConnected {
    return _connectedPeripheral != nil;
}

- (void)write:(NSData *)data {
    if(_connectedPeripheral == nil)
        return;

    CBService *service = _connectedPeripheral.services.firstObject;
    CBCharacteristic *writeCharacteristic = service.characteristics.firstObject;
    if (!service || !writeCharacteristic)
        return;

    [_connectedPeripheral writeValue:data forCharacteristic:writeCharacteristic type:CBCharacteristicWriteWithoutResponse];
}

- (void)stop {
    [_bluetoothManager stopScan];
}

- (void)startSearchingForPeripherals {
    NSArray *connectedPeripherals = [_bluetoothManager retrieveConnectedPeripheralsWithServices:@[_serviceId]];
    if (connectedPeripherals.count > 0)
        [_bluetoothManager connectPeripheral:connectedPeripherals.firstObject options:nil];

    [_bluetoothManager scanForPeripheralsWithServices:@[_serviceId] options:@{CBCentralManagerScanOptionAllowDuplicatesKey : @YES}];
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    if (central.state == CBCentralManagerStatePoweredOn)
        [self startSearchingForPeripherals];
}

- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)rssi {
    NSString *name = [[advertisementData valueForKey:CBAdvertisementDataLocalNameKey] lowercaseString];
    if(![name isEqualToString:@"sumobot_bt"])
        return;
    
    self.connectedPeripheral = peripheral;
    [_connectedPeripheral setDelegate:self];
    [_bluetoothManager connectPeripheral:_connectedPeripheral options:nil];

    [_bluetoothManager stopScan];
}

- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    [peripheral discoverServices:@[_serviceId]];
}

- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    [_bluetoothManager scanForPeripheralsWithServices:@[_serviceId] options:@{CBCentralManagerScanOptionAllowDuplicatesKey : @YES}];
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    dispatch_async(dispatch_get_main_queue(), ^{
        [_delegate onBluetoothServiceDisconnected];
    });

    [self startSearchingForPeripherals];
}

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    for (CBService *service in peripheral.services) {
        if ([service.UUID isEqual:_serviceId]) {
            [peripheral discoverCharacteristics:@[_writeId] forService:service];
        }
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
    for (CBCharacteristic *characteristic in service.characteristics) {
        if ([characteristic.UUID isEqual:_writeId]) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [_delegate onBluetoothServiceConnected];
            });
        }
    }
}

@end