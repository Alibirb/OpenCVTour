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
	private static File _imported_tours_directory;  /// directory of tours that we imported from an archive. These can't be edited.

	private ArrayList<TourItem> _tour_items;
	private boolean _gps_enabled;
	private boolean _enforce_order;   /// indicates if the TourItems must be visited in sequence
	private String _name;
	private ImageDetector _detector = new ImageDetector();
	private File _directory;    /// directory we save this tour in
	private boolean _editable = true;
	private double _item_range; /// if GPS is enabled, we'll only check items within _item_range meters of the current location.

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

	/**
	 * Loads the tours from disk
	 * @param context a context, necessary in order to retrieve the directory for the app's data.
	 */
	private static void loadTours(Context context) {
		_tours = new ArrayList<>();

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

		/// Load imported tours. These can't be edited, since we don't have the image files for them (just the image descriptors).
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


	public Tour() {
		_tour_items = new ArrayList<>();
		_name = "Unnamed tour";
	}
	public Tour(File file) {
		_directory = file.getParentFile();
		_tour_items = new ArrayList<>();
		loadFromFile(file);
	}

	/// Return a File object representing the directory this Tour is stored in.
	public File getDirectory() {
		if(_directory != null)
			return _directory;
		else
			return new File(_tours_directory, _name);
	}

	/**
	 * Saves this Tour to a Map so we can save it to disk or export it.
	 * @return a Map containing the data from this Tour that we want to save
	 */
	public Map<String,Object> saveToMap() {
		Map<String, Object> data = new HashMap<>();
		data.put("gps_enabled", _gps_enabled);
		data.put("name", _name);
		data.put("enforce_order", _enforce_order);
		data.put("item_range", _item_range);

		ArrayList<Map<String, Object>> item_maps = new ArrayList<>();
		for(TourItem item : _tour_items)
			item_maps.add(item.saveToMap());
		data.put("items", item_maps);

		return data;
	}

	/**
	 * Load a Map containing the tour's data
	 * @param data Map containing the tour's data.
	 */
	public void loadFromMap(Map<String,Object> data) {
		setGpsEnabled((Boolean) data.get("gps_enabled"));
		setEnforceOrder((Boolean) data.get("enforce_order"));
		setName((String) data.get("name"));
		if(data.containsKey("item_range") && data.get("item_range") != null)
			setItemRange((Double) data.get("item_range"));
		else
			setItemRange(50);

		_tour_items.clear();
		for(Map<String,Object> map : (ArrayList<Map<String,Object>>) data.get("items")) {
			try {
				_tour_items.add(new TourItem(this, map));
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}

	/**
	 * Saves the Tour to disk.
	 */
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
	/// Loads the tour from the given file. The file should be the "tour.yaml" file in the tour's folder.
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

	public double getItemRange() {
		return _item_range;
	}
	public void setItemRange(double	distance) {
		_item_range = distance;
	}

}
