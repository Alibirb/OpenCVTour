package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import java.util.HashMap;


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
		private HashMap<String, Bitmap> _bitmaps;
		private final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

		public TourItemImageAdapter(Context c, TourItem item) {
			_context = c;
			_item = item;
			_bitmaps = new HashMap<>();
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
			final ImageView image_view;
			if (convertView == null) {
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
				image_view.setScaleType(ImageView.ScaleType.CENTER_CROP);
				image_view.setPadding(8, 8, 8, 8);
			} else {
				/// Recycle convertView for better performance
				image_view = (ImageView) convertView;
			}

			String image_filename = _item.getImageFilenames().get(position);

			/// compute the bitmap for this image if we haven't already done so, and add it to our cache
			if(!_bitmaps.containsKey(image_filename)) {
				_bitmaps.put(image_filename, Utilities.decodeSampledBitmap(image_filename, 128, 128));
			}
			if(_bitmaps.get(image_filename) == null)
				Log.e(TAG, "bitmap is null");

			image_view.setImageBitmap(_bitmaps.get(image_filename));
			image_view.setLayoutParams(new GridView.LayoutParams(_bitmaps.get(image_filename).getWidth(), _bitmaps.get(image_filename).getHeight()));

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
		/// TODO: handle audio

		if(_tour_item.getLocation() != null) {
			Log.i(TAG, "loading GPS coordinates...");
			((TextView) findViewById(R.id.tour_item_location)).setText("location: " + _tour_item.getLocation().getLatitude() + ", " + _tour_item.getLocation().getLongitude());
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
		_tour_item.setDescription( ((EditText) findViewById(R.id.edit_tour_item_description)).getText().toString());

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
					((TextView) findViewById(R.id.tour_item_location)).setText("location: " + location.getLatitude() + ", " + location.getLongitude());
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
