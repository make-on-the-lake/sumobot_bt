import UIKit

class ViewController: UIViewController {
    
    @IBOutlet weak var leftThumb: UIImageView!
    @IBOutlet weak var rightThumb: UIImageView!
    @IBOutlet weak var leftTrack: UIImageView!
    @IBOutlet weak var rightTrack: UIImageView!

    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    @IBAction func onLeftThumbDragged(recognizer:UIPanGestureRecognizer) {
        let translation = recognizer.translationInView(self.view);
        if let view = recognizer.view {
            if recognizer.state == UIGestureRecognizerState.Ended {
                UIView.animateWithDuration(
                    0.4, delay: 0,
                    usingSpringWithDamping: 0.3,
                    initialSpringVelocity: 1,
                    options: UIViewAnimationOptions.CurveEaseOut,
                    animations: {() in view.center = self.leftTrack.center},
                    completion: nil)
                return
            }

            var newY = view.center.y + translation.y
            if newY > leftTrack.frame.origin.y && newY < leftTrack.frame.origin.y + leftTrack.frame.size.height {
                view.center = CGPoint(x:view.center.x, y:view.center.y + translation.y)
                recognizer.setTranslation(CGPointZero, inView:self.view)
            }
        }
    }
    
    @IBAction func onRightThumbDragged(recognizer:UIPanGestureRecognizer) {
        let translation = recognizer.translationInView(self.view);
        if let view = recognizer.view {
            if recognizer.state == UIGestureRecognizerState.Ended {
                UIView.animateWithDuration(
                    0.4, delay: 0,
                    usingSpringWithDamping: 0.3,
                    initialSpringVelocity: 1,
                    options: UIViewAnimationOptions.CurveEaseOut,
                    animations: {() in view.center = self.rightTrack.center},
                    completion: nil)
                return
            }

            var newY = view.center.y + translation.y
            if newY > rightTrack.frame.origin.y && newY < rightTrack.frame.origin.y + rightTrack.frame.size.height {
                view.center = CGPoint(x:view.center.x, y:view.center.y + translation.y)
                recognizer.setTranslation(CGPointZero, inView:self.view)
            }
        }
    }
}

