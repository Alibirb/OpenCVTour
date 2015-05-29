package alicrow.opencvtour;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by daniel on 5/28/15.
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
	GoogleApiClient _google_api_client;
	boolean _requesting_location_updates;
	LocationRequest _location_request;
	Location _current_location;
	ArrayList<LocationUpdateListener> _listeners;


	public void setupGoogleLocationServices() {
		_google_api_client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();

	}

	protected void createLocationRequest() {
		_location_request = new LocationRequest();
		_location_request.setInterval(10000);
		_location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	public Location getCurrentLocation() {
		//return LocationServices.FusedLocationApi.getLastLocation(_google_api_client);
		return _current_location;
	}

	@Override
	public void onCreate() {
		setupGoogleLocationServices();
		createLocationRequest();
		_requesting_location_updates = true;
		Log.i("taggy-thingy", "LocationService created");
		_google_api_client.connect();
		_listeners = new ArrayList<>();
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// Connected to Google Play services!
		// The good stuff goes here.
		Log.i("taggy-thingy", "connected to Google Play services");
		if(_requesting_location_updates)
			LocationServices.FusedLocationApi.requestLocationUpdates(_google_api_client, _location_request, this);

	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection has been interrupted.
		// Disable any UI components that depend on Google APIs
		// until onConnected() is called.
		Log.i("taggy-thingy", "connection to Google Play services has been suspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// This callback is important for handling errors that
		// may occur while attempting to connect with Google.
		//
		// More about this in the next section.
		Log.i("taggy-thingy", "connection to Google Play services has failed");
	}

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocationServiceBinder extends Binder {
		LocationService getService() {
			return LocationService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder _binder = new LocationServiceBinder();


	@Override
	public void onLocationChanged(Location location) {
		Log.i("taggy-thingy", "recieved location update");
		_current_location = location;
		for(LocationUpdateListener l : _listeners)
			l.onLocationUpdated(location);
	}

	public void addListener(LocationUpdateListener l) {
		_listeners.add(l);
	}
	public void removeListener(LocationUpdateListener l) {
		_listeners.remove(l);
	}


	/**
	 * Interface for other classes that want to know when the Location is updated.
	 */
	public interface LocationUpdateListener {
		void onLocationUpdated(Location location);
	}



}


