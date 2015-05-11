using System;
using System.Collections.Generic;
using Windows.UI.ViewManagement;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Media.Animation;
using Windows.UI.Xaml.Navigation;

namespace sumobot {

    public sealed partial class MainPage : Page {
        private readonly byte[] START = { 0xAB };
        private readonly byte[] MOTOR_ID = { 0x01 };
        private readonly byte[] BUTTON_ID = { 0x02 };
        private readonly byte[] MOTOR_PADDING = { 0x00, 0x00, 0x00, 0x00 };
        private readonly byte[] BUTTON_PADDING = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        private readonly byte[] EMPTY = { 0x00 };
        private readonly byte[] END = { 0xEF };
        private const int TRANSMIT_INTERVAL_MSEC = 10;
        private readonly BluetoothLEService _bluetoothService;
        private readonly DispatcherTimer _timer = new DispatcherTimer();

        public MainPage() {
            InitializeComponent();
            _timer.Interval = new TimeSpan(0, 0, 0, 0, TRANSMIT_INTERVAL_MSEC);
            _timer.Tick += OnWriteTimerTick;
            _bluetoothService = new BluetoothLEService(0xffe0, 0xffe1);
            _bluetoothService.Dispatcher = Dispatcher;
            _bluetoothService.Connected += OnBluetoothConnected;
            _bluetoothService.Disconnected += OnBluetoothDisconnected;
            NavigationCacheMode = NavigationCacheMode.Required;
            HideStatusBar();
            OnShowSettings();
            Loaded += OnLoaded;
        }

        private void OnBluetoothConnected(object sender, EventArgs e) {
            StatusTextBlock.Text = "CONNECTED";
            ProgressRing.Opacity = 0d;
            _timer.Start();
        }

        private void OnWriteTimerTick(object sender, object e) {
            if(_bluetoothService.IsConnected)
                _bluetoothService.Write(BuildCommandMessage());
        }

        private void OnBluetoothDisconnected(object sender, EventArgs e) {
            _timer.Stop();
            StatusTextBlock.Text = "SEARCHING";
            ProgressRing.Opacity = 1d;
        }

        public void OnLoaded(object sender, RoutedEventArgs e) {
            NameTextBox.Focus(FocusState.Keyboard);
        }

        private void OnShowSettings() {
            _bluetoothService.Stop();
            SettingsButton.Visibility = Visibility.Collapsed;
            BackButton.Visibility = Visibility.Visible;
            StatusTextBlock.Opacity = 0d;
            NameTextBox.IsReadOnly = false;
            ProgressRing.Opacity = 0d;
            NameTextBox.Focus(FocusState.Keyboard);
        }

        private void OnLeavingSettings() {
            StatusTextBlock.Text = "SEARCHING";
            ProgressRing.Opacity = 1d;
            SettingsButton.Visibility = Visibility.Visible;
            BackButton.Visibility = Visibility.Collapsed;
            StatusTextBlock.Opacity = 1d;
            NameTextBox.IsReadOnly = true;
            NameTextBox.IsEnabled = false;
            NameTextBox.IsEnabled = true;
            _bluetoothService.Name = NameTextBox.Text;
            _bluetoothService.StartSearching();
        }

        private void OnThumbManipulationDelta(object sender, ManipulationDeltaRoutedEventArgs args) {
            var thumb = (FrameworkElement)sender;
            var translate = (TranslateTransform)thumb.RenderTransform;
            var yTranslation = Math.Max(args.Cumulative.Translation.Y, -LeftTrack.ActualHeight / 2);
            yTranslation = Math.Min(yTranslation, LeftTrack.ActualHeight / 2);
            translate.Y = yTranslation;
        }

        private void OnThumbManipulationCompleted(object sender, ManipulationCompletedRoutedEventArgs args) {
            var thumb = (FrameworkElement)sender;

            var ease = new ElasticEase { Oscillations = 1, Springiness = 1 };
            var animation = new DoubleAnimation { EasingFunction = ease, To = 0 };
            animation.Duration = new Duration(new TimeSpan(0, 0, 0, 0, 175));
            Storyboard.SetTarget(animation, thumb.RenderTransform);
            Storyboard.SetTargetProperty(animation, "Y");
            var storyboard = new Storyboard();
            storyboard.Children.Add(animation);
            storyboard.Begin();
        }

        public async void HideStatusBar() {
            await StatusBar.GetForCurrentView().HideAsync();
        }

        private byte[] BuildCommandMessage() {
            var command = new List<byte>();
            command.AddRange(START);
            command.AddRange(MOTOR_ID);
            command.AddRange(BitConverter.GetBytes(GetLeftSpeed()));
            command.AddRange(BitConverter.GetBytes(GetRightSpeed()));
            command.AddRange(MOTOR_PADDING);
            command.AddRange(EMPTY);
            command.AddRange(END);
            return command.ToArray();
        }

        private UInt32 GetLeftSpeed() {
            var translate = (TranslateTransform)LeftThumb.RenderTransform;
            double yRatio = translate.Y / (LeftTrack.ActualHeight / 2d);
            double speed = (yRatio * 512d) + 512d;
            speed = Math.Min(speed, 1024d);
            speed = Math.Max(speed, 0d);
            return (UInt32)speed;
        }

        private UInt32 GetRightSpeed() {
            var translate = (TranslateTransform)RightThumb.RenderTransform;
            double yRatio = translate.Y / (RightTrack.ActualHeight / 2d);
            double speed = (yRatio * 512d) + 512d;
            speed = Math.Min(speed, 1024d);
            speed = Math.Max(speed, 0d);
            return (UInt32)speed;
        }

        private void OnSettingsTapped(object sender, RoutedEventArgs e) {
            OnShowSettings();
        }

        private void OnBackTapped(object sender, RoutedEventArgs e) {
            OnLeavingSettings();
        }

        private void OnTextLostFocus(object sender, RoutedEventArgs e) {
            NameTextBox.Text = NameTextBox.Text.ToUpper();
        }
    }
}
