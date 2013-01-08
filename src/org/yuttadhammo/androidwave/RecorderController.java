package org.yuttadhammo.androidwave;

import java.io.File;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

// This shows the UI, and allows for starting the recording process.
public class RecorderController extends Activity
{

	private boolean mIsBound = false;

	private RecorderService mService = null;

	private ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mService = ((RecorderService.LocalBinder) service).getService();

			// Tell the user about this for our demo.
//			Toast.makeText(RecorderController.this, "Recorder Service Connected",
//					Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className)
		{
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mService = null;
//			Toast.makeText(RecorderController.this, "Recorder Service Disconnected",
//					Toast.LENGTH_SHORT).show();
		}
	};

	void doBindService()
	{
		if (mIsBound)
			return;
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(RecorderController.this, RecorderService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService()
	{
		if (mIsBound)
		{
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
//		mService.clearStateUpdateListener(this);
		doUnbindService();
	}

	private OnClickListener mRecordListener = new OnClickListener()
	{
		public void onClick(View v)
		{
//			Log.d("RecorderController", "mRecordListener");
			if (mService != null)
			{
				if(mService.isRecording()) {
					mService.stopRecording();
					recButton.setText(R.string.record);
				}
				else {
	//				Log.d("RecorderController", "mRecordListener:startRecording()");
					mService.startRecording(null);
					recButton.setText(R.string.stop);
				}
				
			}
		}
	};

	private MediaPlayer mp;

	private OnClickListener mPlayListener = new OnClickListener()
	{

		public void onClick(View v)
		{
			if(mp != null && mp.isPlaying()){
				mp.stop();
				playButton.setText(R.string.play);
				return;
			} 
			
			File lastFile = null;
			if (mService == null || !mService.isRecording())
			{
				File dir = new File(Environment.getExternalStorageDirectory().getPath()+"/Sound Recordings/");
				if (dir != null){
				    File[] filenames = dir.listFiles();
			        for (File tmpf : filenames){
			        	if (lastFile == null || tmpf.compareTo(lastFile) > 0)
			        		lastFile = tmpf;
			        }
				}

			}
			if(lastFile != null) {
				mp = MediaPlayer.create(RecorderController.this,Uri.fromFile(lastFile));
			    mp.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer arg0) {
						playButton.setText(R.string.play);
						
					}
			    	
			    });
				mp.start();
				playButton.setText(R.string.stop);
			}
			else
				Toast.makeText(RecorderController.this, "No files recorded", Toast.LENGTH_SHORT).show();
		}
	};

	private Button recButton;

	private Button playButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		doBindService();
		setContentView(R.layout.main);

		// Watch for button clicks.
		recButton = (Button)findViewById(R.id.record);
		recButton.setOnClickListener(mRecordListener);

		playButton = (Button)findViewById(R.id.play);
		playButton.setOnClickListener(mPlayListener);

//		mService.setStateUpdateListener(this);
	}
}
