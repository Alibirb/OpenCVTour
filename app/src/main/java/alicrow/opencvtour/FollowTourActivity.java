package alicrow.opencvtour;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class FollowTourActivity extends Activity implements View.OnClickListener {

	private static final String TAG = "FollowTourActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_follow_tour);

		findViewById(R.id.find_closest_tour_item).setOnClickListener(this);

		doBindService();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_follow_tour, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
			case R.id.find_closest_tour_item:
				updateDisplay(mBoundService.getCurrentLocation());
				break;
		}
	}



	private LocationService mBoundService;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been established, giving us the service object we can use to interact with the service.  Because we have bound to a explicit service that we know is running in our own process, we can cast its IBinder to a concrete class and directly access it.
			mBoundService = ((LocationService.LocationServiceBinder)service).getService();
			//mBoundService.addListener(_this);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never see this happen.
			mBoundService = null;
		}
	};

	boolean mIsBound = false;

	void doBindService() {
		// Establish a connection with the service.  We use an explicit class name because we want a specific service implementation that we know will be running in our own process (and thus won't be supporting component replacement by other applications).
		bindService(new Intent(this, LocationService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//mBoundService.removeListener(this);
		doUnbindService();
	}

/*
	public void onLocationUpdated(Location location) {
		updateDisplay(location);
		Toast.makeText(this, "location updated", Toast.LENGTH_SHORT).show();
	}*/

	private void updateDisplay(Location current_location) {
		if (current_location == null) {
			Log.e(TAG, "got null current location");
			((TextView) findViewById(R.id.current_location)).setText("Current location: unknown (Ensure that location is enabled on your device)");
			Toast.makeText(this, "Couldn't determine location. Ensure location is enabled on your device, and/or wait a few seconds and try again.", Toast.LENGTH_LONG).show();
			return;
		}
		((TextView) findViewById(R.id.current_location)).setText("Current location: " + current_location.getLatitude() + ", " + current_location.getLongitude() + ", accuracy: " + current_location.getAccuracy() + " meters");


		TourItem closest_item = null;
		double closest_distance = Double.MAX_VALUE;
		for(TourItem item : Tour.getCurrentTour().getTourItems()) {
			if(item.getLocation() != null && current_location.distanceTo(item.getLocation()) < closest_distance) {
				closest_item = item;
				closest_distance = current_location.distanceTo(item.getLocation());
			}
		}
		if(closest_item != null) {
			findViewById(R.id.closest_tour_item_name).setVisibility(View.VISIBLE);
			findViewById(R.id.closest_tour_item_location).setVisibility(View.VISIBLE);
			findViewById(R.id.closest_tour_item_description).setVisibility(View.VISIBLE);

			Log.i(TAG, "closest item is named " + closest_item.getName());
			Log.i(TAG, "image filename is '" + closest_item.getImageFilename() + "'");

			((TextView) findViewById(R.id.closest_tour_item_name)).setText(closest_item.getName());
			((TextView) findViewById(R.id.closest_tour_item_location)).setText("location: " + closest_item.getLocation().getLatitude() + ", " + closest_item.getLocation().getLongitude());
			((TextView) findViewById(R.id.closest_tour_item_description)).setText(closest_item.getDescription());
			if(!closest_item.getImageFilename().equals("")) {
				/// display a picture of the TourItem, scaled to fit available space
				findViewById(R.id.closest_tour_item_picture).setVisibility(View.VISIBLE);
				String filepath = closest_item.getImageFilename();
				BitmapFactory.Options bounds = Utilities.getBitmapBounds(filepath);
				int width = findViewById(R.id.closest_tour_item_picture).getWidth();
				int height = width * (bounds.outHeight / bounds.outWidth);
				Bitmap image = Utilities.decodeSampledBitmap(closest_item.getImageFilename(), width, height);
				((ImageView) findViewById(R.id.closest_tour_item_picture)).setImageBitmap(image);
			} else {
				findViewById(R.id.closest_tour_item_picture).setVisibility(View.INVISIBLE);
			}
		} else {
			/// no TourItem detected.
			findViewById(R.id.closest_tour_item_name).setVisibility(View.INVISIBLE);
			findViewById(R.id.closest_tour_item_location).setVisibility(View.INVISIBLE);
			findViewById(R.id.closest_tour_item_description).setVisibility(View.INVISIBLE);
		}
	}


}
