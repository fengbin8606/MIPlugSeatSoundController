
package com.feng.soundcontroller;

import java.util.LinkedList;
import java.util.Queue;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class SoundRecorder {

    private static final String TAG = "SoundRecorder";

    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    Object mLock;

    static double dBValue = 60;

    public static boolean firstClick = true;

    public static Queue<Long> queue = new LinkedList<Long>();

    public SoundRecorder() {
        mLock = new Object();
    }

    public void getNoiseLevel() {

        if (isGetVoiceRun) {
            Log.e(TAG, "is recording......");
            return;
        }

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord init failed.");
        }

        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                Log.i(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);

                while (isGetVoiceRun) {
                    // r��ʵ�ʶ�ȡ�����ݳ��ȣ�һ�����r��С��buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // �� buffer ����ȡ��������ƽ��������
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // ƽ���ͳ��������ܳ��ȣ��õ�������С��
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "dB value: " + volume);

                    if (volume > dBValue) {
                        Log.e(TAG, "dB value: " + volume);
                        if (firstClick) {
                            Log.i(TAG, "first click");
                            MyAccessibility.performClick();
                            firstClick = false;
                        }

                        long currentTime = System.currentTimeMillis();
                        Log.i(TAG, "currentTime��" + currentTime);
                        queue.offer(currentTime);
                    }

                    // ���һ��ʮ��
                    synchronized (mLock) {
                        try {
                            mLock.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }

}