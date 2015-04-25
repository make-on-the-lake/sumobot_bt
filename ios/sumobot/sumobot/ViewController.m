#import "ViewController.h"
#import "BTLEService.h"

@interface ViewController ()

@property BTLEService *bluetooth;
@property dispatch_source_t timer;

@end

@implementation ViewController

const unsigned char START[] = {0xAB};
const unsigned char MOTOR_ID[] = {0x01};
const unsigned char MOTOR_PADDING[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
const unsigned char EMPTY[] = {0x00};
const unsigned char END[] = {0xEF};
const int TRANSMIT_INTERVAL_SEC = 0.1;

- (id)initWithCoder:(NSCoder *)coder {
    if(self = [super initWithCoder:coder]) {
        self.bluetooth = [[BTLEService alloc] initWithDelegate:self andServiceId:@"FFE0" andWriteId:@"FFE1"];
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    //[self.view setUserInteractionEnabled:NO];
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
                         completion:nil];
    } else {
        CGFloat newY = view.center.y + translation.y;
        if(newY > track.frame.origin.y && newY < track.frame.origin.y + track.frame.size.height) {
            view.center = CGPointMake(view.center.x, view.center.y + translation.y);
            [recognizer setTranslation:CGPointZero inView:self.view];
        }
    }
}

- (void)sendDriveCommand {
    unsigned short leftSpeed = ((_leftThumb.center.y - _leftTrack.frame.origin.y) / _leftTrack.frame.size.height) * 1024.0;
    unsigned short rightSpeed = ((_rightThumb.center.y - _rightTrack.frame.origin.y) / _rightTrack.frame.size.height) * 1024.0;
    leftSpeed = MIN(leftSpeed, 1024);
    rightSpeed = MIN(rightSpeed, 1024);
    NSData *command = [self buildCommandWithLeftSpeed:leftSpeed andRightSpeed:rightSpeed];
    [_bluetooth write:command];
}

- (NSData*)buildCommandWithLeftSpeed:(unsigned short)leftSpeed andRightSpeed:(unsigned short)rightSpeed {
    NSMutableData *command = [NSMutableData dataWithCapacity:16];
    [command appendBytes:START length:sizeof(START)/sizeof(char)];
    [command appendBytes:MOTOR_ID length:sizeof(MOTOR_ID)/sizeof(char)];

    [command appendBytes:&leftSpeed length:2];
    [command appendBytes:&rightSpeed length:2];

    [command appendBytes:MOTOR_PADDING length:sizeof(MOTOR_PADDING)/sizeof(char)];
    [command appendBytes:EMPTY length:sizeof(EMPTY)/sizeof(char)];
    [command appendBytes:END length:sizeof(END)/sizeof(char)];
    return command;
}

- (void)onBluetoothServiceConnected {
    [self.view setUserInteractionEnabled:YES];
    [self startCommandTimer];
}

- (void)onBluetoothServiceDisconnected {
    [self.view setUserInteractionEnabled:NO];
    [self stopCommandTimer];
}

- (void)startCommandTimer {
    _timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, dispatch_get_main_queue());
    dispatch_source_set_timer(_timer, dispatch_time(DISPATCH_TIME_NOW, TRANSMIT_INTERVAL_SEC * NSEC_PER_SEC), TRANSMIT_INTERVAL_SEC * NSEC_PER_SEC, (1ull * NSEC_PER_SEC) / 10);
    dispatch_source_set_event_handler(_timer, ^{
        [self sendDriveCommand];
    });
    dispatch_resume(_timer);
}

- (void)stopCommandTimer {
    dispatch_source_cancel(_timer);
}

@end
