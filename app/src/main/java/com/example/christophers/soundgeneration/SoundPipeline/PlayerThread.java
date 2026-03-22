package com.example.christophers.soundgeneration.SoundPipeline;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by christopherS on 01.12.2016.
 */

public class PlayerThread extends Thread {
    LinkedBlockingQueue<byte[]> soundQueue;
    final AudioTrack audioTrack;
    boolean keepPlaying = true;
    byte[] audioOutput;

    public PlayerThread(LinkedBlockingQueue<byte[]> soundQueue, int bufferSizeInBytes, int sampleRate){
        this.soundQueue = soundQueue;

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        int bufSize = audioTrack.getBufferSizeInFrames();
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
                audioTrack.write(audioOutput, 0, audioOutput.length);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}