## <span style="color: red;">This repository is forked form the original. The README is not updated.</span>

## <span style="color: red;">This repository is considered as unstable for production.</span>

# React Native Bluetooth Serial Next :)

[![npm](https://img.shields.io/npm/v/react-native-bluetooth-serial-next.svg?style=popout-square)](https://www.npmjs.com/package/react-native-bluetooth-serial-next) [![NpmLicense](https://img.shields.io/npm/l/react-native-bluetooth-serial-next.svg?style=popout-square)](https://github.com/nuttawutmalee/react-native-bluetooth-serial-next) [![Dependency Status](https://img.shields.io/david/nuttawutmalee/react-native-bluetooth-serial-next.svg?style=popout-square)](https://david-dm.org/nuttawutmalee/react-native-bluetooth-serial-next) [![NPM Downloads](https://img.shields.io/npm/dt/react-native-bluetooth-serial-next.svg?style=popout-square)](https://www.npmjs.com/package/react-native-bluetooth-serial-next)

React Native version of [BluetoothSerial](https://github.com/don/BluetoothSerial) plugin for both Android and iOS. Pulled from [React Native Bluetooth Serial](https://github.com/rusel1989/react-native-bluetooth-serial).

For iOS, this module now support [service declaration](#service-declaration-for-ios), by default, those services are Read Bear Lab, Adafruit BLE, Bluegiga, Laird Virtual Serial Port, and Rongta.

## Table of Contents

- [Getting started](#getting-started)
- [Example](#example)
- [API References](#api-references)
  - [Device object](#device-object)
  - [Service object](#service-object)
  - [High order component](#high-order-component)
  - [Methods](#methods)
- [Multiple devices connection](#multiple-devices-connection)
- [Events](#events)
- [Todos](#todos)

## Getting started

```bash
npm install react-native-bluetooth-serial-next --save
react-native link react-native-bluetooth-serial-next
```

For Android, you need to put the following code to `AndroidManifest.xml` in `android/app/src/main` at your project root folder.

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Manual Installation

#### iOS

1. `npm install react-native-bluetooth-serial-next --save`
2. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
3. Go to `node_modules` ➜ `react-native-bluetooth-serial-next` and add `RCTBluetoothSerial.xcodeproj`
4. In XCode, in the project navigator, select your project. Add `libRCTBluetoothSerial.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
5. Click `RCTBluetoothSerial.xcodeproj` in the project navigator and go the `Build Settings` tab. Make sure 'All' is toggled on (instead of 'Basic'). In the `Search Paths` section, look for `Header Search Paths` and make sure it contains both `$(SRCROOT)/../../react-native/React` and `$(SRCROOT)/../../../React` - mark both as `recursive`.
6. Run your project (`Cmd+R`)

#### Android

1. `npm install react-native-bluetooth-serial-next --save`
2. Open up `android/app/src/main/java/[...]/MainActivity.java` or `MainApplication.java` for React Native >= 0.29
   <br />
   - Add `import com.nuttawutmalee.RCTBluetoothSerial.*;` to the imports at the top of the file
   - Add `new RCTBluetoothSerialPackage()` to the list returned by the `getPackages()` method
     <br />
3. Append the following lines to `android/settings.gradle`
   <br />
   ```groovy
   include ':react-native-bluetooth-serial-next'
   project(':react-native-bluetooth-serial-next').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-bluetooth-serial-next/android')
   ```
4. Insert the following lines inside the dependencies block in `android/app/build.gradle`
   <br />
   ```groovy
   compile project(':react-native-bluetooth-serial-next')
   ```

## Example

1. `git clone https://github.com/nuttawutmalee/react-native-bluetooth-serial-next.git`
2. `cd react-native-bluetooth-serial-next/example`
3. `npm install && npm link ../`
4. `npm start`
5. `react-native run-ios` or `react-native run-android`

## API References

### Device object

This is basically the result object from API methods depending on operation system.

**iOS**

```js
{
    id: '111-111-111-111',
    uuid: '111-111-111-111',
    name: 'Bluetooth Printer',
    rssi: 'This field might not be present in the object',
}
```

**Android**

```js
{
    id: '111-111-111-111',
    address: '111-111-111-111',
    name: 'Bluetooth Printer',
    class: 'This field might not be present in the object',
}
```

### Service object

**iOS**

```js
{
    name: 'This field might not be present in the object',
    service: 'BLE service UUID string',
    read: 'BLE read characteristic UUID string',
    write: 'BLE write characteristic UUID string',
}
```

### High order component

#### withSubscription( options : <span style="color:#999;">Object</span> ) : <span style="color:#999;">React.Component</span>

This method will create an event listener and send it though as a component prop and it will remove all event listeners on `componentWillUnmount` as well.

- options : <span style="color:#999;">Object</span>
  - subscriptionName : <span style="color:#999;">String</span> = `'subscription'`
    The event listener prop name.
  - destroyOnWillUnmount : <span style="color:#999;">Boolean</span> = `true`
    Should event listeners remove all listeners and subscription

```js
class MyComponent extends React.Component {
    ...
}

export default withSubscription({
    subscriptionName: 'events',
    destroyOnWillUnmount: true,
})(MyComponent);
```

### Methods

- [Bluetooth adapter](#bluetooth-adapter)
- [Device pairing](#device-pairing)
- [Device connection](#device-connection)
- [Device IO](#device-io)
- [Device buffer](#device-buffer)
- [Service declaration for iOS](#service-declaration-for-ios)

#### Bluetooth adapter

##### requestEnable() : <span style="color:#999;">Promise\<Boolean></span>

Prompts the application device to enable bluetooth adapter.

- For iOS, this method will throw an error.
- For Android, if the user does not enable bluetooth upon request, it will throw an error.

```js
await BluetoothSerial.requestEnable();
```

##### enable() : <span style="color:#999;">Promise\<Boolean></span>

Enable bluetooth adapter service.

- For iOS, this method will throw an error.

```js
await BluetoothSerial.enable();
```

##### disable() : <span style="color:#999;">Promise\<Boolean></span>

Disable bluetooth adapter service.

- For iOS, this method will throw an error.

```js
await BluetoothSerial.disable();
```

##### isEnabled() : <span style="color:#999;">Promise\<Boolean></span>

Indicates bluetooth adapter service status.

```js
const isEnabled = await BluetoothSerial.isEnabled();
```

##### list() : <span style="color:#999;">Promise\<[Device](#device-object)[]></span>

List all paired (Android) or connected (iOS) bluetooth devices.

```js
const devices = await BluetoothSerial.list();
```

##### listUnpaired() : <span style="color:#999;">Promise\<[Device](#device-object)[]></span> | discoverUnpairedDevices() : <span style="color:#999;">Promise\<[Device](#device-object)[]></span>

List all unpaired bluetooth devices.

```js
const devices = await BluetoothSerial.listUnpaired();
const devices = await BluetoothSerial.discoverUnpairedDevices();
```

##### cancelDiscovery() : <span style="color:#999;">Promise\<Boolean></span> | stopScanning() : <span style="color:#999;">Promise\<Boolean></span>

Cancel bluetooth device discovery process.

```js
await BluetoothSerial.cancelDiscovery();
await BluetoothSerial.stopScanning();
```

##### setAdapterName( name : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<String></span>

Give bluetooth adapter a new name.

- name : <span style="color:#999;">String</span>
  Bluetooth adapter new name.
- For iOS, this method will throw an error.

```js
const newName = await BluetoothSerial.setAdapterName("New Adapter Name");
```

---

#### Device pairing

##### pairDevice( id : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<[Device](#device-object) | null></span>

Pair with a bluetooth device.

- id : <span style="color:#999;">String</span>
  Device id or uuid.
- For iOS, this method will throw an error.

```js
const device = await BluetoothSerial.pairDevice(id);
```

##### unpairDevice( id : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<[Device](#device-object) | null></span>

Unpair from a bluetooth device.

- id : <span style="color:#999;">String</span>
  Device id or uuid.
- For iOS, this method will throw an error.

```js
const device = await BluetoothSerial.unpairDevice(id);
```

---

#### Device connection

##### connect( id : <span style="color:#999;">String</span> ): <span style="color:#999;">Promise\<[Device](#device-object)></span>

Connect to a specific bluetooth device.

- id : <span style="color:#999;">String</span>
  Device id or uuid.

```js
const device = await BluetoothSerial.connect(id);
```

##### disconnect( id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<Boolean></span>

Disconnect from the specific connected bluetooth device. If `id` is omitted, the first connected device will be disconnected.

- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
await BluetoothSerial.disconnect();
```

##### disconnectAll() : <span style="color:#999;">Promise\<Boolean></span>

Disconnect all connected bluetooth devices.

```js
await BluetoothSerial.disconnectAll();
```

##### isConnected( id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<Boolean></span>

Indicates the specific connected bluetooth device connection status. If `id` is omitted, it will return the connection status of the first connected device.

- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
const isConnected = await BluetoothSerial.isConnected();
```

---

#### Device IO

##### read( callback : <span style="color:#999;">Function</span>, delimiter? : <span style="color:#999;">String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">void</span>

Listen and read data from the selected or first connected device.

- callback : <span style="color:#999;">Function</span>
  - data : <span style="color:#999;">String</span>
  - subscription : <span style="color:#999;">EmitterSubscription</span>
- delimiter? : <span style="color:#999;">String</span> = `''`
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
BluetoothSerial.read((data, subscription) => {
  console.log(data);

  if (this.imBoredNow && subscription) {
    BluetoothSerial.removeSubscription(subscription);
  }
}, "\r\n");
```

##### readOnce( delimiter? : <span style="color:#999;">String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<String></span>

Read data from the selected or first connected device once.

- delimiter? : <span style="color:#999;">String</span> = `''`
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
const data = await BluetoothSerial.readOnce("\r\n");
```

##### readEvery( callback : <span style="color:#999;">Function</span>, ms? : <span style="color:#999;">Number</span>, delimiter? : <span style="color:#999;">String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">void</span>

Read data from the selected or first connected device every n ms.

- callback : <span style="color:#999;">Function</span>
  - data : <span style="color:#999;">String</span>
  - intervalId : <span style="color:#999;">Number</span>
- ms?: <span style="color:#999;">Number</span> = `1000`
- delimiter? : <span style="color:#999;">String</span> = `''`
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
BluetoothSerial.readEvery(
  (data, intervalId) => {
    console.log(data);

    if (this.imBoredNow && intervalId) {
      clearInterval(intervalId);
    }
  },
  5000,
  "\r\n"
);
```

##### readFromDevice( id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<String></span>

Read all buffer data from the selected or first connected device.

- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
const data = await BluetoothSerial.readFromDevice();
```

##### readUntilDelimiter( delimiter : <span style="color:#999;">String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<String></span>

Read all buffer data up to certain delimiter from the selected or first connected device.

- delimiter : <span style="color:#999;">String</span>
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
const data = await BluetoothSerial.readUntilDelimiter("\r\n");
```

##### write( data : <span style="color:#999;">Buffer | String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<Boolean></span>

Write buffer or string to the selected or first connected device.

- data : <span style="color:#999;">Buffer | String</span>
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
await BluetoothSerial.write("This is the test message");
```

##### writeToDevice( data : <span style="color:#999;">String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<Boolean></span>

Write string to the selected or first connected device.

- data : <span style="color:#999;">String</span>
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
await BluetoothSerial.writeToDevice("This is the test message");
```

---

#### Device buffer

##### clear( id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<Boolean></span>

Clear all buffer data of the selected or first connected device.

- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
await BluetoothSerial.clear();
```

##### available( id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<Number></span>

Get length of current buffer data of the selected or first connected device.

- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
const bufferLength = await BluetoothSerial.available();
```

##### withDelimiter( delimiter : <span style="color:#999;">String</span>, id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Promise\<String | null></span>

Set delimiter that will split the buffer data when you are reading from device.

- delimiter : <span style="color:#999;">String</span>
- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

```js
const deviceId = await BluetoothSerial.withDelimiter("\r\n");
```

---

#### Service declaration for iOS

You can get the default services which are Read Bear Lab, Adafruit BLE, Bluegiga, Laird Virtual Serial Port, and Rongta via `BluetoothSerial.DEFAULT_SERVICES` array.

##### setServices( services : <span style="color:#999;">[Service](#service-object)[]</span>, includeDefaultServices? : <span style="color:#999;">Boolean</span> ) : <span style="color:#999;">Promise\<[Service](#service-object)[]></span>

Set custom bluetooth services for filtering out the specific protocols that you need.

- For Android, this method has no effect and will return empty array.
- services : <span style="color:#999;">[Service](#service-object)[]</span>
  Array of custom service object; each of them must have `service`, `read`, and `write` key-value.
- includeDefaultServices? : <span style="color:#999;">Boolean</span> = `true`
  Should we include `BluetoothSerial.DEFAULT_SERVICES` in the services array?

```js
const updatedServices = await BluetoothSerial.setServices([
    name: 'Custom Bluetooth Service',
    service: '111-111-111-111-111-111',
    read: '111-111-111-111-111-111',
    write: '111-111-111-111-111-111',
], false);
```

##### getServices() : <span style="color:#999;">Promise\<[Service](#service-object)[]></span>

Get current services.

- For Android, this method has no effect and will return empty array.

```js
const currentServices = await BluetoothSerial.getServices();
```

##### restoreServices() : <span style="color:#999;">Promise\<[Service](#service-object)[]></span>

Restore services and set them to default services (`BluetoothSerial.DEFAULT_SERVICES`.)

- For Android, this method has no effect and will return empty array.

```js
const services = await BluetoothSerial.restoreServices();
```

### Multiple devices connection

This module supports multiple devices connection, as you can see in [API Methods](#methods), most of the connection, IO, and buffer methods have `id` parameter that you can pass and specify which bluetooth device that you want to control.

However, to keep it clean and simple, you can use the following method to simplify them.

#### device( id? : <span style="color:#999;">String</span> ) : <span style="color:#999;">Object</span>

This method gives the ability to call group of API methods instead of pass `id` parameter at the end of each methods.

- id? : <span style="color:#999;">String</span>
  Optional device id or uuid.

The followings are group of methods that you can use with this method.

- `connect`
- `disconnect`
- `isConnected`
- `clear`
- `available`
- `withDelimiter`
- `read`
- `readOnce`
- `readEvery`
- `readUntilDelimiter`
- `readFromDevice`
- `write`
- `writeToDevice`

```js
const myDevice = BluetoothSerial.device(myId);
const yourDevice = BluetoothSerial.device(yourId);

await myDevice.connect();
await myDevice.write('This is a message for my device.');


let yourReadSubscription;

await yourDevice.connect();
await yourDevice.read((data, subscription) => {
    yourReadSubscription = subscription;

    console.log('Your data:', data);

    if (/** */) {
        BluetoothSerial.removeSubscription(subscription);
        yourReadSubscription = null;
    }
});

await myDevice.disconnect();

if (yourReadSubscription) {
    BluetoothSerial.removeSubscription(yourReadSubscription);
}

await yourDevice.disconnect();
```

### Events

#### Types

- `bluetoothEnabled` : When bluetooth adapter is turned on.
- `bluetoothDisabled` : When bluetooth adapter is turned off.
- `connectionSuccess` : When device is connected. You get object of message and [device](#device-object).

  ```js
  {
      message: ...,
      device: {
          ...
      }
  }
  ```

- `connectionFailed` : When you failed to connect to the device. You get object of message and [device](#device-object).

  ```js
  {
      message: ...,
      device: {
          ...
      }
  }
  ```

- `connectionLost` : When the device connection is lost. You get object of message and [device](#device-object).

  ```js
  {
      message: ...,
      device: {
          ...
      }
  }
  ```

- `read` or `data` : String of data from device. You get object of device id and data.

  ```js
  {
      id: ...,
      data: ...
  }
  ```

- `error` : Error message from native code.

  ```js
  {
      message: ...
  }
  ```

#### Methods

##### once( eventName : <span style="color:#999;">String</span>, handler : <span style="color:#999;">Function</span> ) : <span style="color:#999;">EmitterSubscription</span>

##### on( eventName : <span style="color:#999;">String</span>, handler : <span style="color:#999;">Function</span> ) : <span style="color:#999;">EmitterSubscription</span>

##### addListener( eventName : <span style="color:#999;">String</span>, handler : <span style="color:#999;">Function</span> ) : <span style="color:#999;">EmitterSubscription</span>

##### off( eventName : <span style="color:#999;">String</span>, handler : <span style="color:#999;">Function</span> ) : <span style="color:#999;">void</span>

##### removeListener( eventName : <span style="color:#999;">String</span>, handler : <span style="color:#999;">Function</span> ) : <span style="color:#999;">void</span>

##### removeAllListeners( eventName? : <span style="color:#999;">String</span> ) : <span style="color:#999;">void</span>

##### removeSubscription( subscription : <span style="color:#999;">EmitterSubscription</span>) : <span style="color:#999;">void</span>

## Todos

- Write base64 image.
