package org.yuttadhammo.androidwave;

import java.io.IOException;

public interface AudioWriter
{
	public void open(String filename, int samplingRate) throws IOException, IllegalArgumentException;
	// Write sound data. The format is 16 bit mono.
	public void write(short[] buffer, int offs, int len) throws IOException;
	public void close();
}
