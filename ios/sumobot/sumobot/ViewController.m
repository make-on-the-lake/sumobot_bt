#import "ViewController.h"
#import "BTLEService.h"

@interface ViewController ()

@property BTLEService *bluetooth;

@end

@implementation ViewController

const unsigned char START[] = {0xAB};
const unsigned char MOTOR_ID[] = {0x01};
const unsigned char BUTTON_ID[] = {0x02};
const unsigned char END[] = {0xEF};

- (id)initWithCoder:(NSCoder *)coder {
    if(self = [super initWithCoder:coder]) {
        self.bluetooth = [[BTLEService alloc] initWithDelegate:self andServiceId:@"" andWriteId:@""];
    }
    return self;
}


- (void)viewDidLoad {
    [super viewDidLoad];
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
                         completion:^(BOOL finished) {}];
    }
    
    CGFloat newY = view.center.y + translation.y;
    if(newY > track.frame.origin.y && newY < track.frame.origin.y + track.frame.size.height) {
        view.center = CGPointMake(view.center.x, view.center.y + translation.y);
        [recognizer setTranslation:CGPointZero inView:self.view];
    }
}

- (void)onBluetoothServiceConnected {

}

- (void)onBluetoothServiceDisconnected {

}


@end
