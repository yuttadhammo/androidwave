package org.yuttadhammo.androidwave;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Handler;
import android.preference.PreferenceManager;

public class SoundMeter {

    static final private double EMA_FILTER = 0.6;

    private MediaRecorder mRecorder = null;
    private double mEMA = 0.0;

	private Handler hUpdate;

	private Runnable rUpdate;

	private Activity activity;

	public SoundMeter(Activity activity) {
		this.activity = activity;
	}
	
    public void start() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    	if(!prefs.getBoolean("meter_show", false)) {
    		this.stop();
    		return;
    	}
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null"); 
            try {
				mRecorder.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            mRecorder.start();
            mEMA = 0.0;
        }
        else return;
        hUpdate = new Handler();
        rUpdate = new Runnable() {

			@Override
			public void run() {
				int height = activity.findViewById(R.id.meter).getBottom();
				double amp = getAmplitude();
				int maxAmp = 2;
				int meter = (int) (amp/maxAmp*height*-1);
				activity.findViewById(R.id.meter_cover).setY(meter);
			}
        };

        Thread tUpdate = new Thread() {
        	public void run() {
        	  while(mRecorder != null) {
            	hUpdate.post(rUpdate);
            	try {
            		sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	  }
        	}
        };
        tUpdate.start();
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude()/2700.0);
        else
            return 0;
    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

}