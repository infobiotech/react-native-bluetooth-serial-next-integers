package com.nuttawutmalee.RCTBluetoothSerial;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer; // add
import java.nio.ByteOrder; // add
import java.util.Arrays; // to print arrays
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import static com.nuttawutmalee.RCTBluetoothSerial.RCTBluetoothSerialPackage.TAG;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 *
 * This code was based on the Android SDK BluetoothChat Sample
 * $ANDROID_SDK/samples/android-17/BluetoothChat
 */
class RCTBluetoothSerialService {
    // Debugging
    private static final boolean D = true;

    // UUIDs
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private BluetoothAdapter mAdapter;
    private RCTBluetoothSerialModule mModule;

    private String mFirstDeviceAddress = null;
    private HashMap<String, ConnectThread> mConnectThreads;
    private HashMap<String, ConnectedThread> mConnectedThreads;
    private HashMap<String, String> mStates;

    // Constants that indicate the current connection state
    private static final String STATE_NONE = "none"; // we're doing nothing
    private static final String STATE_CONNECTING = "connecting"; // now initiating an outgoing connection
    private static final String STATE_CONNECTED = "connected"; // now connected to a remote device

    /**
     * Constructor. Prepares a new RCTBluetoothSerialModule session.
     *
     * @param module Module which handles service events
     */
    RCTBluetoothSerialService(RCTBluetoothSerialModule module) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mModule = module;

        if (mConnectThreads == null) {
            mConnectThreads = new HashMap<>();
        }

        if (mConnectedThreads == null) {
            mConnectedThreads = new HashMap<>();
        }

        if (mStates == null) {
            mStates = new HashMap<>();
        }
    }

    public String getFirstDeviceAddress() {
        return mFirstDeviceAddress;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        String id = device.getAddress();

        cancelConnectThread(id); // Cancel any thread attempting to make a connection
        cancelConnectedThread(id); // Cancel any thread currently running a connection

        // Start the thread to connect with the given device
        ConnectThread thread = new ConnectThread(device);
        thread.start();

        if (mConnectedThreads.isEmpty()) {
            mFirstDeviceAddress = id;
        }

        mConnectThreads.put(id, thread);
        mStates.put(id, STATE_CONNECTING);
    }

    /**
     * Check whether service is connected to device
     *
     * @param id Device address
     * @return Is connected to device
     */
    boolean isConnected(String id) {
        if (D) Log.d(TAG, "*** Service.isConnected *** " + id);
        return mStates.containsKey(id) && getState(id).equals(STATE_CONNECTED);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param id  Device address
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    void write(String id, byte[] out) {
        if (D)
            Log.d(TAG, "Write in service of device id " + id + ", state is " + STATE_CONNECTED);
        ConnectedThread r = null; // Create temporary object

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (!isConnected(id)) {
                return;
            }

            if (mConnectedThreads.containsKey(id)) {
                r = mConnectedThreads.get(id);
            }
        }

        if (r != null) {
            r.write(out); // Perform the write unsynchronized
        } else {
            Log.e(TAG, "Unable to write, connected thread is null");
            mModule.onError(new Exception(("Unable to write, connected thread is null")));
        }
    }

    /**
     * Stop threads of a specific device
     *
     * @param id Device address
     */
    synchronized void stop(String id) {
        if (D) Log.d(TAG, "Stop device id " + id);

        cancelConnectThread(id);
        cancelConnectedThread(id);

        mStates.put(id, STATE_NONE);

        if (id == mFirstDeviceAddress) {
            mFirstDeviceAddress = null;
        }
    }

    /**
     * Stop all threads of all devices
     */
    synchronized void stopAll() {
        if (D) Log.d(TAG, "Stop all devices");

        for (Map.Entry<String, ConnectThread> item : mConnectThreads.entrySet()) {
            ConnectThread thread = mConnectThreads.get(item.getKey());

            if (thread != null) {
                thread.cancel();
            }
        }

        mConnectThreads.clear();

        for (Map.Entry<String, ConnectedThread> item : mConnectedThreads.entrySet()) {
            ConnectedThread thread = mConnectedThreads.get(item.getKey());

            if (thread != null) {
                thread.cancel();
            }
        }

        mConnectedThreads.clear();

        for (Map.Entry<String, String> item : mStates.entrySet()) {
            mStates.put(item.getKey(), STATE_NONE);
        }

        mFirstDeviceAddress = null;
    }

    /**
     * Return the current connection state.
     *
     * @param id Device address
     */
    private synchronized String getState(String id) {
        return mStates.get(id);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connectionSuccess(BluetoothSocket socket, BluetoothDevice device) {
        String id = device.getAddress();

        if (D) Log.d(TAG, "Connected to device id " + id);

        cancelConnectThread(id); // Cancel any thread attempting to make a connection
        cancelConnectedThread(id); // Cancel any thread currently running a connection

        // Start the thread to manage the connection and perform transmissions
        ConnectedThread thread = new ConnectedThread(socket, device);
        thread.start();

        mConnectedThreads.put(id, thread);
        mModule.onConnectionSuccess("Connected to " + device.getName(), device);

        if (mStates.containsKey(id)) {
            String oldState = mStates.get(id);
            if (D) Log.d(TAG, "Device id " + id + " setState() " + oldState + " -> " + STATE_CONNECTED);
            mStates.put(id, STATE_CONNECTED);
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     *
     * @param device The BluetoothDevice that has been failed to connect
     */
    private void connectionFailed(BluetoothDevice device) {
        mModule.onConnectionFailed("Unable to connect to device", device); // Send a failure message with device
        RCTBluetoothSerialService.this.stop(device.getAddress()); // Start the service over to restart listening mode
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     *
     * @param device The BluetoothDevice that has been lost
     */
    private void connectionLost(BluetoothDevice device) {
        mModule.onConnectionLost("Device connection was lost", device); // Send a failure message
        RCTBluetoothSerialService.this.stop(device.getAddress()); // Start the service over to restart listening mode
    }

    /**
     * Cancel connect thread
     *
     * @param id Device address
     */
    private void cancelConnectThread(String id) {
        if (mConnectThreads.containsKey(id)) {
            ConnectThread thread = mConnectThreads.get(id);

            if (thread != null) {
                thread.cancel();
                mConnectThreads.remove(id);
            }
        }
    }

    /**
     * Cancel connected thread
     *
     * @param id Device address
     */
    private void cancelConnectedThread(String id) {
        if (mConnectedThreads.containsKey(id)) {
            ConnectedThread thread = mConnectedThreads.get(id);

            if (thread != null) {
                thread.cancel();
                mConnectedThreads.remove(id);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            if (D) Log.d(TAG, "Create ConnectThread");

            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID_SPP);
            } catch (Exception e) {
                mModule.onError(e);
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Begin mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection
                // or an exception
                if (D) Log.d(TAG, "Connecting to socket...");
                mmSocket.connect();
                if (D) Log.d(TAG, "Connected");
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                mModule.onError(e);

                // Some 4.1 devices have problems, try an alternative way to connect
                // See https://github.com/don/RCTBluetoothSerialModule/issues/89
                try {
                    Log.i(TAG, "Trying fallback...");
                    mmSocket = (BluetoothSocket) mmDevice.getClass()
                            .getMethod("createRfcommSocket", new Class[] { int.class }).invoke(mmDevice, 1);
                    mmSocket.connect();
                    Log.i(TAG, "Connected");
                } catch (Exception e2) {
                    Log.e(TAG, e.toString());
                    mModule.onError(e);

                    // Fallback to insecure socket
                    try {
                        Log.i(TAG, "Trying fallback to insecure socket...");
                        mmSocket = createInsecureBluetoothSocket(mmDevice);
                        mmSocket.connect();
                    } catch (Exception e3) {
                        Log.e(TAG, "Couldn't establish a Bluetooth connection.");
                        mModule.onError(e3);
                        try {
                            mmSocket.close();
                        } catch (Exception e4) {
                            Log.e(TAG, "unable to close() socket during connection failure", e3);
                            mModule.onError(e4);
                        }
                        connectionFailed(mmDevice);
                        return;
                    }
                }
            }

            // Reset the ConnectThread because we're done
            synchronized (RCTBluetoothSerialService.this) {
                mConnectThreads.remove(mmDevice.getAddress());
            }

            connectionSuccess(mmSocket, mmDevice); // Start the connected thread
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
                mModule.onError(e);
            }
        }

        private BluetoothSocket createInsecureBluetoothSocket(BluetoothDevice device) throws IOException {
            if (Build.VERSION.SDK_INT >= 10) {
                try {
                    final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord",
                            new Class[] { UUID.class });
                    return (BluetoothSocket) m.invoke(device, UUID_SPP);
                } catch (Exception e) {
                    Log.e(TAG, "Could not create Insecure RFComm Connection", e);
                }
            }
            return device.createRfcommSocketToServiceRecord(UUID_SPP);
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            if (D) Log.d(TAG, "Create ConnectedThread");
            mmSocket = socket;
            mmDevice = device;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                Log.e(TAG, "temp sockets not created", e);
                mModule.onError(e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "Begin mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            String id = mmDevice.getAddress();
            // Keep listening to the InputStream while connected
            while (true) {
                buffer = new byte[1024]; // free and reset
                try {
                    bytes = mmInStream.read(buffer); // Read from the InputStream
                    /*
                     *
                     */
// Log.i(TAG, "*** Mac " + id + " *** Bytes " + bytes + " *** " + Arrays.toString(buffer));
                    // int[] intArray = { 7, 9, 5, 1, 3 };
// System.out.println();
                     //Infobiotech Bluetooth protocol by GIOVANNI
             int packLength = -1;
             try{
                 byte[] packLengthBytes = {buffer[0], buffer[1]};
                 packLength = (int) ByteBuffer.wrap(packLengthBytes).order(ByteOrder.BIG_ENDIAN).getShort();
             }catch(Exception e){
                   Log.e(TAG, "Error parsing packet length in header: " + e.getMessage(), e);
                   mModule.onError(new Exception("Device "+id+". Error parsing packet length in header: " + e.getMessage()));
             }
             /**
              * 
              */
             if(bytes < packLength){
                 Log.e(TAG, "data length mismatch bytes = " + bytes + " less than packLength = " + packLength);
                 mModule.onError(new Exception("Device "+id+". Data length mismatch bytes = " + bytes + " less than packLength = " + packLength));
                 // byte[] realBuffer1 = new byte[1];
                 // mModule.onData(id, realBuffer1);
             }
             /*
             else if(bytes > packLength){
                 Log.e(TAG, "data length mismatch bytes = " + bytes + "  packLength = " + packLength);
                 mModule.onError(new Exception("Device "+id+". Data length mismatch bytes = " + bytes + " >  packLength = " + packLength));
                 // byte[] realBuffer1 = new byte[1];
                 // mModule.onData(id, realBuffer1);
             }
             */
             else { // >= case
                 /*

                  */
                  int shift = 0;
                    do {

                        byte[] realBuffer = new byte[packLength];
                        for (int index = 0; index < packLength; index++) {
                        realBuffer[index] = buffer[index+shift];
                        }
                        /*

                            */
                            //ArrayList<byte[]> messageArrayListItem = new ArrayList<>();
                            //messageArrayListItem.add(realBuffer);
                        // String data = new String(buffer, 0, bytes, "ISO-8859-1");
                        mModule.onData(id, realBuffer/* ALE data */); // Send the new data String to the UI Activity

            shift += packLength;
            packLength = 0;

             try{

                 byte[] packLengthBytes = {buffer[shift], buffer[shift+1]};
                 packLength = (int) ByteBuffer.wrap(packLengthBytes).order(ByteOrder.BIG_ENDIAN).getShort();

             }catch(Exception e){

                   Log.e(TAG, "Error parsing leftover packet length in header: " + e.getMessage(), e);
                   mModule.onError(new Exception("Device "+id+". Error parsing leftover packet length in header: " + e.getMessage()));
                   
             }



                    }while(packLength > 0);
                 /*

                  */

                  /* 

                  */
             }
/*
             int cmd=-1;
             try{
                 byte[] cmdBytes = {buffer[3], buffer[4]};
                 cmd = (int) ByteBuffer.wrap(cmdBytes).order(ByteOrder.BIG_ENDIAN).getShort();
             }catch(Exception e){
                 GR_system.globalLogger.exception(231, "BluetoothConnectThread", "Exception parsing command code: "+e.getMessage(), e);
             }
*/





                    /*

                     *
                    if (bytes != messageLength) {
                      Log.e(TAG, "data length mismatch");
                      mModule.onError(new Exception("data length mismatch bytes = " + bytes + "  messageLength = " + messageLength));
                    }
                    else {
                    }
/*
 *
 */
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    mModule.onError(e);
                    connectionLost(mmDevice);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                String str = new String(buffer, "UTF-8");
                if (D) Log.d(TAG, "Write in thread " + str);
                mmOutStream.write(buffer);
            } catch (Exception e) {
                Log.e(TAG, "Exception during write", e);
                mModule.onError(e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
                mModule.onError(e);
            }
        }
    }
}
