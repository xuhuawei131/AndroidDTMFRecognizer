package com.xuhuawei.dtmfrecognizer.dtmfhelper;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.xuhuawei.dtmfrecognizer.OnReconzerHelperListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.Context.TELEPHONY_SERVICE;


public class Controller {
    private boolean started;

    private RecordTask recordTask;
    private RecognizerTask recognizerTask;
    private OnReconzerHelperListener listener;
    private BlockingQueue<DataBlock> blockingQueue;

    private Character lastValue;
    private Context contxt;

    public Controller(Context contxt,OnReconzerHelperListener listener) {
        this.contxt = contxt;
        this.listener=listener;
    }

    public void changeState() {
        if (!started) {

            lastValue = ' ';

            blockingQueue = new LinkedBlockingQueue<DataBlock>();

            listener.start();

            recordTask = new RecordTask(this, blockingQueue);

            recognizerTask = new RecognizerTask(this, blockingQueue);

            recordTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            recognizerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            started = true;
        } else {

            listener.stop();

            recognizerTask.cancel(true);
            recordTask.cancel(true);

            started = false;
        }
    }

    public boolean isStarted() {
        return started;
    }


    public int getAudioSource() {
        TelephonyManager telephonyManager = (TelephonyManager) contxt.getSystemService(TELEPHONY_SERVICE);

        if (telephonyManager.getCallState() != TelephonyManager.PHONE_TYPE_NONE)
            return MediaRecorder.AudioSource.VOICE_DOWNLINK;

        return MediaRecorder.AudioSource.MIC;
    }

    public void spectrumReady(Spectrum spectrum) {
        listener.drawSpectrum(spectrum);
    }

    public void keyReady(char key) {
        Log.v("xhw","keyReady key="+key);
        if (key != ' ')
            if (lastValue != key)
                listener.addText(key);
        lastValue = key;
    }

}
