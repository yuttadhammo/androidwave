/*
 * Copyright 2011 Anders Kalør
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// modified by Yuttadhammo <yuttadhammo@gmail.com>
// modifications released under GPL 3.0+

package org.yuttadhammo.androidwave;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FilePickerActivity extends ListActivity {
	
	/**
	 * The file path
	 */
	public final static String EXTRA_FILE_PATH = "file_path";
	
	/**
	 * Sets whether hidden files should be visible in the list or not
	 */
	public final static String EXTRA_SHOW_HIDDEN_FILES = "show_hidden_files";

	/**
	 * The allowed file extensions in an ArrayList of Strings
	 */
	public final static String EXTRA_ACCEPTED_FILE_EXTENSIONS = "accepted_file_extensions";
	
	protected File mDirectory;
	protected ArrayList<File> mFiles;
	protected FilePickerListAdapter mAdapter;
	protected boolean mShowHiddenFiles = false;
	protected String[] acceptedFileExtensions = {"wav"};

	private LinearLayout navButtons;

	private SharedPreferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View contentView =  View.inflate(this, R.layout.file_picker_content_view, null);
        setContentView(contentView);
        
        navButtons = (LinearLayout) contentView.findViewById(R.id.navButtons);
      
		// Set the view to be shown if the list is empty
		LayoutInflater inflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View emptyView = inflator.inflate(R.layout.file_picker_empty_view, null);
		((ViewGroup)getListView().getParent()).addView(emptyView);
		getListView().setEmptyView(emptyView);
		
		TextView listHeader = new TextView(this);
		listHeader.setText(R.string.chooseFile);
		listHeader.setTextSize(26);
		getListView().addHeaderView(listHeader);
		
		// Set initial directory
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mDirectory = new File(prefs.getString("data_dir", Environment.getExternalStorageDirectory().getPath()+"/Sound Recordings/"));
		Log.i(prefs.getString("data_dir", "null"),prefs.getString("data_dir", Environment.getExternalStorageDirectory().getPath()+"/Sound Recordings/"));
		refreshNavButtons();
		
		// Initialize the ArrayList
		mFiles = new ArrayList<File>();
		
		// Set the ListAdapter
		mAdapter = new FilePickerListAdapter(this, mFiles);
		setListAdapter(mAdapter);
		
		// Initialize the extensions array to allow wav files
		acceptedFileExtensions = new String[] {"wav"};
		
		// Get intent extras
		if(getIntent().hasExtra(EXTRA_FILE_PATH)) {
			mDirectory = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));
		}
		if(getIntent().hasExtra(EXTRA_SHOW_HIDDEN_FILES)) {
			mShowHiddenFiles = getIntent().getBooleanExtra(EXTRA_SHOW_HIDDEN_FILES, false);
		}
		if(getIntent().hasExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS)) {
			ArrayList<String> collection = getIntent().getStringArrayListExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS);
			acceptedFileExtensions = (String[]) collection.toArray(new String[collection.size()]);
		}
		
		registerForContextMenu(findViewById(android.R.id.list));
	}
	
	@Override
	protected void onResume() {
		refreshFilesList();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.file, menu);
	    return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		super.onOptionsItemSelected(item);
		
		//SharedPreferences.Editor editor = prefs.edit();
		Intent intent;
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            finish();
	            return true;
			case R.id.menuDeleteAll:
		        new AlertDialog.Builder(this)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.delete_all)
		        .setMessage(R.string.really_delete_all)
		        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		        		// Set the extension file filter
		        		ExtensionFilenameFilter filter = new ExtensionFilenameFilter(acceptedFileExtensions);
		        		
		            	// Get the files in the directory
		        		File[] files = mDirectory.listFiles(filter);
		        		if(files != null && files.length > 0) {
		        			for(File f : files) {
		        				f.delete();
		        			}
		        		}
		            	refreshFilesList();
		            }

		        })
		        .setNegativeButton(android.R.string.no, null)
		        .show();	
				break;

			case (int)R.id.menuPrefs:
				intent = new Intent(this, WaveSettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;

			default:
				return false;
	    }
		return true;
	}	


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
       	inflater.inflate(R.menu.main_longclick, menu);
        
	    menu.setHeaderTitle(getString(R.string.file_options));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    int index = info.position;
        switch (item.getItemId()) {
			case R.id.delete:
				File newFile = (File)getListView().getItemAtPosition(index);
				
				if(newFile.isFile()) {
					newFile.delete();
					this.refreshFilesList();
				} 
				return true;
			default:
				break;
		}
		
		return super.onContextItemSelected(item);
	}

	
	private void refreshNavButtons() {
		
		navButtons.removeAllViews();
		
		String directory = mDirectory.getAbsolutePath();
		if(directory.equals("/"))
			directory = "";

		String[] directories = directory.split("/");
		int position = 0;
		for(String dir:directories) {
			int count = 0;
			Button navButton = new Button(this);
			navButton.setText(position==0?"/":dir);
			String newDir = "";
			for(String dir2 : directories) {
				if(count++ > position)
					break;
				newDir += "/"+dir2;
			}
			final String newDir2 = position==0?"/":newDir;
			navButton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					mDirectory = new File(newDir2);
					refreshNavButtons();
					refreshFilesList();
				}
			});
			position++;
			navButtons.addView(navButton);
		}
	}

	
	/**
	 * Updates the list view to the current directory
	 */
	protected void refreshFilesList() {
		// Clear the files ArrayList
		mFiles.clear();
		
		// Set the extension file filter
		ExtensionFilenameFilter filter = new ExtensionFilenameFilter(acceptedFileExtensions);
		
		// Get the files in the directory
		File[] files = mDirectory.listFiles(filter);
		if(files != null && files.length > 0) {
			for(File f : files) {
				if(f.isHidden() && !mShowHiddenFiles) {
					// Don't add the file
					continue;
				}
				
				// Add the file the ArrayAdapter
				mFiles.add(f);
			}
			
			Collections.sort(mFiles, new FileComparator());
		}
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File newFile = (File)l.getItemAtPosition(position);
		
		if(newFile.isFile()) {
			// Set result
			Intent extra = new Intent();
			extra.putExtra(EXTRA_FILE_PATH, newFile.getAbsolutePath());
			setResult(RESULT_OK, extra);
			// Finish the activity
			finish();
		} else {
			mDirectory = newFile;
			
			// refresh nav buttons
			refreshNavButtons();
			
			// Update the files list
			refreshFilesList();
		}
		
		super.onListItemClick(l, v, position, id);
	}
	
	
	private class FilePickerListAdapter extends ArrayAdapter<File> {
		
		private List<File> mObjects;
		
		public FilePickerListAdapter(Context context, List<File> objects) {
			super(context, R.layout.file_picker_list_item, android.R.id.text1, objects);
			mObjects = objects;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View row = null;
			
			if(convertView == null) { 
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.file_picker_list_item, parent, false);
			} else {
				row = convertView;
			}

			File object = mObjects.get(position);

			ImageView imageView = (ImageView)row.findViewById(R.id.file_picker_image);
			TextView textView = (TextView)row.findViewById(R.id.file_picker_text);
			// Set single line
			textView.setSingleLine(true);
			
			textView.setText(object.getName());
			if(object.isFile()) {
				// Show the file icon
				imageView.setImageResource(R.drawable.file_icon);
			} else {
				// Show the folder icon
				imageView.setImageResource(R.drawable.folder);
			}
			
			return row;
		}

	}
	
	private class FileComparator implements Comparator<File> {
	    @Override
	    public int compare(File f1, File f2) {
	    	if(f1 == f2) {
	    		return 0;
	    	}
	    	if(f1.isDirectory() && f2.isFile()) {
	        	// Show directories above files
	        	return -1;
	        }
	    	if(f1.isFile() && f2.isDirectory()) {
	        	// Show files below directories
	        	return 1;
	        }
	    	// Sort the directories alphabetically
	        return f1.getName().compareToIgnoreCase(f2.getName());
	    }
	}
	
	private class ExtensionFilenameFilter implements FilenameFilter {
		private String[] mExtensions;
		
		public ExtensionFilenameFilter(String[] extensions) {
			super();
			mExtensions = extensions;
		}
		
		@Override
		public boolean accept(File dir, String filename) {
			if(new File(dir, filename).isDirectory()) {
				// accept directories
				return true;
			}
			if(mExtensions != null && mExtensions.length > 0) {
				for(int i = 0; i < mExtensions.length; i++) {
					if(filename.endsWith(mExtensions[i])) {
						// The filename ends with the extension
						return true;
					}
				}
				// The filename did not match any of the extensions
				return false;
			}
			// No extensions has been set. Accept all file extensions.
			return true;
		}
	}
}
