package blah.com.anothertest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Kyle Dillon on 4/29/14.
 *
 * Bluetooth class that encapsulates all Bluetooth connection and communication functionality.
 */


public class Bluetooth {

    private String DEVICE_NAME = "DNA_Beta"; // Change this to match your bluetooth device name.
    private String DEVICE_UUID;

    private Context context;
    private MainActivity mainActivity;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private volatile boolean stopWorker;

    public Bluetooth(Context context, MainActivity mainActivity){
        // When this object is created, it needs the context of it's instantiating class, as well as
        // reference to the class itself
        this.context = context;
        this.mainActivity = mainActivity;
        stopWorker = false; // A flag to start/stop the background thread checking for incoming data
    }

    // Function that searches for the Bluetooth with DEVICE_NAME
    public int findBT()
    {
        // Retrieve a BluetoothAdapter object
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast toast = Toast.makeText(context, "No Bluetooth adapter available", Toast.LENGTH_LONG);
            toast.show();

            return -1;
        }

        // Make sure Bluetooth is enabled
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) context).startActivityForResult(enableBluetooth, 0);
        }

        // Loop through paired devices looking for DEVICE_NAME
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(DEVICE_NAME)) {
                    mmDevice = device;
                    Method method = null;
                    try {
                        // If and when device is found, retrieved it's UUID
                        method = mmDevice.getClass().getMethod("getUuids", null);
                        ParcelUuid[] deviceUuids = (ParcelUuid[]) method.invoke(mmDevice, null);
                        DEVICE_UUID = deviceUuids[0].getUuid().toString(); // Will only be one UUID.
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }
        }

        // If device was not found
        if(DEVICE_UUID == null){
            // Did not find device, user must pair with it, so open Bluetooth settings
            Intent intentBluetooth = new Intent();
            intentBluetooth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            context.startActivity(intentBluetooth);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(context, "Pair with " + DEVICE_NAME + " in Bluetooth Settings with key 1234. Then return to app and try again.", Toast.LENGTH_LONG);
                    toast.show();
                }
            }, 1000);

            return -1;
        }
        else { // Else device was found
            Toast toast = Toast.makeText(context, "Bluetooth device found", Toast.LENGTH_LONG);
            toast.show();
        }

        return 0;
    }

    // Method that opens Bluetooth connection with DEVICE_NAME
    public void openBT() throws IOException
    {
        // Create socket, connect to device, grab input and output data streams
        UUID uuid = UUID.fromString(DEVICE_UUID); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        Toast toast = Toast.makeText(context, "Bluetooth connection opened", Toast.LENGTH_LONG);
        toast.show();
    }

    public void beginListenForData()
    {
        Toast toast = Toast.makeText(context, "Listening for incoming Bluetooth data in background", Toast.LENGTH_LONG);
        toast.show();

        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character '\n'

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker) // While stopWorker is false
                {
                    try
                    {
                        // If there is no input data stream then bail
                        if(mmInputStream == null) {
                            stopWorker = true;
                            break;
                        }


                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0) // If there is data available to be read in
                        {
                            // Read in the data and loop through each byte
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter) // If the new line character is found
                                {
                                    // Grab the read data and pass it to the MainActivity
                                    final byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            mainActivity.receiveData(encodedBytes); // MAKE SURE MAINACTIVITY IMPLEMENTS THIS METHOD
                                        }
                                    });
                                }
                                else // While the delimeter (new line char) is not found, keep looping through bytes
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start(); // Start the background thread
    }

    // Send a byte of data over Bluetooth
    public void sendData(byte data) throws IOException
    {
        mmOutputStream.write(data);
    }

    // Close the Bluetooth connection and clean up
    public void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast toast = Toast.makeText(context, "Bluetooth closed", Toast.LENGTH_LONG);
        toast.show();
    }

}
