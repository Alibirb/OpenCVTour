package com.thanh.photodetector;

import alicrow.opencvtour.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class DisplayResultActivity extends Activity {

	public static final String PHOTO_MIME_TYPE = "image/png";	
	public static final String EXTRA_PHOTO_URI = 
		"com.thanh.photodetector.DisplayResultActivity.extra.PHOTO_URI"; 
//	private Uri mUri; 
	public static final String EXTRA_PHOTO_ID =
		"com.thanh.photodetector.DisplayResultActivity.extra.PHOTO_ID";
	public static final String EXTRA_PHOTO_PATH =
			"com.thanh.photodetector.DisplayResultActivity.extra.PHOTO_PATH";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent(); 
		
		final ImageView imageView = new ImageView(this);
//		mUri = intent.getParcelableExtra(EXTRA_PHOTO_URI); 
//		imageView.setImageURI(mUri); 
		
//		int id = intent.getIntExtra(EXTRA_PHOTO_ID, 0);
//		imageView.setImageResource(id);
		
		String path = intent.getStringExtra(EXTRA_PHOTO_PATH);
		imageView.setImageDrawable(Drawable.createFromPath(path));
		
		setContentView(imageView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_result, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
