using System;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Media.Animation;
using Windows.UI.Xaml.Navigation;

namespace sumobot {

    public sealed partial class MainPage : Page {

        public MainPage() {
            InitializeComponent();
            NavigationCacheMode = NavigationCacheMode.Required;
            HideStatusBar();

            LeftThumb.ManipulationMode = ManipulationModes.TranslateY;
            RightThumb.ManipulationMode = ManipulationModes.TranslateY;
            LeftThumb.ManipulationDelta += OnThumbManipulationDelta;
            RightThumb.ManipulationDelta += OnThumbManipulationDelta;
            LeftThumb.ManipulationCompleted += OnThumbManipulationCompleted;
            RightThumb.ManipulationCompleted += OnThumbManipulationCompleted;
        }

        private void OnThumbManipulationDelta(object sender, ManipulationDeltaRoutedEventArgs args) {
            var thumb = (FrameworkElement) sender;
            var translate = (TranslateTransform) thumb.RenderTransform;
            var yTranslation = Math.Max(args.Cumulative.Translation.Y, -LeftTrack.ActualHeight/2);
            yTranslation = Math.Min(yTranslation, LeftTrack.ActualHeight/2);
            translate.Y = yTranslation;
        }

        private void OnThumbManipulationCompleted(object sender, ManipulationCompletedRoutedEventArgs args) {
            var thumb = (FrameworkElement) sender;

            var animation = new DoubleAnimation();
            var ease = new ElasticEase();
            ease.Oscillations = 1;
            ease.Springiness = 1;
            animation.EasingFunction = ease;
            animation.To = 0;
            animation.Duration = new Duration(new TimeSpan(0,0,0,0,175));
            Storyboard.SetTarget(animation, thumb.RenderTransform);
            Storyboard.SetTargetProperty(animation, "Y");
            var storyboard = new Storyboard();
            storyboard.Children.Add(animation);
            storyboard.Begin();
        }

        public async void HideStatusBar() {
            await Windows.UI.ViewManagement.StatusBar.GetForCurrentView().HideAsync();
        }

        protected override void OnNavigatedTo(NavigationEventArgs args) {
        }
    }
}
