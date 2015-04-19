#import <UIKit/UIKit.h>
#import "BTLEServiceDelegate.h"

@interface ViewController : UIViewController<BTLEServiceDelegate>

@property IBOutlet UIImageView *leftThumb;
@property IBOutlet UIImageView *rightThumb;
@property IBOutlet UIImageView *leftTrack;
@property IBOutlet UIImageView *rightTrack;

-(IBAction)onLeftThumbDragged:(UIPanGestureRecognizer*)recognizer;
-(IBAction)onRightThumbDragged:(UIPanGestureRecognizer*)recognizer;

@end

