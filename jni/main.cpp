#include "lame.h"

#include <iostream>

using namespace std;

int main(int argc, char *argv[])
{
	int read, write;

	FILE* pcm = fopen("file.pcm", "rb");
	FILE* mp3 = fopen("file.mp3", "wb");

	if (!pcm || !mp3)
	{
		cerr << "Error opening file.pcm or file.mp3" << endl;
		return 1;
	}

	const int PCM_SIZE = 8192;
	const int MP3_SIZE = 8192;

	short int pcm_buffer[PCM_SIZE*2];
	unsigned char mp3_buffer[MP3_SIZE];

	lame_t lame = lame_init();
	if (!lame)
	{
		cerr << "Error initialising lame." << endl;
		return 2;
	}
	lame_set_in_samplerate(lame, 44100);
	lame_set_VBR(lame, vbr_default);
	lame_init_params(lame);

	do {
		read = fread(pcm_buffer, 2*sizeof(short int), PCM_SIZE, pcm);
		if (read == 0)
			write = lame_encode_flush(lame, mp3_buffer, MP3_SIZE);
		else
			write = lame_encode_buffer_interleaved(lame, pcm_buffer, read, mp3_buffer, MP3_SIZE);
		fwrite(mp3_buffer, write, 1, mp3);
	} while (read != 0);

	lame_close(lame);
	fclose(mp3);
	fclose(pcm);

	return 0;
}
