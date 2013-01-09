package org.yuttadhammo.androidwave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.yuttadhammo.soundfile.CheapSoundFile;

// This shows the UI, and allows for starting the recording process.
public class RecorderController extends Activity implements WaveformView.WaveformListener
{

	private SharedPreferences prefs;

	private Button recButton;
	private Button playButton;

	private static File playingFile;
	private static File dir;

	private static TextView info;

	private static WaveformView mWaveformView;

	private static float mDensity;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		doBindService();
		setContentView(R.layout.main);

		@SuppressWarnings("deprecation")
		int api = Integer.parseInt(Build.VERSION.SDK);
		
		if (api >= 14) {
			getActionBar().setHomeButtonEnabled(true);
		}
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mWaveformView = (WaveformView)findViewById(R.id.waveform);
        mWaveformView.setListener(this);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;
        
		info = (TextView) findViewById(R.id.text);
		
		resetPlayingFile();
		
		// Watch for button clicks.
		recButton = (Button)findViewById(R.id.record);
		recButton.setOnClickListener(mRecordListener);

		playButton = (Button)findViewById(R.id.play);
		playButton.setOnClickListener(mPlayListener);
		
		Intent intent = this.getIntent();
		
    	if(intent.getData() != null) {
			File file = new File(intent.getData().getPath());
			setPlayingFile(file);
			this.playFile();
    	}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		dir = new File(prefs.getString("data_dir", Environment.getExternalStorageDirectory().getPath()+"/Sound Recordings/"));
	}
	

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
//		mService.clearStateUpdateListener(this);
		doUnbindService();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		super.onOptionsItemSelected(item);
		Bundle dataBundle = new Bundle();
		
		//SharedPreferences.Editor editor = prefs.edit();
		Intent intent;
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            finish();
	            return true;
			case R.id.menuFile:
				intent = new Intent(getBaseContext(), FilePickerActivity.class);
				startActivityForResult(intent, 0);
				break;

			case (int)R.id.menuPrefs:
				intent = new Intent(this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;

			default:
				return false;
	    }
		return true;
	}	

	
	private boolean mIsBound = false;

	private RecorderService mService = null;

	protected MessageHandler handler = new MessageHandler();

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
			mService.setHandler(handler);
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
					recButton.setBackgroundColor(0xFFCC0000);
				}
				else {
	//				Log.d("RecorderController", "mRecordListener:startRecording()");
					mService.startRecording(dir);
					recButton.setText(R.string.stop);
					recButton.setBackgroundColor(0xFF990000);
				}
				
			}
		}
	};

	private MediaPlayer mp;

	private OnClickListener mPlayListener = new OnClickListener()
	{

		public void onClick(View v)
		{
			if(mp != null) {
				stopPlayFile();
				return;
			}
			
			if (mService != null && mService.isRecording())
				return;

			playFile();
		}
	};

	private void playFile() {
		if(playingFile == null || !playingFile.isFile())
			resetPlayingFile();
		if(playingFile == null) {
			Toast.makeText(RecorderController.this, "Cannot play file", Toast.LENGTH_SHORT).show();
			return;
		}
		if(mp != null)
			mp.stop();
		Log.d("file to play",playingFile.getAbsolutePath());
		mp = MediaPlayer.create(RecorderController.this,Uri.fromFile(playingFile));
	    if(mp == null){
			Toast.makeText(RecorderController.this, "Cannot play file", Toast.LENGTH_SHORT).show();
			return;
	    }
		mp.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer arg0) {
				stopPlayFile();
			}
	    	
	    });
		mp.start();
		playButton.setText(R.string.stop);
		playButton.setBackgroundColor(0xFF009900);
	}
    
	protected void stopPlayFile() {
		mp.stop();
		mp.release();
		mp = null;

		playButton.setText(R.string.play);
		playButton.setBackgroundColor(0xFF00CC00);

		return;
	}


	private long mLoadingLastUpdateTime = 0;

	private Handler mHandler;

	private static CheapSoundFile cSoundFile;

	private static void setPlayingFile(File file){
		if(file == null)
			return;
		playingFile = file;
		info.setText(file.getAbsolutePath());
		
		if(playingFile == null)
			return;
		
		try {
			cSoundFile = CheapSoundFile.create(playingFile.getAbsolutePath(),
		        new CheapSoundFile.ProgressListener() {
		            public boolean reportProgress(double fractionComplete) {
		                return true;
		            }
		        });
	        if (cSoundFile != null) {
	            mWaveformView.setSoundFile(cSoundFile);
	            mWaveformView.recomputeHeights(mDensity);
	        }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void resetPlayingFile() {
		File lastFile = null;
		if (dir != null){
		    File[] filenames = dir.listFiles();
	        for (File tmpf : filenames){
	        	if (lastFile == null || tmpf.compareTo(lastFile) > 0)
	        		lastFile = tmpf;
	        }
		}
		setPlayingFile(lastFile);
	}
		
	protected void  onActivityResult (int requestCode, int resultCode, Intent  data) {
		
		// returning from file picker
		if(data != null && data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
			// Get the file path
			File f = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
			setPlayingFile(f);
			playFile();
		}
	}
	
   @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mHandler.postDelayed(new Runnable() {
                public void run() {
                    mWaveformView.recomputeHeights(mDensity);
                }
            }, 200);
    }

	public static class MessageHandler extends Handler {
		
		public MessageHandler() {
		}
	
		@Override
		public void handleMessage(Message msg) {
			Log.i("message",msg.what+"");

			setPlayingFile(new File((String) msg.obj));
		}
	}
	   
	@Override
	public void waveformTouchStart(float x) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void waveformTouchMove(float x) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void waveformTouchEnd() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void waveformFling(float x) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void waveformDraw() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void waveformZoomIn() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void waveformZoomOut() {
		// TODO Auto-generated method stub
		
	}
	
}
