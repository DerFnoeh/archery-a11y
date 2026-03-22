package com.example.christophers.soundgeneration.SoundPipeline;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.util.Log;

import com.example.christophers.soundgeneration.Configuration.BluetoothDataHandler;
import com.example.christophers.soundgeneration.MainActivity;
import com.example.christophers.soundgeneration.R;
import com.example.christophers.soundgeneration.SoundPipeline.SoundHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Created by christopherS on 01.12.2016.
 */

public class BluetoothHandler {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Somehow a well known ID for the SPP-Protocol?!?!
    BluetoothAdapter btAdapter;
    BluetoothSocket btSocket = null;
    InputStream inStream = null;
    OutputStream outStream = null;
    Semaphore inStreamReady = new Semaphore(0);
    public boolean canControlSound = true;
    boolean forwardRawDataWithoutProcessing = false;
    BluetoothDataHandler dataHandler;

    SoundHandler sHandler;
    MainActivity main;
    BTThread readerAndConnectorThread;

    public int xOffset = 0;
    public int yOffset = 0;
    boolean calibrate = false; //the next Image will be used for calibration and is interpreted as the middle.


    public BluetoothHandler(SoundHandler sHandler, MainActivity main) {
        this.sHandler = sHandler;
        this.main = main;
        readerAndConnectorThread = new BTThread();
    }

    public BluetoothHandler() {
        readerAndConnectorThread = new BTThread();
    }

    public void setResources(SoundHandler sHandler, MainActivity main){
        this.sHandler = sHandler;
        this.main = main;
    }

    class BTThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            if (CheckBTState() == true) {   //if BT is turned on
                BluetoothDevice btDevice = chooseDevice();
                if (btDevice != null) {
                    toastMessage(main.getString(R.string.tryToConenct_toast));
                    if (openBluetoothStream(btDevice)) { //try 3 times to establish connection
                        processIncomingData();
                    }
                    if (!isInterrupted())
                        toastMessage(main.getString(R.string.connectionFailed_toast_1));
                    if (!isInterrupted() && openBluetoothStream(btDevice)) {
                        processIncomingData();
                    }
                    if (!isInterrupted())
                        toastMessage(main.getString(R.string.connectionFailed_toast_2));
                    if (!isInterrupted() && openBluetoothStream(btDevice)) {
                        processIncomingData();
                    }
                    if (!isInterrupted())
                        toastMessage(main.getString(R.string.connectionFailed_toast_3));
                }
            }
        }

        private void processIncomingData() {
            writeToMicrochip((byte)18,(byte)4);     //set output to "Only one picture diff max"
            sHandler.startSound();
            writeToMicrochip((byte)4);              //start picture taking
            int readBytes;
            int z;  //the biggest increase the camera found. Can be used to estimate the quality of the camera and lighting setup
            byte[] b = new byte[530]; //100 bytes => 33 pictures => 0.3 Seconds buffered
            while (!isInterrupted()) {
                try {
                    int bytesInStream = inStream.available();
                    if(bytesInStream < 3) bytesInStream=3;
                    if(forwardRawDataWithoutProcessing) bytesInStream=256;
                    if(bytesInStream>b.length) bytesInStream = b.length; //avoid overflow of b(uffer)
                    readBytes = inStream.read(b, 0, bytesInStream);
                    //logBluetooth(readBytes, bytesInStream);
                    if (forwardRawDataWithoutProcessing && dataHandler != null){
                        dataHandler.handleData(readBytes, b);
                    }
                    else interpretDataAsXandY(readBytes, b);
                } catch (IOException e) {
                    this.interrupt();
                    e.printStackTrace();
                    this.interrupt();
                }
            }
        }
    }

    private void interpretDataAsXandY(int readBytes, byte[] b) {
        int i = 0;
        int xTemp = 0;
        int yTemp = 0;
        int x = 0;
        int y = 0;
        int z = 0;
        if (readBytes > 5)
            i = readBytes - 5;  //make sure to only read the last 5 bytes maximum. This will automaticly contain a complete 3-byte package!
        while (i < readBytes) {    //read all the buffered bytes and then change the sound. Because only the last information ist still relevant!
            int message = b[i];
            i++;
            message = message & 0x000000FF; //erase the first 24 bits
            int prefix = ((message & 0x000000C0) >> 6);  //look at first 2 bits
            int payLoad = message & 0x0000003F;
            if (prefix < 2) {//PP==1X if its starts with 0: first 7 of y have arrived: 0x0yyyyyyy
                yTemp = (message & 0x0000007F) << 1;
            } else if (prefix == 2) {  //PP==10: first 6 bits of x have arrived: 0x10xxxxxx, get the last 6
                xTemp = payLoad << 2;
            } else if (prefix == 3) {  //PP==11: last 2 bits of x have and z have arrived: 0x11xxyzzz
                xTemp = xTemp + ((payLoad & 0x00000030) >> 4); //Pos 3,4 from message to 7,8 of x
                yTemp = yTemp + ((payLoad & 0x00000008) >> 3); //only pos 5 from message to the end of y
                z = message & 0x00000007;   //last 3 bits of the message
                x = xTemp;
                y = yTemp;
                milliseconds = System.currentTimeMillis();
                if (canControlSound && (readBytes - i) < 3) //readbytes-i<3 is for preventing a double update in packet which has 2 packages with prefix 3
                    adjustSound(x, y, z); //if a slider is currently moved this class has no access to Sound-Control in order to prevent overwriting the user input. readBytes-i is the number of bytes that will follow. Meaning that 3 following bytes will have another "Packet 3" and so the current one is not the 'freshest'
            }
        }
    }

    long milliseconds = 0;
    private void logBluetooth(int readBytes, int bytesInStream) {
        Log.d("Incoming", "Read Bytes: "+readBytes+" InStream: "+bytesInStream+" Time: "+ (System.currentTimeMillis()-milliseconds));
        milliseconds = System.currentTimeMillis();
    }


    int consecutiveNotFound = 0;
    int previousZ = 0;
    int i = 0;

    private void adjustSound(int x, int y, int z) {
        i++;
        if ((i % 10 == 0) && z != previousZ) {
            main.setZProgressBar(z);
            previousZ = z;
        }
        if (x != 0 && y != 0) {   //values when a point couldn't be found
            sHandler.changeVol(100);
            sHandler.changeX(x + xOffset);    //x+xOffset is for calibration aka trimming
            sHandler.changeY(y + yOffset);
            consecutiveNotFound = 0;
            if (calibrate) {
                xOffset = 127 - x;
                yOffset = 127 - y;
                calibrate = false;
                toastMessage(main.getString(R.string.set_Middle_Options_Menu_Toast));
                main.updateTrimmingText();
            }
        } else {
            consecutiveNotFound++;
            if (consecutiveNotFound > 5) {
                sHandler.changeVol(0);
                if(calibrate){
                    calibrate = false;
                    toastMessage(main.getString(R.string.trimmingFailed_noLight_toast));
                }
            }
        }
    }

    private boolean openBluetoothStream(BluetoothDevice btDevice) {
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        btAdapter.cancelDiscovery();
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
            e.printStackTrace();
            return false;
        }
        try {
            inStream = btSocket.getInputStream();
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            toastMessage(main.getString(R.string.connectionFailedGettingStreams_toast) + e.getMessage());
            e.printStackTrace();
            return false;
        }
        toastMessage(main.getString(R.string.connectionEstablished_toast));
        inStreamReady.release();
        return true;
    }

    private BluetoothDevice chooseDevice() {
        Set<BluetoothDevice> btBondedDevices = btAdapter.getBondedDevices();
        return main.chooseFromDevicesDialog(btBondedDevices);
    }

    public void writeToMicrochip(byte message0, byte message1) {
        byte[] b = {message0, message1};
        try {
            outStream.write(b);
            outStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToMicrochip(byte message) {
        writeToMicrochip(message, (byte)0);
    }

    private boolean CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            toastMessage(main.getString(R.string.noBluetoothOnPhone_toast));
            return false;
        } else {
            if (btAdapter.isEnabled()) {
                return true;
            } else {
                toastMessage(main.getString(R.string.turnOnBT_toast));
                return false;
            }
        }
    }

    public void connectAndRead() {
        if (readerAndConnectorThread.isAlive())
            toastMessage(main.getString(R.string.allreadyConnectingBT_toast));
        else {
            readerAndConnectorThread = new BTThread();
            readerAndConnectorThread.setPriority(Thread.MAX_PRIORITY);
            readerAndConnectorThread.start();
        }
    }

    public boolean isActive() {
        return readerAndConnectorThread.isAlive();
    }

    public void closeBT(Boolean fromUserInput) {
        if (readerAndConnectorThread.isAlive()) {
            try {
                readerAndConnectorThread.interrupt();
                if (inStream != null) inStream.close();
                if (btSocket != null) btSocket.close();
                if(fromUserInput)toastMessage(main.getString(R.string.connectionClosed_toast));
            } catch (Exception e) {
                toastMessage(main.getString(R.string.closingBTnotPossible_toast) + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if(fromUserInput)toastMessage(main.getString(R.string.noConnectionToClose_toast));
        }
    }


    public boolean recalibrate() {
        if (readerAndConnectorThread.isAlive()) {
            calibrate = true;
            return true;
        } else{
            toastMessage(main.getString(R.string.no_Data_receiving_toast));
            return false;
        }
    }

    private void toastMessage(String text) {
        main.toastMessage(text);
    }

    public void forwardIncomingDataTo(BluetoothDataHandler handler) {
        forwardRawDataWithoutProcessing = true;
        dataHandler = handler;
    }

    public void unforwardData() {
        writeToMicrochip((byte)18,(byte)4);
        forwardRawDataWithoutProcessing = false;
        dataHandler = null;
    }
}
