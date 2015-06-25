package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to follow a Tour.
 */
public class FollowTourActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "FollowTourActivity";

	private LocationService.ServiceConnection _connection;
	private boolean _service_is_bound = false;
	private int _next_item_index = 0;

	private Uri _photo_uri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_follow_tour);

		findViewById(R.id.find_closest_tour_item).setOnClickListener(this);

		bindLocationService();

		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("next_item_index"))
				_next_item_index = savedInstanceState.getInt("next_item_index");
			if (savedInstanceState.containsKey("_photo_uri")) {
				_photo_uri = Uri.parse(savedInstanceState.getString("_photo_uri"));
			}
		}

		if(getNextTourItem() != null)
			((TextView) findViewById(R.id.directions)).setText(getNextTourItem().getDirections());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_follow_tour, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.find_closest_tour_item:
				_photo_uri = Utilities.takePicture(this);
				break;
		}
	}

	private void bindLocationService() {
		_connection = new LocationService.ServiceConnection();
		bindService(new Intent(this, LocationService.class), _connection, Context.BIND_AUTO_CREATE);
		_service_is_bound = true;
	}
	private void unbindLocationService() {
		if (_service_is_bound) {
			unbindService(_connection);
			_service_is_bound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindLocationService();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("next_item_index", _next_item_index);

		if(_photo_uri != null)
			outState.putString("_photo_uri", _photo_uri.toString());
	}

	private void updateDisplay() {
		Location current_location = _connection.getService().getCurrentLocation();
		if (current_location == null) {
			Log.e(TAG, "got null current location");
			((TextView) findViewById(R.id.current_location)).setText("Current location: unknown (Ensure that location is enabled on your device)");
			Toast.makeText(this, "Couldn't determine location. Ensure location is enabled on your device, and/or wait a few seconds and try again.", Toast.LENGTH_LONG).show();
			return;
		}

		((TextView) findViewById(R.id.current_location)).setText("Current location: " + current_location.getLatitude() + ", " + current_location.getLongitude() + ", accuracy: " + current_location.getAccuracy() + " meters");

		TourItem closest_item = Tour.getCurrentTour().getTourItem(Tour.getCurrentTour().getDetector().identifyObject(_photo_uri.getPath()));

		if(closest_item != null)
			Log.i(TAG, "closest item is named " + closest_item.getName());

		if(closest_item != null && (!Tour.getCurrentTour().getEnforceOrder() || getNextTourItem() == closest_item)) {
			displayTourItem(closest_item);
		} else {
			findViewById(R.id.closest_tour_item_name).setVisibility(View.INVISIBLE);
			findViewById(R.id.closest_tour_item_location).setVisibility(View.INVISIBLE);
			findViewById(R.id.closest_tour_item_description).setVisibility(View.INVISIBLE);
			findViewById(R.id.closest_tour_item_picture).setVisibility(View.INVISIBLE);
		}
	}

	private TourItem findClosestTourItem(Location current_location) {
		TourItem closest_item = null;
		double closest_distance = Double.MAX_VALUE;
		for(TourItem item : Tour.getCurrentTour().getTourItems()) {
			if(item.getLocation() != null && current_location.distanceTo(item.getLocation()) < closest_distance) {
				closest_item = item;
				closest_distance = current_location.distanceTo(item.getLocation());
			}
		}
		return closest_item;
	}

	private void displayTourItem(TourItem item) {
		findViewById(R.id.closest_tour_item_name).setVisibility(View.VISIBLE);
		findViewById(R.id.closest_tour_item_location).setVisibility(View.VISIBLE);
		findViewById(R.id.closest_tour_item_description).setVisibility(View.VISIBLE);
		findViewById(R.id.directions).setVisibility(View.VISIBLE);

		((TextView) findViewById(R.id.closest_tour_item_name)).setText(item.getName());
		if(item.getLocation() == null)
			findViewById(R.id.closest_tour_item_location).setVisibility(View.INVISIBLE);
		else
			((TextView) findViewById(R.id.closest_tour_item_location)).setText("location: " + item.getLocation().getLatitude() + ", " + item.getLocation().getLongitude());
		((TextView) findViewById(R.id.closest_tour_item_description)).setText(item.getDescription());

		if(item.hasMainImage()) {
			/// display a picture of the TourItem, scaled to fit available space
			ImageView item_picture = (ImageView) findViewById(R.id.closest_tour_item_picture);
			item_picture.setVisibility(View.VISIBLE);
			String filepath = item.getMainImageFilename();
			BitmapFactory.Options bounds = Utilities.getBitmapBounds(filepath);
			int width = item_picture.getWidth();
			int height = width * (bounds.outHeight / bounds.outWidth);
			Utilities.loadBitmap(item_picture, filepath, width, height);
		} else {
			findViewById(R.id.closest_tour_item_picture).setVisibility(View.INVISIBLE);
		}

		_next_item_index = Tour.getCurrentTour().getTourItems().indexOf(item) + 1;
		if(getNextTourItem() == null) {
			/// End of tour.
			((TextView) findViewById(R.id.directions)).setText("Tour complete");
		} else {
			if(!getNextTourItem().getDirections().equals("")) {
				((TextView) findViewById(R.id.directions)).setText(getNextTourItem().getDirections());
			} else {
				findViewById(R.id.directions).setVisibility(View.INVISIBLE);
			}
		}
	}

	private TourItem getNextTourItem() {
		if(Tour.getCurrentTour().getTourItems().size() == _next_item_index)
			return null;    /// Tour is finished
		return Tour.getCurrentTour().getTourItems().get(_next_item_index);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == Utilities.REQUEST_IMAGE_CAPTURE) {
			updateDisplay();
		}
	}

}
