const ReactNative = require("react-native");
const React = require("react");
const { Buffer } = require("buffer");

const { NativeModules, DeviceEventEmitter } = ReactNative;
const { BluetoothSerial } = NativeModules;

/**
 * High order component that will
 * attach native event emitter and
 * send it as a props named subscription.
 *
 * It will create an emitter when component did mount
 * and remove all listeners when component will unmount.
 *
 * @param  {Object}   [options]
 * @param  {String}   [options.subscriptionName=subscription]
 * @param  {Boolean}  [options.destroyOnWilUnmount=true]
 * @return {React.Component}
 */
export const withSubscription = (
  options = {
    subscriptionName: "subscription",
    destroyOnWilUnmount: true
  }
) => WrappedComponent => {
  const subscriptionName =
    typeof options.subscriptionName === "string" &&
    options.subscriptionName !== ""
      ? options.subscriptionName
      : "subscription";
  const destroyOnWilUnmount =
    typeof options.destroyOnWilUnmount === "boolean"
      ? options.destroyOnWilUnmount
      : true;

  const subscription = DeviceEventEmitter;
  subscription.on = DeviceEventEmitter.addListener;
  subscription.off = DeviceEventEmitter.removeListener;
  subscription.remove = DeviceEventEmitter.removeAllListeners;

  return class RTCBluetoothSerialComponent extends React.Component {
    componentWillUnmount() {
      if (destroyOnWilUnmount && subscription) {
        if (typeof subscription.remove === "function") {
          subscription.remove();
        }

        if (typeof subscription.removeAllListeners === "function") {
          subscription.removeAllListeners();
        }
      }
    }

    render() {
      return (
        <WrappedComponent
          {...this.props}
          {...{ [subscriptionName]: subscription }}
        >
          {this.props.children}
        </WrappedComponent>
      );
    }
  };
};

// Overloading
const {
  disconnect,
  isConnected,
  readFromDevice,
  readUntilDelimiter,
  writeToDevice,
  clear,
  available,
  withDelimiter,
  listUnpaired,
  cancelDiscovery,
  setServices
} = BluetoothSerial;

BluetoothSerial.disconnect = (id = null) => disconnect(id);
BluetoothSerial.isConnected = (id = null) => isConnected(id);
BluetoothSerial.readFromDevice = (id = null) => readFromDevice(id);
BluetoothSerial.readUntilDelimiter = (delimiter, id = null) =>
  readUntilDelimiter(delimiter, id);
BluetoothSerial.writeToDevice = (data, id = null) => writeToDevice(data, id);
BluetoothSerial.clear = (id = null) => clear(id);
BluetoothSerial.available = (id = null) => available(id);
BluetoothSerial.withDelimiter = (delimiter, id = null) =>
  withDelimiter(delimiter, id);
BluetoothSerial.setServices = (services, includeDefaultServices = true) =>
  setServices(services, includeDefaultServices);
BluetoothSerial.discoverUnpairedDevices = listUnpaired;
BluetoothSerial.stopScanning = cancelDiscovery;

/**
 * Select a specific bluetooth device and
 * give you the ability to read / write from
 * that device.
 *
 * @param {String} [id]
 * @return {Object}
 */
BluetoothSerial.device = (id = null) => ({
  /**
   * Connect to certain bluetooth device / peripheral.
   *
   * @return {Promise<Object>}
   *
   * @throws this will throws an error if android bluetooth adapter
   *         is missing.
   */
  connect: () => BluetoothSerial.connect(id),

  /**
   * Disconnect from the selected bluetooth device / peripheral.
   *
   * @return {Promise<Boolean>}
   *
   * @throws this will throws an error if android bluetooth adapter
   *         is missing.
   */
  disconnect: () => BluetoothSerial.disconnect(id),

  /**
   * Indicates if you are connected to the selected bluetooth device / peripheral or not.
   *
   * @return {Promise<Boolean>}
   */
  isConnected: () => BluetoothSerial.isConnected(id),

  /**
   * Clear all buffer data of the selected bluetooth device / peripheral.
   *
   * @return {Promise<Boolean>}
   */
  clear: () => BluetoothSerial.clear(id),

  /**
   * Get length of buffer data from the selected bluetooth device / peripheral.
   *
   * @return {Promise<Number>}
   */
  available: () => BluetoothSerial.available(id),

  /**
   * Set delimiter split the buffer data
   * when you are reading from the selected device.
   *
   * @param delimiter
   * @return {Promise<String>}
   */
  withDelimiter: delimiter => BluetoothSerial.withDelimiter(delimiter, id),

  /**
   * Listen and read data from the selected device.
   *
   * @param {Function} [callback=() => {}]
   * @param {String} [delimiter=""]
   */
  read: (callback = () => {}, delimiter = "") => {
    if (typeof callback !== "function") {
      return;
    }

    BluetoothSerial.withDelimiter(delimiter, id).then(deviceId => {
      const subscription = BluetoothSerial.addListener("read", result => {
        const { id: readDeviceId, data } = result;

        if (readDeviceId === deviceId) {
          callback(data, subscription);
        }
      });
    });
  },

  /**
   * Read data from the selected device once.
   *
   * @param  {String} [delimiter=""]
   * @return {Promise<String>}
   */
  readOnce: (delimiter = "") =>
    typeof delimiter === "string"
      ? BluetoothSerial.readUntilDelimiter(delimiter, id)
      : BluetoothSerial.readFromDevice(id),

  /**
   * Read data from the selected device every n ms.
   *
   * @param {Function} [callback=() => {}]
   * @param {Number} [ms=1000]
   * @param {String} [delimiter=""]
   */
  readEvery: (callback = () => {}, ms = 1000, delimiter = "") => {
    if (typeof callback !== "function") {
      return;
    }

    const intervalId = setInterval(async () => {
      const data =
        typeof delimiter === "string"
          ? await BluetoothSerial.readUntilDelimiter(delimiter, id)
          : await BluetoothSerial.readFromDevice(id);

      callback(data, intervalId);
    }, ms);
  },

  /**
   * Read all buffer data up to particular delimiter
   * from the selected device.
   *
   * @param delimiter
   * @return {Promise<String>}
   */
  readUntilDelimiter: delimiter =>
    BluetoothSerial.readUntilDelimiter(delimiter, id),

  /**
   * Read all buffer data from connected device.
   *
   * @return {Promise<String>}
   */
  readFromDevice: () => BluetoothSerial.readFromDevice(id),

  /**
   * Write data to the selected device, you can pass string or buffer,
   * We must convert to base64 in RN there is no way to pass buffer directly.
   *
   * @param  {Buffer|String} data
   * @return {Promise<Boolean>}
   */
  write: data => {
    /*
    if (typeof data === "string") {
      data = new Buffer(data);
    }
    const buffer = data.toString("base64");
    */
    return BluetoothSerial.writeToDevice(data, id);
  },

  /**
   * Write string to the selected device.
   *
   * @param {String} data
   * @return {Promise<Boolean>}
   */
  writeToDevice: data => BluetoothSerial.writeToDevice(data, id)
});

/**
 * Similar to addListener, except that the listener is removed after it is
 * invoked once.
 *
 * @param eventName - Name of the event to listen to
 * @param listener - Function to invoke only once when the
 *   specified event is emitted
 * @param context - Optional context object to use when invoking the
 *   listener
 */
BluetoothSerial.once = (eventName, handler, context) =>
  DeviceEventEmitter.once(eventName, handler, context);

/**
 * Attach listener to a certain event name.
 *
 * @param eventName - Name of the event to listen to
 * @param listener - Function to invoke only once when the
 *   specified event is emitted
 * @param context - Optional context object to use when invoking the
 *   listener
 */
BluetoothSerial.addListener = (eventName, handler, context) =>
  DeviceEventEmitter.addListener(eventName, handler, context);

/**
 * Attach listener to a certain event name.
 *
 * @param eventName - Name of the event to listen to
 * @param listener - Function to invoke only once when the
 *   specified event is emitted
 * @param context - Optional context object to use when invoking the
 *   listener
 */
BluetoothSerial.on = (eventName, handler, context) =>
  DeviceEventEmitter.addListener(eventName, handler, context);

/**
 * Removes the given listener for event of specific type.
 *
 * @param eventName - Name of the event to emit
 * @param listener - Function to invoke when the specified event is
 *   emitted
 *
 * @example
 *   emitter.removeListener('someEvent', function(message) {
 *     console.log(message);
 *   }); // removes the listener if already registered
 *
 */
BluetoothSerial.removeListener = (eventName, handler) =>
  DeviceEventEmitter.removeListener(eventName, handler);

/**
 * Removes the given listener for event of specific type.
 *
 * @param eventName - Name of the event to emit
 * @param listener - Function to invoke when the specified event is
 *   emitted
 *
 * @example
 *   emitter.removeListener('someEvent', function(message) {
 *     console.log(message);
 *   }); // removes the listener if already registered
 *
 */
BluetoothSerial.off = (eventName, handler) =>
  DeviceEventEmitter.removeListener(eventName, handler);

/**
 * Removes all of the registered listeners, including those registered as
 * listener maps.
 *
 * @param eventName - Optional name of the event whose registered
 *   listeners to remove
 */
BluetoothSerial.removeAllListeners = eventName =>
  DeviceEventEmitter.removeAllListeners(eventName);

/**
 * Removes a specific subscription. Called by the `remove()` method of the
 * subscription itself to ensure any necessary cleanup is performed.
 */
BluetoothSerial.removeSubscription = subscription =>
  DeviceEventEmitter.removeSubscription(subscription);

/**
 * Listen and read data from device.
 *
 * @param {Function} callback
 * @param {String} [delimiter=""]
 * @param {String} [id]
 */
BluetoothSerial.read = (callback, delimiter = "", id = null) => {
  if (typeof callback !== "function") {
    return;
  }

  BluetoothSerial.withDelimiter(delimiter, id).then(deviceId => {
    const subscription = BluetoothSerial.addListener("read", result => {
      const { id: readDeviceId, data } = result;

      if (readDeviceId === deviceId) {
        callback(data, subscription);
      }
    });
  });
};

/**
 * Read data from device once.
 *
 * @param  {String} [delimiter=""]
 * @param  {String} [id]
 * @return {Promise<String>}
 */
BluetoothSerial.readOnce = (delimiter = "", id = null) =>
  typeof delimiter === "string"
    ? BluetoothSerial.readUntilDelimiter(delimiter, id)
    : BluetoothSerial.readFromDevice(id);

/**
 * Read data from device every n ms.
 *
 * @param {Function} callback
 * @param {Number} [ms=1000]
 * @param {String} [delimiter=""]
 * @param {String} [id]
 */
BluetoothSerial.readEvery = (
  callback = () => {},
  ms = 1000,
  delimiter = "",
  id = null
) => {
  if (typeof callback !== "function") {
    return;
  }

  const intervalId = setInterval(async () => {
    const data =
      typeof delimiter === "string"
        ? await BluetoothSerial.readUntilDelimiter(delimiter, id)
        : await BluetoothSerial.readFromDevice(id);

    callback(data, intervalId);
  }, ms);
};

/**
 * Write data to device, you can pass string or buffer,
 * We must convert to base64 in RN there is no way to pass buffer directly.
 *
 * @param  {Buffer|String} data
 * @param  {String} [id]
 * @return {Promise<Boolean>}
 */
BluetoothSerial.write = (data, id = null) => {
  /*
  if (typeof data === "string") {
    data = new Buffer(data);
  }
  const buffer = data.toString("base64");
  */
  return BluetoothSerial.writeToDevice(data, id);
};

export default BluetoothSerial;
