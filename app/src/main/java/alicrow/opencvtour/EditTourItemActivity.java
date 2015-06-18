package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.app.ActionBar;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/*
 * Activity to edit an item in a tour.
 */
public class EditTourItemActivity extends Activity implements View.OnClickListener, AbsListView.MultiChoiceModeListener {

	private static final String TAG = "EditTourItemActivity";

	/// Request codes for Intents
	public static final int EDIT_TOUR_ITEM_REQUEST = 0x0003;
	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private TourItem _tour_item;

	private Uri _photo_uri; /// uri we told the camera app to save to. We store this so we know where to find the image after the camera app returns

	private Menu _context_menu;
	private ArrayList<String> _images_selected = new ArrayList<>();

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
		private Context _context;
		private TourItem _item;
		private final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

		public TourItemImageAdapter(Context c, TourItem item) {
			_context = c;
			_item = item;
		}

		public int getCount() {
			return _item.getImageFilenames().size();
		}

		public Object getItem(int position) {
			return _item.getImageFilenames().get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, final ViewGroup parent) {
			Log.v(TAG, "getView called");
			final ImageView image_view;
			if (convertView == null) {
				Log.v(TAG, "creating new ImageView");
				// We don't have an existing view to convert, so we need to create a new view
				image_view = new ImageView(_context) {
					@Override public int[] onCreateDrawableState(int extraSpace) {
						final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
						if (((AbsListView)parent).isItemChecked(position)) {
							/// display selector on the image if it's checked
							mergeDrawableStates(drawableState, CHECKED_STATE_SET);
						}
						return drawableState;
					}
				};
				image_view.setBackgroundResource(R.drawable.item_selection_background);
				image_view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				int border_size = Utilities.dp_to_px(4);
				image_view.setPadding(border_size, border_size, border_size, border_size);
			} else {
				/// Recycle convertView for better performance
				image_view = (ImageView) convertView;
			}

			String image_filename = _item.getImageFilenames().get(position);
			int column_width = ((GridView) parent).getRequestedColumnWidth();
			Utilities.loadBitmap(image_view, image_filename, column_width, column_width);

			return image_view;
		}
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if(checked)
			_images_selected.add(_tour_item.getImageFilenames().get(position));
		else
			_images_selected.remove(_tour_item.getImageFilenames().get(position));

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
		mode.getMenuInflater().inflate(R.menu.context_menu_edit_tour_item, menu);
		_context_menu = menu;
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		_images_selected.clear();
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// Perform updates to the CAB due to an invalidate() request
		// This gets called when the screen orientation changes, so we use it to fix the layout

		_context_menu = menu;

		if(_images_selected.size() == 1)
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(true);
		else
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(false);

		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindLocationService();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/// custom action bar
		final View actionBarView = getLayoutInflater().inflate(R.layout.actionbar_custom_view_done, null);
		final ActionBar actionbar = getActionBar();
		if(actionbar == null) {
			Log.e(TAG, "could not retrieve action bar");
		} else {
			actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
			actionbar.setCustomView(actionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		actionBarView.findViewById(R.id.actionbar_done).setOnClickListener(this);

		setContentView(R.layout.activity_edit_tour_item);

		/// Find the TourItem we're editing, and display its current values.
		Bundle bundle = getIntent().getExtras();
		short position_in_tour =  bundle.getShort("position");
		_tour_item = Tour.getCurrentTour().getTourItems().get(position_in_tour);

		/// GridView of images in the TourItem
		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(new TourItemImageAdapter(this, _tour_item));
		gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		gridview.setMultiChoiceModeListener(this);
		gridview.setDrawSelectorOnTop(true);

		((EditText) findViewById(R.id.edit_tour_item_name)).setText(_tour_item.getName());
		((EditText) findViewById(R.id.edit_tour_item_description)).setText(_tour_item.getDescription());
		((EditText) findViewById(R.id.edit_tour_item_directions)).setText(_tour_item.getDirections());
		/// TODO: handle audio

		if(_tour_item.getLocation() != null) {
			Log.i(TAG, "loading GPS coordinates...");
			((TextView) findViewById(R.id.tour_item_location)).setText("location: " + _tour_item.getLocation().getLatitude() + ", " + _tour_item.getLocation().getLongitude() + ", accuracy: " + _tour_item.getLocation().getAccuracy() + " meters");
		}

		findViewById(R.id.image_picker).setOnClickListener(this);
		findViewById(R.id.get_current_gps_location).setOnClickListener(this);

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_edit_tour_item, menu);
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

	private void applyChanges()
	{
		_tour_item.setName(((EditText) findViewById(R.id.edit_tour_item_name)).getText().toString());
		_tour_item.setDescription(((EditText) findViewById(R.id.edit_tour_item_description)).getText().toString());
		_tour_item.setDirections(((EditText) findViewById(R.id.edit_tour_item_directions)).getText().toString());

		Bundle data = new Bundle();
		data.putBoolean("item_edited", true);
		Intent intent = new Intent();
		intent.putExtras(data);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.image_picker:
				takePicture();
				break;

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
			case R.id.actionbar_done:
				applyChanges();
				break;
		}
	}

	private void takePicture() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (intent.resolveActivity(getPackageManager()) != null) {
			// Create a file to save the photo to
			_photo_uri = null;
			try {
				_photo_uri = createImageFile();
			} catch (IOException ex) {
				Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
				Log.e(TAG, ex.toString());
			}

			if (_photo_uri != null) {
				intent.putExtra(MediaStore.EXTRA_OUTPUT, _photo_uri);
				startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
			} else {
				Log.e(TAG, "failed to create file for image");
			}
		}
	}

	private Uri createImageFile() throws IOException {
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String image_filename = "JPEG_" + timestamp + "_";
		return Uri.fromFile(File.createTempFile(
				image_filename,  /* prefix */
				".jpg",         /* suffix */
				Tour.getTourDirectory() /* directory */
		));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == EditTourItemActivity.REQUEST_IMAGE_CAPTURE) {
			_tour_item.addImageFilename(_photo_uri.getPath());
			Log.i(TAG, "saved photo as " + _photo_uri.toString());
			GridView gridview = (GridView) findViewById(R.id.gridview);
			((TourItemImageAdapter) gridview.getAdapter()).notifyDataSetChanged();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		if(_photo_uri != null)
			outState.putString("_photo_uri", _photo_uri.toString());

		outState.putStringArrayList("_images_selected", _images_selected);
	}


}
