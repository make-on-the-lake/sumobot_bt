#import "ViewController.h"
#import "BTLEService.h"

@interface ViewController ()

@property BTLEService *bluetooth;
@property dispatch_source_t timer;
@property BOOL buttonState;

@end

@implementation ViewController

const unsigned char START[] = {0xAB};
const unsigned char MOTOR_ID[] = {0x01};
const unsigned char BUTTON_ID[] = {0x02};
const unsigned char MOTOR_PADDING[] = {0x00, 0x00, 0x00, 0x00};
const unsigned char BUTTON_PADDING[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
const unsigned char EMPTY[] = {0x00};
const unsigned char END[] = {0xEF};
const double TRANSMIT_INTERVAL_SEC = 0.1;

- (id)initWithCoder:(NSCoder *)coder {
    if(self = [super initWithCoder:coder]) {
        self.bluetooth = [[BTLEService alloc] initWithDelegate:self andServiceId:@"FFE0" andWriteId:@"FFE1"];
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self showSettings];
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

- (NSData*)buildCommandWithLeftSpeed:(unsigned int)leftSpeed andRightSpeed:(unsigned int)rightSpeed {
    NSMutableData *command = [NSMutableData dataWithCapacity:16];
    [command appendBytes:START length:sizeof(START)/sizeof(char)];
    [command appendBytes:MOTOR_ID length:sizeof(MOTOR_ID)/sizeof(char)];

    [command appendBytes:&leftSpeed length:4];
    [command appendBytes:&rightSpeed length:4];

    [command appendBytes:MOTOR_PADDING length:sizeof(MOTOR_PADDING)/sizeof(char)];
    [command appendBytes:EMPTY length:sizeof(EMPTY)/sizeof(char)];
    [command appendBytes:END length:sizeof(END)/sizeof(char)];
    return command;
}

-(IBAction)onGpioTapped:(id)sender {
    self.buttonState = !self.buttonState;
    NSData *command = [self buildCommandForButton:0 andState:_buttonState];
    [_bluetooth write:command];
}

- (NSData*)buildCommandForButton:(unsigned int)buttonIndex andState:(BOOL)isHigh {
    NSMutableData *command = [NSMutableData dataWithCapacity:16];
    [command appendBytes:START length:sizeof(START)/sizeof(char)];
    [command appendBytes:BUTTON_ID length:sizeof(BUTTON_ID)/sizeof(char)];
    
    [command appendBytes:&buttonIndex length:1];
    [command appendBytes:&isHigh length:1];
    
    [command appendBytes:BUTTON_PADDING length:sizeof(BUTTON_PADDING)/sizeof(char)];
    [command appendBytes:EMPTY length:sizeof(EMPTY)/sizeof(char)];
    [command appendBytes:END length:sizeof(END)/sizeof(char)];
    return command;
}

- (void)onBluetoothServiceConnected {
    [self startCommandTimer];
    [self showConnected];
}

- (void)onBluetoothServiceDisconnected {
    [self stopCommandTimer];
    [self showSearching];
}

- (void)onLeavingSettings {
    _bluetooth.name = _name.text;
    [_bluetooth startSearching];
    [self showSearching];
}

-(IBAction)onSettingsTapped:(id)sender {
    [self stopCommandTimer];
    [_bluetooth stop];
    [self showSettings];
}

-(IBAction)onBackTapped:(id)sender {
    [self onLeavingSettings];
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [self onLeavingSettings];
    return YES;
}

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSRange lowercaseCharRange = [string rangeOfCharacterFromSet:[NSCharacterSet lowercaseLetterCharacterSet]];
    if (lowercaseCharRange.location != NSNotFound) {
        textField.text = [textField.text stringByReplacingCharactersInRange:range withString:[string uppercaseString]];
        return NO;
    }

    return YES;
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
    if(_timer)
        dispatch_source_cancel(_timer);
}

- (void)showSettings {
    [_spinner stopAnimating];
    _status.hidden = YES;
    _settingsButton.hidden = YES;
    _backButton.hidden = NO;
    _name.enabled = YES;
    [_name becomeFirstResponder];
}

- (void)showConnected {
    _name.enabled = NO;
    [_name resignFirstResponder];
    [_spinner stopAnimating];
    _status.hidden = NO;
    _status.text = @"CONNECTED";
    _settingsButton.hidden = NO;
    _backButton.hidden = YES;
}

- (void)showSearching {
    _name.enabled = NO;
    [_name resignFirstResponder];
    [_spinner startAnimating];
    _spinner.hidden = NO;
    _status.hidden = NO;
    _status.text = @"SEARCHING";
    _settingsButton.hidden = NO;
    _backButton.hidden = YES;
}

- (NSUInteger)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskLandscape;
}

- (BOOL)prefersStatusBarHidden {
    return YES;
}

@end
