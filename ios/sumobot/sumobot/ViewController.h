#import <UIKit/UIKit.h>
#import "BTLEServiceDelegate.h"

@interface ViewController : UIViewController<BTLEServiceDelegate, UITextFieldDelegate>

@property IBOutlet UIImageView *leftThumb;
@property IBOutlet UIImageView *rightThumb;
@property IBOutlet UIImageView *leftTrack;
@property IBOutlet UIImageView *rightTrack;
@property IBOutlet UITextField *name;
@property IBOutlet UILabel *status;
@property IBOutlet UIActivityIndicatorView *spinner;
@property IBOutlet UIButton *settingsButton;
@property IBOutlet UIButton *backButton;

-(IBAction)onSettingsTapped:(id)sender;
-(IBAction)onBackTapped:(id)sender;
-(IBAction)onGpioTapped:(id)sender;

-(IBAction)onLeftThumbDragged:(UIPanGestureRecognizer*)recognizer;
-(IBAction)onRightThumbDragged:(UIPanGestureRecognizer*)recognizer;

@end

