package alicrow.opencvtour;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class FollowTourActivity extends ActionBarActivity implements View.OnClickListener, LocationService.LocationUpdateListener {


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
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
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
	private FollowTourActivity _this = this;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((LocationService.LocationServiceBinder)service).getService();
			mBoundService.addListener(_this);

			// Tell the user about this for our demo.
			//Toast.makeText(Binding.this, R.string.local_service_connected,Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			//Toast.makeText(Binding.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
		}
	};

	boolean mIsBound = false;

	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
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
		mBoundService.removeListener(this);
		doUnbindService();
	}


	public void onLocationUpdated(Location location) {
		updateDisplay(location);
		Toast.makeText(this, "location updated", Toast.LENGTH_SHORT).show();
	}

	private void updateDisplay(Location current_location) {
		if (current_location == null) {
			Log.e("taggy-thingy", "got null current location");
			Toast.makeText(this, "Could not find location. Try again in a little bit", Toast.LENGTH_SHORT).show();
			return;
		}

		TourItem closest_item = null;
		double closest_distance = Double.MAX_VALUE;
		for(TourItem item : Tour.getCurrentTour().getTourItems()) {
			if(item.getLocation() != null && current_location.distanceTo(item.getLocation()) < closest_distance) {
				closest_item = item;
				closest_distance = current_location.distanceTo(item.getLocation());
			}
		}
		if(closest_item != null) {
			((TextView) findViewById(R.id.closest_tour_item_name)).setText("Closest tour item: " + closest_item.getName());
			((TextView) findViewById(R.id.closest_tour_item_location)).setText("Closest tour item location: " + closest_item.getLocation().getLatitude() + ", " + closest_item.getLocation().getLongitude());
			((TextView) findViewById(R.id.current_location)).setText("Current location: " + current_location.getLatitude() + ", " + current_location.getLongitude() + ", accuracy: " + current_location.getAccuracy() + " meters");
		}
	}


}
