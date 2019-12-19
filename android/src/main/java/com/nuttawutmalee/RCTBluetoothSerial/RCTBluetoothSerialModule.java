package com.nuttawutmalee.RCTBluetoothSerial;

import java.lang.reflect.Method;
import java.util.Arrays; // to compare arrays
import java.util.ArrayList; // to use ArrayList
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.util.Base64;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import static com.nuttawutmalee.RCTBluetoothSerial.RCTBluetoothSerialPackage.TAG;

@SuppressWarnings("unused")
public class RCTBluetoothSerialModule extends ReactContextBaseJavaModule
        implements ActivityEventListener, LifecycleEventListener {

    // Debugging
    private static final boolean D = true;

    // Constants
    private static final String DEFAULT_SERVICES = "DEFAULT_SERVICES";

    // Event names
    private static final String BT_ENABLED = "bluetoothEnabled";
    private static final String BT_DISABLED = "bluetoothDisabled";
    private static final String CONN_SUCCESS = "connectionSuccess";
    private static final String CONN_FAILED = "connectionFailed";
    private static final String CONN_LOST = "connectionLost";
    private static final String DEVICE_READ = "read";
    private static final String DATA_READ = "data";
    private static final String ERROR = "error";

    // Other stuff
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_PAIR_DEVICE = 2;
    private static final String FIRST_DEVICE = "firstDevice";

    // Members
    private BluetoothAdapter mBluetoothAdapter;
    private RCTBluetoothSerialService mBluetoothService;
    private ReactApplicationContext mReactContext;

    // Promises
    private Promise mEnabledPromise;
    private Promise mDeviceDiscoveryPromise;
    private Promise mPairDevicePromise;
    private HashMap<String, Promise> mConnectedPromises;

/*
 *
 */
    private HashMap<String, /*byte[]*/ArrayList<byte[]>> mBuffers; // changed to byte[] from StringBuffer
    /*
     *
     */
    private HashMap<String, String> mDelimiters;

    public RCTBluetoothSerialModule(ReactApplicationContext reactContext) {
        super(reactContext);

        if (D) Log.d(TAG, "Bluetooth module started");

        mReactContext = reactContext;

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (mBluetoothService == null) {
            mBluetoothService = new RCTBluetoothSerialService(this);
        }

        if (mConnectedPromises == null) {
            mConnectedPromises = new HashMap<>();
        }

        if (mBuffers == null) {
            mBuffers = new HashMap<>();
        }

        if (mDelimiters == null) {
            mDelimiters = new HashMap<>();
        }

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            sendEvent(BT_ENABLED, null);
        } else {
            sendEvent(BT_DISABLED, null);
        }

        mReactContext.addActivityEventListener(this);
        mReactContext.addLifecycleEventListener(this);
        registerBluetoothStateReceiver();
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DEFAULT_SERVICES, Arguments.createArray());
        return constants;
    }

    @Override
    public String getName() {
        return "RCTBluetoothSerial";
    }

    // @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "On activity result request: " + requestCode + ", result: " + resultCode);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (D) Log.d(TAG, "User enabled Bluetooth");
                if (mEnabledPromise != null) {
                    mEnabledPromise.resolve(true);
                    mEnabledPromise = null;
                }
            } else {
                if (D) Log.d(TAG, "User did not enable Bluetooth");
                if (mEnabledPromise != null) {
                    mEnabledPromise.reject(new Exception("User did not enable Bluetooth"));
                    mEnabledPromise = null;
                }
            }
        }

        if (requestCode == REQUEST_PAIR_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                if (D) Log.d(TAG, "Pairing ok");
            } else {
                if (D) Log.d(TAG, "Pairing failed");
            }
        }
    }

    // @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "On activity result request: " + requestCode + ", result: " + resultCode);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (D) Log.d(TAG, "User enabled Bluetooth");
                if (mEnabledPromise != null) {
                    mEnabledPromise.resolve(true);
                    mEnabledPromise = null;
                }
            } else {
                if (D) Log.d(TAG, "User did not enable Bluetooth");
                if (mEnabledPromise != null) {
                    mEnabledPromise.reject(new Exception("User did not enable Bluetooth"));
                    mEnabledPromise = null;
                }
            }
        }

        if (requestCode == REQUEST_PAIR_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                if (D) Log.d(TAG, "Pairing ok");
            } else {
                if (D) Log.d(TAG, "Pairing failed");
            }
        }
    }

    // @Override
    public void onNewIntent(Intent intent) {
        if (D) Log.d(TAG, "On new intent");
    }

    @Override
    public void onHostResume() {
        if (D) Log.d(TAG, "Host resume");
    }

    @Override
    public void onHostPause() {
        if (D) Log.d(TAG, "Host pause");
    }

    @Override
    public void onHostDestroy() {
        if (D) Log.d(TAG, "Host destroy");
        mBluetoothService.stopAll();
    }

    @Override
    public void onCatalystInstanceDestroy() {
        if (D) Log.d(TAG, "Catalyst instance destroyed");
        super.onCatalystInstanceDestroy();
        mBluetoothService.stopAll();
    }

    @ReactMethod
    public void requestEnable(Promise promise) {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                // If bluetooth is already enabled resolve promise immediately
                promise.resolve(true);
            } else {
                // Start new intent if bluetooth is note enabled
                Activity activity = getCurrentActivity();
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                if (activity != null) {
                    mEnabledPromise = promise;
                    activity.startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
                } else {
                    Exception e = new Exception("Cannot start activity");
                    Log.e(TAG, "Cannot start activity", e);
                    promise.reject(e);
                    onError(e);
                }
            }
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void enable(Promise promise) {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                if (D) Log.d(TAG, "Bluetooth enabled");
                promise.resolve(true);
            } else {
                try {
                    mBluetoothAdapter.enable();
                    if (D) Log.d(TAG, "Bluetooth enabled");
                    promise.resolve(true);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot enable bluetooth");
                    promise.reject(e);
                    onError(e);
                }
            }
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void disable(Promise promise) {
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (D) Log.d(TAG, "Bluetooth disabled");
                promise.resolve(true);
            } else {
                try {
                    mBluetoothAdapter.disable();
                    if (D) Log.d(TAG, "Bluetooth disabled");
                    promise.resolve(true);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot disable bluetooth");
                    promise.reject(e);
                    onError(e);
                }
            }
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void isEnabled(Promise promise) {
        if (mBluetoothAdapter != null) {
            promise.resolve(mBluetoothAdapter.isEnabled());
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void list(Promise promise) {
        if (D) Log.d(TAG, "List paired called");

        if (mBluetoothAdapter != null) {
            WritableArray deviceList = Arguments.createArray();
            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();

            for (BluetoothDevice rawDevice : bondedDevices) {
                WritableMap device = deviceToWritableMap(rawDevice);
                deviceList.pushMap(device);
            }

            promise.resolve(deviceList);
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void listUnpaired(Promise promise) {
        if (D) Log.d(TAG, "Discover unpaired called");

        if (mBluetoothAdapter != null) {
            mDeviceDiscoveryPromise = promise;
            registerBluetoothDeviceDiscoveryReceiver();
            mBluetoothAdapter.startDiscovery();
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void cancelDiscovery(Promise promise) {
        if (D) Log.d(TAG, "Cancel discovery called");

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            promise.resolve(true);
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void pairDevice(String id, Promise promise) {
        if (D) Log.d(TAG, "Pair device: " + id);

        if (mBluetoothAdapter != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(id);

            if (device != null) {
                mPairDevicePromise = promise;
                pairDevice(device);
            } else {
                promise.reject(new Exception("Could not pair device " + id));
            }
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void unpairDevice(String id, Promise promise) {
        if (D) Log.d(TAG, "Unpair device: " + id);

        if (mBluetoothAdapter != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(id);

            if (device != null) {
                mPairDevicePromise = promise;
                unpairDevice(device);
            } else {
                promise.reject(new Exception("Could not unpair device " + id));
            }
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void connect(String id, Promise promise) {
        if (D) Log.d(TAG, "connect " + id);

        if (mBluetoothAdapter != null) {
            BluetoothDevice rawDevice = mBluetoothAdapter.getRemoteDevice(id);

            if (rawDevice != null) {
                mConnectedPromises.put(id, promise);
                mBluetoothService.connect(rawDevice);
            } else {
                mConnectedPromises.put(FIRST_DEVICE, promise);
                registerFirstAvailableBluetoothDeviceDiscoveryReceiver();
            }
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void disconnect(@Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }

        if (D) Log.d(TAG, "Disconnect from device id " + id);

        if (id != null) {
            mBluetoothService.stop(id);
        }

        promise.resolve(true);
    }

    @ReactMethod
    public void disconnectAll(Promise promise) {
        mBluetoothService.stopAll();
        promise.resolve(true);
    }

    @ReactMethod
    public void isConnected(@Nullable String id, Promise promise) {

        if (D) Log.d(TAG, "*** isConnected ***");

        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
            if (D) Log.d(TAG, "*** isConnected *** id was null, now is: " + id);
        }

        if (id == null) {
            if (D) Log.d(TAG, "*** isConnected *** id is again null, no connected device");
            promise.resolve(false);
        } else {
            if (D) Log.d(TAG, "*** isConnected *** resolving connection status for: " + id);
            promise.resolve(mBluetoothService.isConnected(id));
        }
    }

    @ReactMethod
    public void writeToDevice(String message, @Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }

        if (D) Log.d(TAG, "Write to device id " + id + " : " + message);

        if (id != null) {
            byte[] data = Base64.decode(message, Base64.DEFAULT);
            mBluetoothService.write(id, data);
        }

        promise.resolve(true);
    }

    @ReactMethod
    public void readFromDevice(@Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }
        if (D) Log.d(TAG, "Read from device id " + id);
            /*
             *
             */
        ArrayList<byte[]> data = new ArrayList<>();
            /* String data = ""; */
        /*
         *
         */
        if (mBuffers.containsKey(id)) {

          ArrayList<byte[]> buffer = new ArrayList<>();
            buffer = mBuffers.get(id);
            /*
             * @todo split/clear the buffer
             */
            data = (ArrayList) buffer.clone();
            // ALE int length = buffer.length();
            // ALE data = buffer.substring(0, length);
            // ALE buffer.delete(0, length);
            buffer.removeAll(buffer);
            mBuffers.put(id, buffer);
            /*
             *
             */
        }
/*
 *
 */
        promise.resolve(data);
        /*
         *
         */
    }

    @ReactMethod
    public void readUntilDelimiter(String delimiter, @Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }

        promise.resolve(readUntil(id));
    }

    @ReactMethod
    public void withDelimiter(String delimiter, @Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }

        if (D) Log.d(TAG, "Set delimiter of device id " + id + " to " + delimiter);

        if (id != null) {
            mDelimiters.put(id, delimiter);
        }

        promise.resolve(id);
    }

    @ReactMethod
    public void clear(@Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }

        if (mBuffers.containsKey(id)) {
          /*
           *
           */
            // StringBuffer buffer = mBuffers.get(id);
            // buffer.setLength(0);
           ArrayList<byte[]> buffer = new ArrayList<>(); // set length to 1 as if it would 'buffer.setLength(0);''
        /*
         *
         */
            mBuffers.put(id, buffer);
        }

        promise.resolve(true);
    }

    @ReactMethod
    public void available(@Nullable String id, Promise promise) {
        if (id == null) {
            id = mBluetoothService.getFirstDeviceAddress();
        }

        int length = 0;
        /*
         *
         */
        if (mBuffers.containsKey(id)) {
          /*
           *
           */
          // byte[] emptyBuffer = new byte[1024]; // ADD
          // byte[] buffer = new byte[1024]; // ADD
          /* ALE StringBuffer */ ArrayList<byte[]> buffer = mBuffers.get(id);
          // ALE length = buffer.length();
          length = buffer.size(); // ADD
          /*
           *
           */
        }

/*
 *
 */
        promise.resolve(length);
        /*
         *
         */
    }

    @ReactMethod
    public void setAdapterName(String newName, Promise promise) {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.setName(newName);
            promise.resolve(mBluetoothAdapter.getName());
        } else {
            rejectNullBluetoothAdapter(promise);
        }
    }

    @ReactMethod
    public void setServices(ReadableArray services, Boolean includeDefaultServices, Promise promise) {
        WritableArray updated = Arguments.createArray();
        promise.resolve(updated);
    }

    @ReactMethod
    public void getServices(Promise promise) {
        WritableArray services = Arguments.createArray();
        promise.resolve(services);
    }

    @ReactMethod
    public void restoreServices(Promise promise) {
        WritableArray services = Arguments.createArray();
        promise.resolve(services);
    }

    /**
     * Handle connection success
     *
     * @param msg             Additional message
     * @param connectedDevice Connected device
     */
    void onConnectionSuccess(String msg, BluetoothDevice connectedDevice) {
        String id = connectedDevice.getAddress();

        if (!mDelimiters.containsKey(id)) {
            mDelimiters.put(id, "");
        }

        if (!mBuffers.containsKey(id)) {
          /*
           *
           */
          ArrayList<byte[]> buffer = new ArrayList<>(); // ADD
            mBuffers.put(id, buffer /* ALE StringBuffer() */);
            /*
             *
             */
        }

        if (mConnectedPromises.containsKey(id)) {
            Promise promise = mConnectedPromises.get(id);

            if (promise != null) {
                WritableMap deviceForPromise = deviceToWritableMap(connectedDevice);
                promise.resolve(deviceForPromise);
            }
        }

        WritableMap device = deviceToWritableMap(connectedDevice);
        WritableMap params = Arguments.createMap();
        params.putMap("device", device);
        params.putString("message", msg);
        sendEvent(CONN_SUCCESS, params);
    }

    /**
     * handle connection failure
     *
     * @param msg             Additional message
     * @param connectedDevice Connected device
     */
    void onConnectionFailed(String msg, BluetoothDevice connectedDevice) {
        WritableMap params = Arguments.createMap();
        WritableMap device = deviceToWritableMap(connectedDevice);

        params.putMap("device", device);
        params.putString("message", msg);

        String id = connectedDevice.getAddress();

        if (mConnectedPromises.containsKey(id)) {
            Promise promise = mConnectedPromises.get(id);

            if (promise != null) {
                promise.reject(new Exception(msg));
            }
        }

        sendEvent(CONN_FAILED, params);
    }

    /**
     * Handle lost connection
     *
     * @param msg             Message
     * @param connectedDevice Connected device
     */
    void onConnectionLost(String msg, BluetoothDevice connectedDevice) {
        WritableMap params = Arguments.createMap();
        WritableMap device = deviceToWritableMap(connectedDevice);

        params.putMap("device", device);
        params.putString("message", msg);
        mConnectedPromises.remove(connectedDevice.getAddress());

        sendEvent(CONN_LOST, params);
    }

    /**
     * Handle error
     *
     * @param e Exception
     */
    void onError(Exception e, String id) {
        WritableMap params = Arguments.createMap();
        params.putString("message", e.getMessage());
        params.putString("deviceId", id);
        sendEvent(ERROR, params);
    }

    // overloading
    void onError(Exception e) {
        onError(e, "");
    }

    /**
     * Handle read
     *
     * @param id   Device address
     * @param data Message
     */
    void onData(String id, byte[] data) { // fingerprint changed
        if (mBuffers.containsKey(id)) {
          /*
           * String[] both = ArrayUtils.addAll(buffer, data);
           */
          // byte[] buffer = new byte[1024]; // ADD
            /* ALE StringBuffer */ ArrayList<byte[]> buffer = mBuffers.get(id);
            // buffer.append(data);
            buffer.add(data);
            mBuffers.put(id, /* ALE buffer *//*data*//*mergedData*/buffer);
            /*
             *
             */
        }

/*
        String delimiter = "";

        if (mDelimiters.containsKey(id)) {
            delimiter = mDelimiters.get(id);
        }
*/

/*
 *
 */
        /* String */byte[] completeData = readUntil(id);





        if (completeData.length > 0 /* != null/* && completeData.length() > 0*/) {

                WritableArray completeDataWritableArray = Arguments.createArray();
                for (int index = 0; index < completeData.length; index++) {
                  completeDataWritableArray.pushInt(completeData[index]&0xFF);
                }

            WritableMap readParams = Arguments.createMap();
            readParams.putString("id", id);
            readParams.putArray("data", completeDataWritableArray);
            sendEvent(DEVICE_READ, readParams);

            /*
                  WritableArray completeDataWritableArray2 = Arguments.createArray();
                  for (int index = 0; index < completeData.length; index++) {
                    completeDataWritableArray2.pushInt(completeData[index]&0xFF);
                  }

            WritableMap dataParams = Arguments.createMap();
            dataParams.putString("id", id);
            dataParams.putArray("data", completeDataWritableArray2);
            sendEvent(DATA_READ, dataParams);
            */
        }
    }
/*
 *
 */

    /**
     * Handle read until find a certain delimiter
     *
     * @param id        Device address
     * @return buffer data from device
     */
    private byte[] readUntil(String id) {
        // String data = "";
        if (mBuffers.containsKey(id)) {
        /*
         *
         */
        // byte[] buffer = new byte[length];
          /*
           *
           */
          /* ALE StringBuffer */ ArrayList<byte[]> buffer = mBuffers.get(id);

          byte[] data = buffer.remove(0);

          mBuffers.put(id, buffer);






          return data; // ALE
/*
            int index = 5; // buffer.indexOf(delimiter, 0);

            if (index > -1) {
                data = buffer.substring(0, index + delimiter.length());
                buffer.delete(0, index + delimiter.length());
                mBuffers.put(id, buffer);
            }
*/
/*
 *
 */
        }
        else {
          byte[] emptyBuffer = new byte[1];
          return emptyBuffer;
        }

    }

    /**
     * Check if is api level 19 or above
     *
     * @return is above api level 19
     */
    private boolean isKitKatOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Send event to javascript
     *
     * @param eventName Name of the event
     * @param params    Additional params
     */
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        if (mReactContext.hasActiveCatalystInstance()) {
            if (D) Log.d(TAG, "Sending event: " + eventName);
            mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
        }
    }

    /**
     * Convert BluetoothDevice into WritableMap
     *
     * @param device Bluetooth device
     */
    private WritableMap deviceToWritableMap(BluetoothDevice device) {
        if (D) Log.d(TAG, "device " + device.toString());

        WritableMap params = Arguments.createMap();

        if (device != null) {
            params.putString("name", device.getName());
            params.putString("address", device.getAddress());
            params.putString("id", device.getAddress());

            if (device.getBluetoothClass() != null) {
                params.putInt("class", device.getBluetoothClass().getDeviceClass());
            }
        }

        return params;
    }

    /**
     * Pair device before kitkat
     *
     * @param device Device
     */
    private void pairDevice(BluetoothDevice device) {
        try {
            if (D) Log.d(TAG, "Start Pairing...");
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            registerDevicePairingReceiver(device, BluetoothDevice.BOND_BONDED);
        } catch (Exception e) {
            Log.e(TAG, "Cannot pair device " + device.getAddress(), e);
            if (mPairDevicePromise != null) {
                mPairDevicePromise.reject(e);
                mPairDevicePromise = null;
            }
            onError(e);
        }
    }

    /**
     * Unpair device
     *
     * @param device Device
     */
    private void unpairDevice(BluetoothDevice device) {
        try {
            if (D) Log.d(TAG, "Start Unpairing...");
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            registerDevicePairingReceiver(device, BluetoothDevice.BOND_NONE);
        } catch (Exception e) {
            Log.e(TAG, "Cannot unpair device " + device.getAddress(), e);
            if (mPairDevicePromise != null) {
                mPairDevicePromise.reject(e);
                mPairDevicePromise = null;
            }
            onError(e);
        }
    }

    /**
     * Return reject promise for null bluetooth adapter
     *
     * @param promise
     */
    private void rejectNullBluetoothAdapter(Promise promise) {
        Exception e = new Exception("Bluetooth adapter not found");
        Log.e(TAG, "Bluetooth adapter not found");
        promise.reject(e);
        onError(e);
    }

    /**
     * Register receiver for device pairing
     *
     * @param rawDevice     Bluetooth device
     * @param requiredState State that we require
     */
    private void registerDevicePairingReceiver(final BluetoothDevice rawDevice, final int requiredState) {
        final WritableMap device = deviceToWritableMap(rawDevice);
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        final BroadcastReceiver devicePairingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                            BluetoothDevice.ERROR);

                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        if (D) Log.d(TAG, "Device paired");
                        if (mPairDevicePromise != null) {
                            mPairDevicePromise.resolve(device);
                            mPairDevicePromise = null;
                        }
                        try {
                            mReactContext.unregisterReceiver(this);
                        } catch (Exception e) {
                            Log.e(TAG, "Cannot unregister receiver", e);
                            onError(e);
                        }
                    } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                        if (D) Log.d(TAG, "Device unpaired");
                        if (mPairDevicePromise != null) {
                            mPairDevicePromise.resolve(device);
                            mPairDevicePromise = null;
                        }
                        try {
                            mReactContext.unregisterReceiver(this);
                        } catch (Exception e) {
                            Log.e(TAG, "Cannot unregister receiver", e);
                            onError(e);
                        }
                    }
                }
            }
        };

        mReactContext.registerReceiver(devicePairingReceiver, intentFilter);
    }

    /**
     * Register receiver for bluetooth device discovery
     */
    private void registerBluetoothDeviceDiscoveryReceiver() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        final BroadcastReceiver deviceDiscoveryReceiver = new BroadcastReceiver() {
            private WritableArray unpairedDevices = Arguments.createArray();

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (D) Log.d(TAG, "onReceive called");

                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    if (D) Log.d(TAG, "Discovery started");
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice rawDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (D) Log.d(TAG, "Discovery extra device (device id: " + rawDevice.getAddress() + ")");

                    WritableMap device = deviceToWritableMap(rawDevice);
                    unpairedDevices.pushMap(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    if (D) Log.d(TAG, "Discovery finished");

                    if (mDeviceDiscoveryPromise != null) {
                        mDeviceDiscoveryPromise.resolve(unpairedDevices);
                        mDeviceDiscoveryPromise = null;
                    }

                    try {
                        mReactContext.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to unregister receiver", e);
                        onError(e);
                    }
                }
            }
        };

        mReactContext.registerReceiver(deviceDiscoveryReceiver, intentFilter);
    }

    /**
     * Register receiver for first available device discovery
     */
    private void registerFirstAvailableBluetoothDeviceDiscoveryReceiver() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        final BroadcastReceiver deviceDiscoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (D) Log.d(TAG, "onReceive called");

                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    if (D) Log.d(TAG, "Discovery started");
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice rawDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String id = rawDevice.getAddress();

                    if (D) Log.d(TAG, "Discovery first available device (device id: " + id + ")");

                    mBluetoothService.connect(rawDevice);

                    if (mConnectedPromises.containsKey(FIRST_DEVICE)) {
                        Promise promise = mConnectedPromises.get(FIRST_DEVICE);
                        mConnectedPromises.remove(FIRST_DEVICE);
                        mConnectedPromises.put(id, promise);

                        if (promise != null) {
                            WritableMap device = deviceToWritableMap(rawDevice);
                            promise.resolve(device);
                        }
                    }

                    try {
                        mReactContext.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to unregister receiver", e);
                        onError(e);
                    }
                }
            }
        };

        mReactContext.registerReceiver(deviceDiscoveryReceiver, intentFilter);
    }

    /**
     * Register receiver for bluetooth state change
     */
    private void registerBluetoothStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if (D) Log.d(TAG, "Bluetooth was disabled");
                        sendEvent(BT_DISABLED, null);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (D) Log.d(TAG, "Bluetooth was enabled");
                        sendEvent(BT_ENABLED, null);
                        break;
                    default:
                        break;
                    }
                }
            }
        };

        mReactContext.registerReceiver(bluetoothStateReceiver, intentFilter);
    }
}
