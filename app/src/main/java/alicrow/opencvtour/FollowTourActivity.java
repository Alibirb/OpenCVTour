package alicrow.opencvtour;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eyeem.recyclerviewtools.adapter.WrapAdapter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to follow a Tour.
 */
public class FollowTourActivity extends AppCompatActivity implements View.OnClickListener {
	private static final String TAG = "FollowTourActivity";

	private LocationService.ServiceConnection _connection;
	private boolean _service_is_bound = false;
	private ArrayList<Integer> _visited_item_ids;
	private ArrayList<TourItem> _remaining_items;
	private TourItem _current_item;
	private Location _current_location;

	private MediaPlayer _player = null;

	private Uri _photo_uri;

	private TourItemAdapter _adapter;

	class TourItemAdapter extends RecyclerView.Adapter<TourItemAdapter.ViewHolder> {
		final List<TourItem> _tour_items;

		public class ViewHolder extends RecyclerView.ViewHolder {
			public final TextView _item_name;
			public final TextView _item_directions;

			public ViewHolder(RelativeLayout v) {
				super(v);

				_item_name = (TextView) v.findViewById(R.id.item_name);
				_item_directions = (TextView) v.findViewById(R.id.item_directions);
			}
		}

		public TourItemAdapter(List<TourItem> tour_items) {
			_tour_items = tour_items;
		}

		@Override
		public TourItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.follow_tour_item_line, parent, false);

			return new ViewHolder((RelativeLayout) v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			TourItem item = _tour_items.get(position);

			holder._item_name.setText(item.getName());
			holder._item_directions.setText(item.getDirections());
			if(item.getDirections().equals(""))
				holder._item_directions.setVisibility(View.GONE);
			else
				holder._item_directions.setVisibility(View.VISIBLE);
		}

		@Override
		public int getItemCount() {
			return _tour_items.size();
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getIntent().getData() != null) {
			/// FollowTourActivity was launched directly, with a Tour archive. Must initialize OpenCV and load the Tour.

			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
				@Override
				public void onManagerConnected(int status) {
					switch (status) {
						case LoaderCallbackInterface.SUCCESS: {
							Log.i(TAG, "OpenCV loaded successfully");

							Uri data = getIntent().getData();
							String scheme = data.getScheme();
							File extracted_folder = null;
							if(ContentResolver.SCHEME_CONTENT.equals(scheme)) {
								/// content uri
								try {
									/// Try to retrieve the filename so we know what to name our extracted folder.
									String folder_name;
									Cursor cursor = getContentResolver().query(data, new String[]{"_display_name"}, null, null, null);
									cursor.moveToFirst();
									int nameIndex = cursor.getColumnIndex("_display_name");
									if (nameIndex >= 0) {
										folder_name = cursor.getString(nameIndex);
									} else {
										folder_name = "imported-folder.zip.tour";
									}
									cursor.close();

									folder_name = folder_name.substring(0, folder_name.length() - ".zip.tour".length());
									extracted_folder = new File(Tour.getImportedToursDirectory(FollowTourActivity.this), folder_name);
									Utilities.extractFolder(getContentResolver().openInputStream(data), extracted_folder.getPath());
								} catch(FileNotFoundException ex) {
									Log.e(TAG, ex.getMessage());
									/// Import failed. Exit the activity.
									FollowTourActivity.this.finish();
								}
							} else {
								/// regular file uri
								String filepath = data.getPath();
								String folder_name = data.getLastPathSegment();
								folder_name = folder_name.substring(0, folder_name.length() - ".zip.tour".length());
								extracted_folder = new File(Tour.getImportedToursDirectory(FollowTourActivity.this), folder_name);
								Utilities.extractFolder(filepath, extracted_folder.getPath());
							}
							Tour.setSelectedTour(new Tour(new File(extracted_folder, "tour.yaml")));
							load(savedInstanceState);
							break;
						}
						default: {
							super.onManagerConnected(status);
							break;
						}
					}
				}
			});
		} else
			load(savedInstanceState);
	}


	private void load(Bundle savedInstanceState) {
		setContentView(R.layout.activity_follow_tour);

		RecyclerView recycler_view = (RecyclerView) findViewById(R.id.remaining_items_list);
		recycler_view.setLayoutManager(new LinearLayoutManager(this));

		findViewById(R.id.fab).setOnClickListener(this);
		findViewById(R.id.exit_button).setOnClickListener(this);
		findViewById(R.id.restart_button).setOnClickListener(this);

		if(Tour.getCurrentTour().getGpsEnabled())
			bindLocationService();

		_visited_item_ids = new ArrayList<>();

		if(savedInstanceState != null) {
			if (savedInstanceState.containsKey("_photo_uri")) {
				_photo_uri = Uri.parse(savedInstanceState.getString("_photo_uri"));
			}
			if(savedInstanceState.containsKey("_current_location")) {
				_current_location = savedInstanceState.getParcelable("_current_location");
			}
			if(savedInstanceState.containsKey("_visited_item_ids")) {
				_visited_item_ids = savedInstanceState.getIntegerArrayList("_visited_item_ids");
			}
		}

		_remaining_items = new ArrayList<>();
		for(TourItem item : Tour.getCurrentTour().getTourItems()) {
			if(!_visited_item_ids.contains((int) item.getId()))
				_remaining_items.add(item);
		}

		_adapter = new TourItemAdapter(_remaining_items);
		WrapAdapter wrap_adapter = new WrapAdapter(_adapter);
		wrap_adapter.addFooter(getLayoutInflater().inflate(R.layout.empty_list_footer, recycler_view, false));
		recycler_view.setAdapter(wrap_adapter);

		if(savedInstanceState != null && savedInstanceState.containsKey("current_item_id")) {
			setCurrentItem(Tour.getCurrentTour().getTourItem(savedInstanceState.getLong("current_item_id")));
		}

		updateDisplay();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.fab:
				_photo_uri = Utilities.takePicture(this, true);
				break;
			case R.id.exit_button:
				setResult(RESULT_OK);
				finish();
				break;
			case R.id.restart_button:
				Intent intent = getIntent();
				finish();
				startActivity(intent);
				break;
		}
	}

	private void bindLocationService() {
		_connection = new LocationService.ServiceConnection();
		Intent intent = new Intent(getApplicationContext(), LocationService.class);
		startService(intent);
		bindService(intent, _connection, Context.BIND_AUTO_CREATE);
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
		Log.d(TAG, "onDestroy called");
		unbindLocationService();

		if(!isChangingConfigurations()) {
			stopService(new Intent(getApplicationContext(), LocationService.class));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume called");
		if(_connection != null && _connection.getService() != null)
			_connection.getService().startLocationUpdates();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop called");
		if(!isChangingConfigurations())
			if(_connection != null && _connection.getService() != null)
				_connection.getService().stopLocationUpdates();

		if(_player != null)
			stopAudio();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState called");

		if(_photo_uri != null)
			outState.putString("_photo_uri", _photo_uri.toString());

		if(_connection != null && _connection.getService() != null)
			_current_location = _connection.getService().getCurrentLocation();

		if(_current_location != null)
			outState.putParcelable("_current_location", _current_location);
		else
			Log.d(TAG, "_current_location is null");

		outState.putIntegerArrayList("_visited_item_ids", _visited_item_ids);

		if(_current_item != null)
			outState.putLong("current_item_id", _current_item.getId());
	}

	private void identifyItem() {
		Tour current_tour = Tour.getCurrentTour();

		List<TourItem> filtered_items = current_tour.getTourItems();

		if(current_tour.getGpsEnabled()) {
			if(_connection.getService() != null)
				_current_location = _connection.getService().getCurrentLocation();
			if (_current_location == null) {
				Log.e(TAG, "got null current location");
				Toast.makeText(this, "Couldn't determine location. Ensure location is enabled on your device, and/or wait a few seconds and try again.", Toast.LENGTH_LONG).show();
				return;
			}

			filtered_items = filterDistantTourItems(filtered_items, _current_location, current_tour.getItemRange());
		} else
			_current_location = null;

		List<Long> filtered_item_ids = new ArrayList<>();
		for(TourItem item : filtered_items) {
			filtered_item_ids.add(item.getId());
		}

		TourItem detected_item = current_tour.getTourItem(current_tour.getDetector().identifyObject(_photo_uri.getPath(), filtered_item_ids));

		if(detected_item != null) {
			Log.i(TAG, "detected item named " + detected_item.getName());
			if(_current_location != null)
				Toast.makeText(this, "distance is " + _current_location.distanceTo(detected_item.getLocation()) + " meters", Toast.LENGTH_LONG).show();
		} else {
			Log.i(TAG, "got null TourItem");
			Toast.makeText(this, "No tour item detected.", Toast.LENGTH_SHORT).show();
		}

		if(detected_item != null && (!current_tour.getEnforceOrder() || getNextTourItem() == detected_item)) {
			setCurrentItem(detected_item);
			if(detected_item.hasAudioFile())
				playAudio();
		}
	}

	private void playAudio() {
		_player = new MediaPlayer();
		try {
			_player.setDataSource(_current_item.getAudioFilepath());
			_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					mp.start();
				}
			});
			_player.prepareAsync();
		} catch (IOException e) {
			Log.e(TAG, "prepare() failed");
		}
	}

	private void stopAudio() {
		_player.release();
		_player = null;
	}

	/// Filters out any TourItems not within threshold meters of current_location
	private static List<TourItem> filterDistantTourItems(List<TourItem> items, Location current_location, double threshold) {
		List<TourItem> nearby_items = new ArrayList<>();
		for(TourItem item : items) {
			/// if the item is close enough, or doesn't have a location set, we add it to the list
			if(item.getLocation() == null || current_location.distanceTo(item.getLocation()) < threshold) {
				nearby_items.add(item);
			}

			if(item.getLocation() != null)
				Log.d(TAG, item.getName() + " is " + current_location.distanceTo(item.getLocation()) + " meters away");
		}
		return nearby_items;
	}

	private void setCurrentItem(TourItem item) {
		_current_item = item;

		findViewById(R.id.current_item_container).setVisibility(View.VISIBLE);

		((TextView) findViewById(R.id.current_tour_item_name)).setText(item.getName());
		((TextView) findViewById(R.id.current_tour_item_description)).setText(item.getDescription());

		if(!_visited_item_ids.contains((int) item.getId()))
			_visited_item_ids.add((int) item.getId());

		_remaining_items.remove(item);
		_adapter.notifyDataSetChanged();

		updateDisplay();
	}

	private void updateDisplay() {
		if(Tour.getCurrentTour().getEnforceOrder()) {
			findViewById(R.id.remaining_items_header).setVisibility(View.GONE);
			findViewById(R.id.remaining_items_list).setVisibility(View.GONE);
			findViewById(R.id.directions).setVisibility(View.VISIBLE);
			if(getNextTourItem() == null) {
				/// End of tour.
				findViewById(R.id.next_item_container).setVisibility(View.GONE);
				findViewById(R.id.fab).setVisibility(View.GONE);
				findViewById(R.id.tour_complete_container).setVisibility(View.VISIBLE);
			} else {
				if(!getNextTourItem().getDirections().equals("")) {
					((TextView) findViewById(R.id.directions)).setText(getNextTourItem().getDirections());
				} else {
					findViewById(R.id.directions).setVisibility(View.INVISIBLE);
				}
				((TextView) findViewById(R.id.next_item_name)).setText(getNextTourItem().getName());
			}
		} else {
			findViewById(R.id.next_item_container).setVisibility(View.GONE);
			if(_remaining_items.isEmpty()) {
				/// End of tour.
				findViewById(R.id.remaining_items_header).setVisibility(View.GONE);
				findViewById(R.id.remaining_items_list).setVisibility(View.GONE);
				findViewById(R.id.fab).setVisibility(View.GONE);
				findViewById(R.id.tour_complete_container).setVisibility(View.VISIBLE);
			}
		}
	}

	private TourItem getNextTourItem() {
		int index;
		if(_current_item == null)
			index = 0;
		else
			index = Tour.getCurrentTour().getTourItems().indexOf(_current_item) + 1;

		if(Tour.getCurrentTour().getTourItems().size() == index)
			return null;    /// Tour is finished
		return Tour.getCurrentTour().getTourItems().get(index);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == Utilities.REQUEST_IMAGE_CAPTURE) {
			identifyItem();
		}
	}

}
