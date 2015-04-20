#import "ViewController.h"
#import "BTLEService.h"

@interface ViewController ()

@property BTLEService *bluetooth;

@end

@implementation ViewController

const unsigned char START[] = {0xAB};
const unsigned char MOTOR_ID[] = {0x01};
const unsigned char EMPTY[] = {0x00};
const unsigned char END[] = {0xEF};

- (id)initWithCoder:(NSCoder *)coder {
    if(self = [super initWithCoder:coder]) {
        self.bluetooth = [[BTLEService alloc] initWithDelegate:self andServiceId:@"713D0000-503E-4C75-BA94-3148F18D941E" andWriteId:@"713D0003-503E-4C75-BA94-3148F18D941E"];
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self.view setUserInteractionEnabled:NO];
    [_bluetooth startSearching];
}

-(IBAction)onRightThumbDragged:(UIPanGestureRecognizer*)recognizer {
    [self handleThumbDragged:recognizer onTrack:_rightTrack];
}

-(IBAction)onLeftThumbDragged:(UIPanGestureRecognizer*)recognizer {
    [self handleThumbDragged:recognizer onTrack:_leftTrack];
}

-(void)handleThumbDragged:(UIPanGestureRecognizer *)recognizer onTrack:(UIView*)track {
    CGPoint translation = [recognizer translationInView:self.view];
    UIView *view = recognizer.view;
    if(recognizer.state == UIGestureRecognizerStateEnded) {
        [UIView animateWithDuration:0.4 delay:0 usingSpringWithDamping:0.3 initialSpringVelocity:1
                            options:UIViewAnimationOptionCurveEaseOut
                         animations:^{ view.center = track.center; }
                         completion:^(BOOL finished) { view.center = track.center; }];
    }
    
    CGFloat newY = view.center.y + translation.y;
    if(newY > track.frame.origin.y && newY < track.frame.origin.y + track.frame.size.height) {
        view.center = CGPointMake(view.center.x, view.center.y + translation.y);
        [recognizer setTranslation:CGPointZero inView:self.view];
    }

    [self sendDriveCommand];
}

- (void)sendDriveCommand {
    NSUInteger leftSpeed = (_leftThumb.center.y / _leftTrack.frame.size.height) * 1024.0;
    NSUInteger rightSpeed = (_rightThumb.center.y / _rightTrack.frame.size.height) * 1024.0;
    NSData *command = [self buildCommandWithLeftSpeed:leftSpeed andRightSpeed:rightSpeed];
    [_bluetooth write:command];
}

- (NSData*)buildCommandWithLeftSpeed:(NSUInteger)leftSpeed andRightSpeed:(NSUInteger)rightSpeed {
    NSMutableData *command = [NSMutableData dataWithCapacity:16];
    [command appendBytes:START length:sizeof(START)/sizeof(char)];
    [command appendBytes:MOTOR_ID length:sizeof(MOTOR_ID)/sizeof(char)];

    //NSUInteger bigEndianLeftSpeed = CFSwapInt32HostToBig((unsigned int)leftSpeed);
    //NSUInteger bigEndianRightSpeed = CFSwapInt32HostToBig((unsigned int)rightSpeed);
    //[command appendBytes:&bigEndianLeftSpeed length:4];
    //[command appendBytes:&bigEndianRightSpeed length:4];
    [command appendBytes:&leftSpeed length:2];
    [command appendBytes:&rightSpeed length:2];

    [command appendBytes:(unsigned char[]){0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00} length:8];
    [command appendBytes:EMPTY length:sizeof(EMPTY)/sizeof(char)];
    [command appendBytes:END length:sizeof(END)/sizeof(char)];
    return command;
}

- (void)onBluetoothServiceConnected {
    [self.view setUserInteractionEnabled:YES];
}

- (void)onBluetoothServiceDisconnected {
    [self.view setUserInteractionEnabled:NO];
}


@end
