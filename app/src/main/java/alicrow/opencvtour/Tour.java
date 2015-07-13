package alicrow.opencvtour;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.thanh.photodetector.ImageDetector;

/**
 * Created by daniel on 5/26/15.
 *
 * Class representing a virtual tour.
 */
public class Tour {
	private static final String TAG = "Tour";
	private static Tour _currentTour;
	private static ArrayList<Tour> _tours;
	private static File _tours_directory;  /// directory to save our Tours in
	private static File _imported_tours_directory;

	private ArrayList<TourItem> _tour_items;
	private boolean _gps_enabled;
	private boolean _enforce_order;   /// indicates if the TourItems must be visited in sequence
	private String _name;
	private ImageDetector _detector = new ImageDetector();
	private File _directory;
	private boolean _editable = true;

	public static Tour getCurrentTour() {
		if(_currentTour == null)
			_currentTour = new Tour();
		return _currentTour;
	}
	public static void setSelectedTour(Tour tour) {
		_currentTour = tour;
	}

	public static ArrayList<Tour> getTours(Context context) {
		if(_tours == null)
			loadTours(context);

		return _tours;
	}

	private static void loadTours(Context context) {
		_tours = new ArrayList<>();

		if(hasOldToursDirectory())
			for(File tour_file : getOldToursDirectory().listFiles()) {
				if(tour_file.isFile() && tour_file.getName().endsWith(".yaml") && !tour_file.getName().endsWith(".descriptors.yaml")) {
					/// Old way that Tours were saved. We'll load it, then save it (in the new manner), then delete the old version.
					convertFromOldFormat(tour_file, context);
				}
			}

		for(File dir : getToursDirectory(context).listFiles()) {
			if(dir.isDirectory() && !dir.getName().equals("imported")) {
				try {
					File tour_file = new File(dir, "tour.yaml");
					if(!tour_file.exists()) {
						Log.w(TAG, "no tour description file found in directory " + dir);
						continue;
					}
					Log.i(TAG, "loading tour from file '" + tour_file.toString() + "'");
					_tours.add(new Tour(tour_file));
				} catch(Exception e) {
					Log.e(TAG, e.toString());
				}
			}
		}

		/// Automatically extract any Tours in the Downloads folder, since we cannot rely on other apps (e.g. email, bluetooth transfer) to open the tour with our app.
		for(File archive : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles()) {
			if(archive.isFile() && archive.getName().endsWith(".zip.tour")) {
				String folder_name = archive.getName().substring(0, archive.getName().length() - ".zip.tour".length());
				File extracted_folder = new File(getImportedToursDirectory(context), folder_name);
				if(extracted_folder.exists()) {
					Log.d(TAG, "Already extracted '" + archive.getName() + "'.");
				} else {
					Log.i(TAG, "Extracting '" + archive.getName() + "' from Downloads folder");
					Utilities.extractFolder(archive.getPath(), extracted_folder.getPath());
				}
			}
		}

		for(File dir : getImportedToursDirectory(context).listFiles()) {
			if(dir.isDirectory()) {
				try {
					File tour_file = new File(dir, "tour.yaml");
					if(!tour_file.exists()) {
						Log.w(TAG, "no tour description file found in directory " + dir);
						continue;
					}

					Log.i(TAG, "loading tour from file '" + tour_file.toString() + "'");
					Tour tour = new Tour(tour_file);
					tour.setEditable(false);    /// imported Tours are only meant to be followed, not edited.
					_tours.add(tour);
				} catch(Exception e) {
					Log.e(TAG, e.toString());
				}
			}
		}
	}

	public static Tour addNewTour() {
		Tour tour = new Tour();
		_tours.add(tour);
		return tour;
	}

	public static File getToursDirectory(Context context) {
		Log.i(TAG, Environment.getExternalStorageState());

		if(_tours_directory == null)
			_tours_directory = context.getExternalFilesDir(null);

		return _tours_directory;
	}
	public static File getImportedToursDirectory(Context context) {
		Log.i(TAG, Environment.getExternalStorageState());

		if(_imported_tours_directory == null)
			_imported_tours_directory = new File(getToursDirectory(context), "imported");

		if(!_imported_tours_directory.exists())
			_imported_tours_directory.mkdir();

		return _imported_tours_directory;
	}
	private static File getOldToursDirectory() {
		Log.i(TAG, Environment.getExternalStorageState());

		File old_tour_directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "opencvtour");
		if(!old_tour_directory.exists())
			Log.e(TAG, "Old tour directory does not exist");

		return old_tour_directory;
	}
	private static boolean hasOldToursDirectory() {
		Log.i(TAG, Environment.getExternalStorageState());

		return (new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "opencvtour")).exists();
	}
	/// Converts a Tour saved in an old format to the new format
	private static File convertFromOldFormat(File old_tour_file, Context context) {
		Log.i(TAG, "converting '" + old_tour_file + "'");
		try {
			Yaml yaml = new Yaml();
			Map<String, Object> tour_data = (Map<String, Object>) yaml.load(new FileReader(old_tour_file));
			File new_tour_dir = new File(getToursDirectory(context), (String) tour_data.get("name"));
			new_tour_dir.mkdir();

			for(Map<String,Object> item_data : (ArrayList<Map<String,Object>>) tour_data.get("items")) {
				/// Move the images inside the new Tour directory, and save only their filenames, not the full path

				if(item_data.containsKey("main_image") && item_data.get("main_image") != null) {
					String image_path = (String) item_data.get("main_image");
					item_data.put("main_image", image_path.substring(image_path.lastIndexOf("/") + 1));
				}

				if(item_data.containsKey("images") && item_data.get("images") != null) {
					ArrayList<String> new_image_filenames = new ArrayList<>();
					for(String image_path : (ArrayList<String>) item_data.get("images")) {
						File old_image_file = new File(image_path);
						File new_image_file = new File(new_tour_dir, old_image_file.getName());
						new_image_filenames.add(old_image_file.getName());
						File old_descriptors_file = new File(image_path +".descriptors.yaml");
						old_descriptors_file.delete();
						if (old_image_file.renameTo(new_image_file))
							Log.i(TAG, "successfully moved image file " + old_image_file);
						else
							Log.e(TAG, "failed to rename file " + old_image_file);
					}
					item_data.put("images", new_image_filenames);
				}
			}
			File new_tour_file = new File(new_tour_dir, "tour.yaml");
			FileWriter writer = new FileWriter(new_tour_file);
			yaml.dump(tour_data, writer);
			writer.close();
			old_tour_file.renameTo(new File(old_tour_file.getPath() + ".old"));
			return new_tour_dir;
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			return null;
		}
	}


	public Tour() {
		_tour_items = new ArrayList<>();
		_name = "Unnamed tour";
	}
	public Tour(File file) {
		_directory = file.getParentFile();
		_tour_items = new ArrayList<>();
		loadFromFile(file);
	}

	/// Return a File for the directory this Tour is stored in.
	public File getDirectory() {
		if(_directory != null)
			return _directory;
		else
			return new File(_tours_directory, _name);
	}

	/**
	 * Saves this Tour to a Map so we can export it.
	 * @return a map containing the data from this Tour that we want to save
	 */
	public Map<String,Object> saveToMap() {
		Map<String, Object> data = new HashMap<>();
		data.put("gps_enabled", _gps_enabled);
		data.put("name", _name);
		data.put("enforce_order", _enforce_order);

		ArrayList<Map<String, Object>> item_maps = new ArrayList<>();
		for(TourItem item : _tour_items)
			item_maps.add(item.saveToMap());
		data.put("items", item_maps);

		return data;
	}
	public void loadFromMap(Map<String,Object> data) {
		setGpsEnabled((Boolean) data.get("gps_enabled"));
		setEnforceOrder((Boolean) data.get("enforce_order"));
		setName((String) data.get("name"));

		_tour_items.clear();
		for(Map<String,Object> map : (ArrayList<Map<String,Object>>) data.get("items")) {
			try {
				_tour_items.add(new TourItem(this, map));
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}

	public void saveToFile() {
		Map<String, Object> data = saveToMap();
		Yaml yaml = new Yaml();
		try {
			File dir = getDirectory();
			if(!dir.exists())
				dir.mkdir();
			File file = new File(dir, "tour.yaml");
			FileWriter writer = new FileWriter(file);
			yaml.dump(data, writer);
			writer.close();
			Log.i(TAG, "saved '" + file + "'");
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		_detector.saveImageDescriptors();
	}
	public void loadFromFile(File file) {
		try {
			Yaml yaml = new Yaml();

			Map<String, Object> data = (Map<String, Object>) yaml.load(new FileReader(file));
			loadFromMap(data);
			Log.i(TAG, "loaded '" + file + "'");
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}

	public ImageDetector getDetector() {
		return _detector;
	}

	public String toString() {
		return _name;
	}

	public ArrayList<TourItem> getTourItems() {
		return _tour_items;
	}
	public TourItem getTourItem(long id) {
		for(TourItem item : _tour_items) {
			if(item.getId() == id)
				return item;
		}
		return null;
	}
	public TourItem addNewTourItem() {
		TourItem item = new TourItem(this);
		_tour_items.add(item);
		return item;
	}

	public void setGpsEnabled(boolean enabled) {
		_gps_enabled = enabled;
	}
	public boolean getGpsEnabled() {
		return _gps_enabled;
	}

	public String getName() {
		return _name;
	}
	public void setName(String name) {
		_name = name;
	}

	public boolean getEnforceOrder() {
		return _enforce_order;
	}
	public void setEnforceOrder(boolean enforce) {
		_enforce_order = enforce;
	}

	public boolean getEditable() {
		return _editable;
	}
	public void setEditable(boolean editable) {
		_editable = editable;
	}

}
