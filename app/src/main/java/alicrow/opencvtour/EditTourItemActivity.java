package alicrow.opencvtour;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.os.Bundle;
import android.app.ActionBar;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/*
 * Activity to edit an item in a tour.
 */
public class EditTourItemActivity extends Activity implements View.OnClickListener, AbsListView.MultiChoiceModeListener {

	private static final String TAG = "EditTourItemActivity";

	public static int EDIT_TOUR_ITEM_REQUEST = 0x0002;
	public static int REQUEST_IMAGE_CAPTURE = 1;


	short _position_in_tour;    /// Position of this TourItem in the Tour.
	TourItem _tour_item;

	File _photoFile;

	private GridView _gridview;
	private Menu _context_menu;
	private ArrayList<String> _images_selected = new ArrayList<>();



	private LocationService mBoundService;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((LocationService.LocationServiceBinder)service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
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

		// create a new ImageView for each item referenced by the Adapter
		public View getView(final int position, View convertView, final ViewGroup parent) {
			final ImageView image_view;
			if (convertView == null) {
				// if it's not recycled, initialize some attributes
				image_view = new ImageView(_context) {
					@Override public int[] onCreateDrawableState(int extraSpace) {
						final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
						if (((AbsListView)parent).isItemChecked(position)) {
							mergeDrawableStates(drawableState, CHECKED_STATE_SET);
						}
						return drawableState;
					}
				};
				image_view.setBackgroundResource(R.drawable.item_selection_background);
				image_view.setScaleType(ImageView.ScaleType.CENTER_CROP);
				image_view.setPadding(8, 8, 8, 8);
			} else {
				image_view = (ImageView) convertView;
			}

			/// check the cache to see if we've already computed this bitmap.
			if(!_bitmaps.containsKey(_item.getImageFilenames().get(position))) {
				_bitmaps.put(_item.getImageFilenames().get(position), Utilities.decodeSampledBitmap(_item.getImageFilenames().get(position), 128, 128));
			}
			image_view.setImageBitmap(_bitmaps.get(_item.getImageFilenames().get(position)));
			image_view.setLayoutParams(new GridView.LayoutParams(_bitmaps.get(_item.getImageFilenames().get(position)).getWidth(), _bitmaps.get(_item.getImageFilenames().get(position)).getHeight()));

			return image_view;
		}
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if(checked)
			_images_selected.add(_tour_item.getImageFilenames().get(position));
		else
			_images_selected.remove(_tour_item.getImageFilenames().get(position));

		/// If only a single image is selected, the user can set that as the main image
		if(_images_selected.size() == 1)
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(true);
		else
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(false);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// Respond to clicks on the actions in the CAB
		switch (item.getItemId()) {
			case R.id.menu_delete: {
				/// Remove the selected images from our list.
				for(String image : _images_selected) {
					_tour_item.removeImage(image);
				}
				mode.finish(); // Action picked, so close the CAB
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
		// Inflate the menu for the CAB
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.context_menu_edit_tour_item, menu);
		_context_menu = menu;
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		// Make any necessary updates to the activity when the CAB is removed
		_images_selected.clear();
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// Perform updates to the CAB due to an invalidate() request
		// This gets called when the screen orientation changes, so we can use it to adjust the layout

		_context_menu = menu;   /// Store a reference to the context menu for later usage

		if(_images_selected.size() == 1)
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(true);
		else
			_context_menu.findItem(R.id.menu_set_as_main_image).setVisible(false);

		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/// custom action bar
		final LayoutInflater inflater = getLayoutInflater();
		final View actionBarView = inflater.inflate(R.layout.actionbar_custom_view_done, null);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(actionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		actionBarView.findViewById(R.id.actionbar_done).setOnClickListener(this);

		setContentView(R.layout.activity_edit_tour_item);

		/// Find the TourItem we're editing, and display its current values.
		Bundle bundle = getIntent().getExtras();
		_position_in_tour =  bundle.getShort("position");

		_tour_item = Tour.getCurrentTour().getTourItems().get(_position_in_tour);

		_gridview = (GridView) findViewById(R.id.gridview);
		_gridview.setAdapter(new TourItemImageAdapter(this, _tour_item));
		_gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		_gridview.setMultiChoiceModeListener(this);
		_gridview.setDrawSelectorOnTop(true);

		((EditText) findViewById(R.id.edit_tour_item_name)).setText(_tour_item.getName());
		((EditText) findViewById(R.id.edit_tour_item_description)).setText(_tour_item.getDescription());
		/// TODO: handle audio

		if(_tour_item.getLocation() != null) {
			Log.i(TAG, "loading GPS coordinates...");
			((TextView) findViewById(R.id.tour_item_location)).setText("location: " + _tour_item.getLocation().getLatitude() + ", " + _tour_item.getLocation().getLongitude());
		}

		findViewById(R.id.image_picker).setOnClickListener(this);
		findViewById(R.id.get_current_gps_location).setOnClickListener(this);

		doBindService();


		if(savedInstanceState != null) {
			if (savedInstanceState.containsKey("_photoFile_uri")) {
				Log.i(TAG, "attempting to create _photoFile from URI in savedInstanceState");
				_photoFile = new File(URI.create(savedInstanceState.getString("_photoFile_uri")));
			}
			if(savedInstanceState.containsKey("_images_selected")) {
				_images_selected = savedInstanceState.getStringArrayList("_images_selected");
			}
		}

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_edit_tour_item, menu);
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

	public void applyChanges()
	{
		/// Apply changes and return to previous Activity.
		_tour_item.setName(((EditText) findViewById(R.id.edit_tour_item_name)).getText().toString());
		_tour_item.setDescription( ((EditText) findViewById(R.id.edit_tour_item_description)).getText().toString());
		/// TODO: handle audio and image

		/// finish this Activity
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
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
					// Create the File where the photo should go
					_photoFile = null;
					try {
						_photoFile = createImageFile();
					} catch (IOException ex) {
						// Error occurred while creating the File
						Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
						Log.e(TAG, ex.toString());
					}
					// Continue only if the File was successfully created
					if (_photoFile != null) {
						takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(_photoFile));
						startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
					} else {
						Log.e(TAG, "_photoFile is null");
					}
				}
				break;

			case R.id.get_current_gps_location:
			{
				Location location = mBoundService.getCurrentLocation();
				if (location == null) {
					Log.e(TAG, "got null current location");
					Toast.makeText(this, "Could not determine location. Make sure you have location enabled on your device, and/or wait a few seconds and try again.", Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
					_tour_item.setLocation(location);
					((TextView) findViewById(R.id.tour_item_location)).setText("location: " + _tour_item.getLocation().getLatitude() + ", " + _tour_item.getLocation().getLongitude());
				}
				break;
			}
			case R.id.actionbar_done:
				applyChanges();
		}
	}


	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		return File.createTempFile(
				imageFileName,  /* prefix */
				".jpg",         /* suffix */
				Tour.getTourDirectory()      /* directory */
		);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == Activity.RESULT_OK && requestCode == EditTourItemActivity.REQUEST_IMAGE_CAPTURE)
		{
			_tour_item.addImage(_photoFile.toString());

			if(_photoFile == null)
				Log.e(TAG, "_photoFile is null");

			Log.i(TAG, "there should be a picture called " + _photoFile.toURI().toString());
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		Log.i(TAG, "onSaveInstanceState called");

		if(_photoFile != null)
			outState.putString("_photoFile_uri", _photoFile.toURI().toString());

		/// save which items are checked.
		outState.putStringArrayList("_images_selected", _images_selected);
	}


}
