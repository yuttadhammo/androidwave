package org.yuttadhammo.androidwave;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

// Simple class allowing one to write to a wav file and send the
// left and right channel data in different function calls.

public class WavWriter implements AudioWriter
{
	// The file we write the wav to.
	RandomAccessFile raf = null;
	// The number of samples we have written. 
	int samplesWritten = 0;
	
	@Override
	public void open(String filename, int samplingRate) throws IOException, IllegalArgumentException
	{
		if (samplingRate != 8000 && samplingRate != 11025 && samplingRate != 16000
				&& samplingRate != 22050 && samplingRate != 32000
				&& samplingRate != 44100 && samplingRate != 48000)
			throw new IllegalArgumentException("Invalid sampling rate.");
		try
		{
			raf = new RandomAccessFile(filename, "rw");
			raf.setLength(0); // Truncate the file if it already exists.

			// Write wav header.
			
			// Chunk metadata.
			raf.writeBytes("RIFF");
			// Size of chunk (fill in later). This is the size of entire file minus 8 bytes
			// (i.e. the size of everything following this int).
			raf.writeInt(0);
			
			// Chunk header.
			raf.writeBytes("WAVE");
			raf.writeBytes("fmt ");
			raf.writeInt(B2L(16)); // Size of the rest of this header. B2L is Big to Little endian.
			raf.writeShort(B2L_s(1)); // 1 = PCM.
			raf.writeShort(B2L_s(1)); // 1 = mono, 2 = stereo.
			raf.writeInt(B2L(samplingRate)); // Sample rate, 8 kHz
			raf.writeInt(B2L(samplingRate*2*1)); // Byte rate. Pretty redundant.
			raf.writeShort(B2L_s(2)); // Bytes per frame.
			raf.writeShort(B2L_s(16)); // Bits per sample.
			
			raf.writeBytes("data");
			raf.writeInt(0); // Fill in later, number of bytes of data.
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error creating output file.");
			raf = null;
			throw e;
		}
	}
	
	@Override
	public void write(short[] buffer, int offs, int len) throws IOException
	{
		if (samplesWritten > 2000*1024*1024)
		{
			// File too big. 2000 MB...
			return;
		}
		
		if (raf == null)
			return;

		try
		{
	        byte[] bb = new byte[2 * len];
			for (int i = 0; i < len; ++i)
			{
				// I did use writeShort(), but this creates a new byte[2] on
				// EVERY CALL which really slows things down.
				int k = buffer[offs+i];
				bb[i*2+0] = (byte) (k);
				bb[i*2+1] = (byte) (k >> 8);
			}
			raf.write(bb, 0, 2*len);
			samplesWritten += len;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error writing to output file.");
			throw e;
		}
	}
	public void close()
	{
		if (raf == null)
			return;
		try
		{
			// Seek back.
			raf.seek(4);
			raf.writeInt(B2L(36 + samplesWritten * 2));
			raf.seek(40);
			raf.writeInt(B2L(samplesWritten * 2));
			raf.close();
			raf = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error writing final data to output file.");
		}
	}
	
	// Convert big endian short to little endian.
	int B2L_s(int i)
	{
		return (((i >> 8) & 0x00ff) + ((i << 8) & 0xff00));
	}

	// Convert big endian int ot little endian
	int B2L(int i)
	{
		return ((i & 0xff) << 24) + ((i & 0xff00) << 8) + ((i & 0xff0000) >> 8)
				+ ((i >> 24) & 0xff);
	}
}
