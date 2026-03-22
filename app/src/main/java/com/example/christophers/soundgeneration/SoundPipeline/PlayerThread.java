package com.example.christophers.soundgeneration.SoundPipeline;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by christopherS on 01.12.2016.
 */

public class PlayerThread extends Thread {
    LinkedBlockingQueue<byte[]> soundQueue;
    final AudioTrack audioTrack;
    boolean keepPlaying = true;
    public float targetLeftVolume  = 1.0f;
    public float targetRightVolume = 1.0f;
    float leftVolume = 0.0f;    //start with 0 and let it gracefully scale up
    float rightVolume= 0.0f;

    public float leftChannelTrim  = 1.0f;
    public float rightChannelTrim = 1.0f;

    byte[] audioOutput;


    public PlayerThread(LinkedBlockingQueue<byte[]> soundQueue, int framesPerBuffer, int sampleRate){
        this.soundQueue = soundQueue;

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, framesPerBuffer, AudioTrack.MODE_STREAM);
        int bufSize = audioTrack.getBufferSizeInFrames();    //648 => 8000 per Second => 80ms delay at max. 10 Snippets fit into the buffer
        Log.d("Delay", "BufSize: "+bufSize);
    }

    @Override
    public void run() {
        Looper.prepare();
        audioTrack.play();
        play();
    }

    public void play() {
        try {
            while (keepPlaying) {
                audioOutput = soundQueue.take();    //wait for the next audio
                playSound();
                //sleep((int)(snippetDuration*1000*0.75f));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void playSound() throws InterruptedException {
        audioTrack.write(audioOutput, 0, audioOutput.length);
        smoothVolChange();
        audioTrack.setStereoVolume(leftVolume * leftChannelTrim, rightVolume * rightChannelTrim);
    }

    float maxVolumeStep = 0.03f;
    private void smoothVolChange() {
        if (targetLeftVolume - leftVolume > maxVolumeStep) leftVolume += maxVolumeStep;
        else if (targetLeftVolume - leftVolume < -maxVolumeStep) leftVolume -= maxVolumeStep;
        else leftVolume = targetLeftVolume;
        if (targetRightVolume - rightVolume > maxVolumeStep) rightVolume += maxVolumeStep;
        else if (targetRightVolume - rightVolume < -maxVolumeStep) rightVolume -= maxVolumeStep;
        else rightVolume = targetRightVolume;
    }
}