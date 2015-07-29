/*
 * Copyright 2015 Lafayette College
 *
 * This file is part of OpenCVTour.
 *
 * OpenCVTour is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCVTour is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenCVTour.  If not, see <http://www.gnu.org/licenses/>.
 */

package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


/*
 * Activity to edit an item in a tour.
 */
public class EditTourItemActivity extends AppCompatActivity implements View.OnClickListener, AbsListView.MultiChoiceModeListener {

	private static final String TAG = "EditTourItemActivity";

	/// Request codes for Intents
	public static final int EDIT_TOUR_ITEM_REQUEST = 0x0003;
	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private TourItem _tour_item;

	private Uri _photo_uri; /// uri we told the camera app to save to. We store this so we know where to find the image after the camera app returns

	private Menu _context_menu;
	private ArrayList<String> _images_selected = new ArrayList<>();

	private MediaPlayer _player = null;
	private MediaRecorder _recorder = null;
	private boolean _recording = false;
	private boolean _playing = false;

	private LocationService.ServiceConnection _connection;
	private boolean _service_is_bound = false;

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

	/**
	 * Adapter to display the TourItem's images
	 */
	public class TourItemImageAdapter extends BaseAdapter {
		private TourItem _item;

		public TourItemImageAdapter(TourItem item) {
			_item = item;
		}

		public int getCount() {
			return _item.getImageFilepaths().size();
		}

		public Object getItem(int position) {
			return _item.getImageFilepaths().get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, final ViewGroup parent) {
			Log.v(TAG, "getView called");
			final ImageView image_view;
			final FrameLayout frame_layout;
			if (convertView == null) {
				Log.v(TAG, "creating new ImageView");
				// We don't have an existing view to convert, so we need to create a new view
				frame_layout = (FrameLayout) getLayoutInflater().inflate(R.layout.selectable_image, parent, false);
				image_view = (ImageView) frame_layout.findViewById(R.id.image);
				image_view.setScaleType(ImageView.ScaleType.CENTER_CROP);
			} else {
				/// Recycle convertView for better performance
				frame_layout = (FrameLayout) convertView;
				image_view = (ImageView) frame_layout.findViewById(R.id.image);
			}

			String image_filename = _item.getImageFilepaths().get(position);
			int column_width = ((GridView) parent).getRequestedColumnWidth() - Utilities.dp_to_px(8);    /// 8dp padding;
			Utilities.loadBitmap(image_view, image_filename, column_width, column_width, EditTourItemActivity.this);

			return frame_layout;
		}
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if(checked)
			_images_selected.add(_tour_item.getImageFilepaths().get(position));
		else
			_images_selected.remove(_tour_item.getImageFilepaths().get(position));

		/// If only a single image is selected, the user can set that as the main image, so we add that option to the contextual action bar
		if(_images_selected.size() == 1)
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(true);
		else
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(false);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		/// Called when a button is clicked on the contextual action bar

		switch (item.getItemId()) {
			case R.id.menu_delete: {
				for(String image : _images_selected) {
					_tour_item.removeImage(image);
				}
				mode.finish();
				return true;
			}
			case R.id.menu_set_as_main_image: {
				_tour_item.setMainImage(_images_selected.get(0));
				mode.finish();
				return true;
			}
			default:
				return false;
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		Log.d(TAG, "onCreateActionMode called");
		Log.d(TAG, _images_selected.size() + " images selected");
		mode.getMenuInflater().inflate(R.menu.context_menu_edit_tour_item, menu);
		_context_menu = menu;

		if(_images_selected.size() == 1)
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(true);
		else
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(false);

		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		_images_selected.clear();
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// Perform updates to the CAB due to an invalidate() request
		Log.d(TAG, "onPrepareActionMode called");
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindLocationService();
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
		if(!isChangingConfigurations())
			if(_connection != null && _connection.getService() != null)
				_connection.getService().stopLocationUpdates();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate called");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_edit_tour_item);

		/// Find the TourItem we're editing, and display its current values.
		Bundle bundle = getIntent().getExtras();
		short position_in_tour =  bundle.getShort("position");
		_tour_item = Tour.getCurrentTour().getTourItems().get(position_in_tour);

		/// GridView of images in the TourItem
		ExpandableHeightGridView gridview = (ExpandableHeightGridView) findViewById(R.id.gridview);
		gridview.setAdapter(new TourItemImageAdapter(_tour_item));
		gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		gridview.setMultiChoiceModeListener(this);
		gridview.setExpanded(true);

		((EditText) findViewById(R.id.edit_tour_item_name)).setText(_tour_item.getName());
		((EditText) findViewById(R.id.edit_tour_item_description)).setText(_tour_item.getDescription());
		((EditText) findViewById(R.id.edit_tour_item_directions)).setText(_tour_item.getDirections());

		if(_tour_item.getLocation() != null) {
			Log.i(TAG, "loading GPS coordinates...");
			((TextView) findViewById(R.id.tour_item_location)).setText("location: " + _tour_item.getLocation().getLatitude() + ", " + _tour_item.getLocation().getLongitude() + ", accuracy: " + _tour_item.getLocation().getAccuracy() + " meters");
		}

		findViewById(R.id.image_picker).setOnClickListener(this);
		findViewById(R.id.get_current_gps_location).setOnClickListener(this);
		findViewById(R.id.record_audio).setOnClickListener(this);
		findViewById(R.id.play_audio).setOnClickListener(this);

		if(_tour_item.hasAudioFile())
			findViewById(R.id.play_audio).setVisibility(View.VISIBLE);
		else
			findViewById(R.id.play_audio).setVisibility(View.INVISIBLE);

		bindLocationService();

		if(savedInstanceState != null) {
			if (savedInstanceState.containsKey("_photo_uri")) {
				_photo_uri = Uri.parse(savedInstanceState.getString("_photo_uri"));
			}
			if(savedInstanceState.containsKey("_images_selected")) {
				_images_selected = savedInstanceState.getStringArrayList("_images_selected");
			}
		}
	}

	private void applyChanges() {
		_tour_item.setName(((EditText) findViewById(R.id.edit_tour_item_name)).getText().toString());
		_tour_item.setDescription(((EditText) findViewById(R.id.edit_tour_item_description)).getText().toString());
		_tour_item.setDirections(((EditText) findViewById(R.id.edit_tour_item_directions)).getText().toString());

		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void onBackPressed() {
		applyChanges();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.image_picker:
				_photo_uri = Utilities.takePicture(this, false);
				break;

			case R.id.record_audio: {
				_recording = !_recording;
				if(_recording) {
					((TextView) v).setText("stop recording");
					startRecording();
				} else {
					((TextView) v).setText("record audio");
					stopRecording();
				}
				break;
			}
			case R.id.play_audio: {
				_playing = !_playing;
				if(_playing) {
					((TextView) v).setText("stop playing");
					startPlaying();
				} else {
					((TextView) v).setText("play audio");
					stopPlaying();
				}
				break;
			}
			case R.id.get_current_gps_location: {
				Location location = _connection.getService().getCurrentLocation();
				if (location == null) {
					Log.e(TAG, "got null current location");
					Toast.makeText(this, "Could not determine location. Make sure you have location enabled on your device, and/or wait a few seconds and try again.", Toast.LENGTH_LONG).show();
				} else {
					_tour_item.setLocation(location);
					((TextView) findViewById(R.id.tour_item_location)).setText("location: " + location.getLatitude() + ", " + location.getLongitude() + ", accuracy: " + location.getAccuracy() + " meters");
				}
				break;
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == Utilities.REQUEST_IMAGE_CAPTURE) {
			_tour_item.addImageFilepath(_photo_uri.getPath());
			Log.i(TAG, "saved photo as " + _photo_uri.toString());
			GridView gridview = (GridView) findViewById(R.id.gridview);
			((TourItemImageAdapter) gridview.getAdapter()).notifyDataSetChanged();
		}
	}

	private void startPlaying() {
		_player = new MediaPlayer();
		try {
			_player.setDataSource(_tour_item.getAudioFilepath());
			_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					((TextView) findViewById(R.id.play_audio)).setText("play audio");
					_playing = false;
					stopPlaying();
				}
			});
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
	private void stopPlaying() {
		_player.release();
		_player = null;
	}

	private void startRecording() {
		_recorder = new MediaRecorder();
		_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		_recorder.setOutputFile(_tour_item.getAudioFilepath());
		_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			_recorder.prepare();
		} catch (IOException e) {
			Log.e(TAG, "prepare() failed");
		}

		_recorder.start();
	}
	private void stopRecording() {
		_recorder.stop();
		_recorder.release();
		_recorder = null;
		findViewById(R.id.play_audio).setVisibility(View.VISIBLE);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		if(_photo_uri != null)
			outState.putString("_photo_uri", _photo_uri.toString());

		outState.putStringArrayList("_images_selected", _images_selected);
	}


}
