package org.yuttadhammo.androidwave;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;


public class WaveSettingsActivity extends PreferenceActivity {
	
	private Context context;
	private Activity activity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		this.context = getApplicationContext();
		this.activity = this;
		addPreferencesFromResource(R.xml.preferences);
		
        // Load the sounds
        final ListPreference sampleRate = (ListPreference)findPreference("sample_rate");
    	
        ArrayList<Integer> csa = getValidSampleRates();
        CharSequence[] csl = new CharSequence[csa.size()];
        int i = 0;
        for(int sr : csa) {
        	csl[i++] = sr+"";
        }
    	CharSequence [] entries = csl;
    	
		sampleRate.setEntries(entries);
		sampleRate.setEntryValues(entries);

		sampleRate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					final Object newValue) {

                sampleRate.setSummary((String)newValue);
				return true;
			}
			
		});
		sampleRate.setSummary(sampleRate.getEntry());
		
		final EditTextPreference dirPref = (EditTextPreference)findPreference("data_dir");
		if(dirPref.getText() == null || dirPref.getText().equals(""))
			dirPref.setText(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Sound Recordings");
		dirPref.setSummary(dirPref.getText());
		
		dirPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					final Object newValue) {
				final File f = new File((String) newValue);
				if(!f.exists() || !f.isDirectory()) {
			        new AlertDialog.Builder(WaveSettingsActivity.this)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setTitle(R.string.mkdir)
			        .setMessage(R.string.want_mkdir)
			        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

			            @Override
			            public void onClick(DialogInterface dialog, int which) {
			            	if(!f.mkdirs()) {
								Toast.makeText(WaveSettingsActivity.this, "Cannot make directory",
										Toast.LENGTH_SHORT).show();
			            	}
			            	else {
			            		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			                    SharedPreferences.Editor editor = prefs.edit();
			                    editor.putString("data_dir", (String) newValue);
			                    editor.commit();
			    				dirPref.setSummary((String)newValue);
			            	}
			            }

			        })
			        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

			            @Override
			            public void onClick(DialogInterface dialog, int which) {
			            }

			        })
			        .show();
			        return false;
				}
				dirPref.setSummary((String)newValue);
				return true;
			}
			
		});

	}
	
	public ArrayList<Integer> getValidSampleRates() {
		ArrayList<Integer> csa = new ArrayList<Integer>();
	    for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
	        int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
	        if (bufferSize > 0) {
	            // buffer size is valid, Sample rate supported
	        	csa.add(rate);
	        }
	    }
	    return csa;
	}
}
