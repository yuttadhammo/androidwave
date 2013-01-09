package org.yuttadhammo.androidwave;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public class ScreenSlidePageFragment extends Fragment {
	private int position;
	private ViewGroup rootView;
	public ScreenSlidePageFragment() {
	}

	public ScreenSlidePageFragment(int position) {
    	this.position = position;
    }
	@SuppressLint("NewApi")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		if(position == 0)
			rootView = (ViewGroup) inflater.inflate(R.layout.main, container, false);
		else
			rootView = (ViewGroup) inflater.inflate(R.layout.file_picker_content_view, container, false);
			
        return rootView;
    }

	
}
