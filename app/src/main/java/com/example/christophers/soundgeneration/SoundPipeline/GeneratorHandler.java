package com.example.christophers.soundgeneration.SoundPipeline;

import android.os.Handler;
import android.os.Looper;

import com.example.christophers.soundgeneration.Configuration.BluetoothDataHandler;
import com.example.christophers.soundgeneration.MainActivity;
import com.example.christophers.soundgeneration.R;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by christopherS on 01.12.2016.
 */

public class GeneratorHandler extends Thread implements SoundHandler {
    boolean keepGenerating = false;
    boolean nextInterpolate = false;   //determines wether the next Audio-Piece will be interpolated
    public float nextVol = 0 / 100;
    public float nextFreq = 100;
    public float targetFreq=100;
    public int baseFreq;
    float targetVol = 100/100;
    public int spectrum;
    boolean invertingX=true;
    boolean invertingY=false;


    private final int sampleRate;
    private final int numSamples;
    private final double sample[];  //used for calculating the wave. Will later be written into generatedSnd[] but with 2 bytes for each double value.
    private float volume = 100 / 100;
    private float freqOfTone;
    private final byte generatedSnd[];  //twice as big as sample in order to Stereo
    private double offset = 0;   //the position of pi where i stopped last time (Sinus-Wave)
    int bullseyeRadius = 4; //1 means 126, 127 and 128 are in. 0 means only 127.
    boolean bullseyeMode=false;
    //Bullseye mode means that on the x-Axis only 3 different positions are interpreted and modulated into sound. Left-Bullseye-Right. This makes it easier to determine wether you are in the center or not.

    LinkedBlockingQueue<byte[]> soundQueue;
    PlayerThread pThread;
    GeneratorThread generatorThread;
    MainActivity main;

    public GeneratorHandler(LinkedBlockingQueue<byte[]> soundQueue, int sampleRate, int baseFreq, int spectrum, int framesPerBuffer, MainActivity main){
        this.soundQueue = soundQueue;
        this.sampleRate = sampleRate;
        numSamples = framesPerBuffer/2;

        sample = new double[framesPerBuffer/2];
        generatedSnd = new byte[framesPerBuffer];
        this.baseFreq = baseFreq;
        this.spectrum = spectrum;
        generatorThread = new GeneratorThread();
        this.main = main;
        pThread = new PlayerThread(soundQueue, framesPerBuffer, sampleRate);
        pThread.start();
    }

    class GeneratorThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            keepGenerating = true;
            stream();
        }
    }

    void stream() {
        while (!interrupted() && keepGenerating) { //run till an interrupt arrives
            genTone();  //generate the sound
            try {
                soundQueue.put(generatedSnd.clone());
            } catch (InterruptedException e) {
                keepGenerating = false;
                this.interrupt();
            }
        }
    }

    float maxFreqJumpPerSample = 2.5f;    //prevents audio glitches caused by to fast frequenzy changes. A snippet is rougly 0.5% of a second. 1 per Sample=>200per Second
    float maxVolJumpPerSample = 0.01f; //prevents audio glitches caused by to fast volume changes.
    void genTone() {
        float lastVolume = volume;
        float lastFreq = freqOfTone;
        if(lastFreq-targetFreq<-maxFreqJumpPerSample) nextFreq = lastFreq + maxFreqJumpPerSample;
        else if(lastFreq-targetFreq>maxFreqJumpPerSample)  nextFreq = lastFreq - maxFreqJumpPerSample;
        else nextFreq = targetFreq;
        if(lastVolume-targetVol<-maxVolJumpPerSample) nextVol = lastVolume + maxVolJumpPerSample;
        else if(lastVolume-targetVol>maxVolJumpPerSample)  nextVol = lastVolume - maxVolJumpPerSample;
        else nextVol = targetVol;

        for (int i = 0; i < sample.length; i++) {
            volume = lastVolume + ((nextVol - lastVolume) * i / sample.length);      //interpolate between the old and new volume
            freqOfTone = lastFreq + ((nextFreq - lastFreq) * i / (sample.length));   //interpolate between the old and new frequenzy

            sample[i] = volume * Math.sin(offset + (2 * Math.PI * (i) / (sampleRate / freqOfTone)));
        }
        offset = offset + (2 * Math.PI * (sample.length) / (sampleRate / freqOfTone)); //so the next wave will start at the same "height" as the previous ended. Could add %PI*2
        nextInterpolate = false;

        //convert to 16bit PCM sound array
        //assumes the sample buffer is normalised
        int idx = 0;
        for (final double dVal : sample) {
            //scale to max. amplitude
            final short val = (short) ((dVal * 32767));
            //in 16bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff); //only take the last bits, first
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);    //second take the first bits in the buffer
        }
    }

    public void changeVol(int vol) {
        targetVol = ((float) vol) / 100;
    }


    float leftVol;
    float rightVol;
    float distToMiddle;
    public void changeX(int balance) {      //127 is the middle
        if(invertingX) balance=254-balance;    //invert, so it appears on the right side
        if(bullseyeMode){
            if(Math.abs(balance-127)<=bullseyeRadius){
                pThread.targetRightVolume= 1.0f;
                pThread.targetLeftVolume = 1.0f;
            }
            if(balance>127+bullseyeRadius){
                pThread.targetRightVolume = 0.0f;
                pThread.targetLeftVolume  = 1.0f;
            }
            if(balance<127-bullseyeRadius){
                pThread.targetRightVolume = 1.0f;
                pThread.targetLeftVolume  = 0.0f;
            }
        }
        else {
            leftVol = 1f;     //middle is both tones on 100, else one channel is always 100%.
            rightVol = 1f;
            if (balance > 127) {
                distToMiddle = balance - 127;
                //rightVol = 0.9f - distToMiddle * 0.0071f;
                rightVol = 1.0f - distToMiddle * 0.0071f;
                rightVol = rightVol * rightVol * rightVol * rightVol * rightVol;
                pThread.targetRightVolume = rightVol;
                pThread.targetLeftVolume = 1.0f;
            } else if (balance < 127) {
                distToMiddle = 127 - balance;
                //leftVol = 0.9f - distToMiddle * 0.0071f;
                leftVol = 1.0f - distToMiddle * 0.0071f;
                leftVol = leftVol * leftVol * leftVol * leftVol * leftVol;
                pThread.targetLeftVolume = leftVol;
                pThread.targetRightVolume = 1.0f;
            } else {
                pThread.targetLeftVolume = 1.0f;
                pThread.targetRightVolume = 1.0f;
            }
        }
    }

    float freq;
    public void changeY(int yValue) {
        if(invertingY) yValue=254-yValue;
        freq = Math.abs(127-yValue);
        freq = freq/ 127;  //difference to the middle in percent
        freq = 1-freq; //invert distance ==> closer to middle means higher number
        freq = freq * freq * freq;// * freq *freq;
        freq = spectrum * freq;

        targetFreq = baseFreq + freq;
        nextInterpolate = true;
    }

    public void startSound(){
        if(generatorThread.isAlive()){
            //toastMessage("Sound-Generator läuft bereits.");
        }
        else{
            generatorThread = new GeneratorThread();
            generatorThread.start();
            //toastMessage("Generator gestartet");
        }
    }

    public void stopSound(){
        if(generatorThread.isAlive()){
            changeVol(0);
            generatorThread.interrupt();
            //toastMessage(main.getString(R.string.soundGen_halted_toast));
        }
        //else toastMessage(main.getString(R.string.soundGenerator_allreadyStopped_toast));
    }

    @Override
    public void invertX() {
        invertingX = !invertingX;
        float temp = pThread.targetLeftVolume;
        pThread.targetLeftVolume = pThread.targetRightVolume;
        pThread.targetRightVolume = temp;
    }

    @Override
    public void invertY(){
        invertingY = !invertingY;
    }

    @Override
    public boolean xIsInverted() {
        return invertingX;
    }

    @Override
    public boolean yIsInverted() {
        return invertingY;
    }

    @Override
    public void trimBalance(float balancePercentage) {
        if(balancePercentage>=0){
            pThread.rightChannelTrim = 1.0f - balancePercentage;
            pThread.leftChannelTrim = 1.0f;
        }
        if(balancePercentage<0){
            balancePercentage = balancePercentage * -1;
            pThread.rightChannelTrim = 1.0f;
            pThread.leftChannelTrim = 1.0f - balancePercentage;
        }
    }

    public boolean toggleBullseye() {
        bullseyeMode = !bullseyeMode;
        return bullseyeMode;
    }

    public void setBullseyeRadius(int bullseyeRad) {
        bullseyeRadius = bullseyeRad;
    }

    @Override
    public void setBaseFreq(int freq){
        baseFreq = freq;
    }

    private void toastMessage(String text) {
        main.toastMessage(text);
    }

}
