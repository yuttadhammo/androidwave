package org.yuttadhammo.androidwave;

import android.util.Log;

public class MP3Writer implements AudioWriter
{
	@Override
	public void open(String filename, int samplingRate)
	{
		Log.d("MP3Writer", "Recording at sample rate: " + samplingRate);
	
	}	
	// Write sound data. The format is 16 bit mono.
	@Override
	public void write(short[] buffer, int offs, int len)
	{
		Log.d("MP3Writer", "Got data: " + len);
	}
	
	@Override
	public void close()
	{
		Log.d("MP3Writer", "Close");
	}


}
