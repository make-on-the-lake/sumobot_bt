using System;
using System.Diagnostics;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Devices.Enumeration;
using Windows.Storage.Streams;
using Windows.UI.Core;

namespace sumobot {

    public class BluetoothLEService {

        public BluetoothLEService(ushort serviceId, ushort writeId) {
            _serviceId = serviceId;
            _writeId = writeId;
        }

        private readonly ushort _serviceId;
        private readonly ushort _writeId;
        private GattDeviceService _connectedService;

        public event EventHandler Connected;
        public event EventHandler Disconnected;

        public string Name { get; set; }
        public CoreDispatcher Dispatcher { get; set; }

        public async Task StartSearching() {
            try {
                var deviceSelector = GattDeviceService.GetDeviceSelectorFromShortId(_serviceId);
                var devices = await DeviceInformation.FindAllAsync(deviceSelector);
                foreach (var device in devices) {
                    if (String.Equals(device.Name, Name, StringComparison.CurrentCultureIgnoreCase)) {
                        _connectedService = await GattDeviceService.FromIdAsync(device.Id);
                        if (_connectedService != null) {
                            _connectedService.Device.ConnectionStatusChanged += OnServiceConnectionStatusChanged;
                            OnConnected();
                            return;
                        }
                    }
                }

                await StartSearching();
            }
            catch (Exception ex) {

                throw ex;
            }
        }

        private void OnServiceConnectionStatusChanged(BluetoothLEDevice sender, object args) {
            if (sender.ConnectionStatus == BluetoothConnectionStatus.Disconnected) {
                Stop();
                StartSearching();
            }
        }

        public void Stop() {
            if(_connectedService != null)
                _connectedService.Device.ConnectionStatusChanged -= OnServiceConnectionStatusChanged;
            _connectedService = null;
            OnDisconnected();
        }

        public async void Write(byte[] data) {
            if (!IsConnected)
                return;

            var writeCharacterisic = _connectedService.GetCharacteristics(GattCharacteristic.ConvertShortIdToUuid(_writeId))[0];
            var dataWriter = new DataWriter();
            dataWriter.WriteBytes(data);
            await writeCharacterisic.WriteValueAsync(dataWriter.DetachBuffer(), GattWriteOption.WriteWithoutResponse);
        }

        public bool IsConnected {
            get { return _connectedService != null; }
        }

        protected virtual void OnConnected() {
            var handler = Connected;
            if (handler != null) handler(this, EventArgs.Empty);
        }

        protected virtual void OnDisconnected() {
            var handler = Disconnected;
            if (handler != null) handler(this, EventArgs.Empty);
        }
    }
}
