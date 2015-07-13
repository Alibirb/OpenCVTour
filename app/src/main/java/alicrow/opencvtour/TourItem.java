package alicrow.opencvtour;

import android.location.Location;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by daniel on 5/25/15.
 *
 * Represents an item on a tour, e.g. the Eiffel Tower
 */
public class TourItem {
	private static final String TAG = "TourItem";

	private String _name;
	private String _description;
	private String _directions;
	private String _main_image_filepath = "";    /// filepath of the TourItem's main image, to be displayed in thumbnails
	private ArrayList<String> _image_filepaths = new ArrayList<>();
	/// TODO: audio file
	private Location _location;
	private Tour _tour;

	private final long _unique_id;
	private static long _next_id = 0;

	public TourItem(Tour tour) {
		_tour = tour;
		_name = "";
		_description = "";
		_directions = "";
		_unique_id = _next_id;
		++_next_id;
	}
	public TourItem(Tour tour, Map<String,Object> data) {
		_tour = tour;
		_unique_id = _next_id;
		++_next_id;
		loadFromMap(data);
	}

	/**
	 * Saves this TourItem into a Map so we can export it.
	 * @return a map containing all the data we want to save from this TourItem
	 */
	public Map<String,Object> saveToMap() {
		Map<String, Object> data = new HashMap<>();
		data.put("name", _name);
		data.put("description", _description);
		data.put("directions", _directions);

		if(hasMainImage())
			data.put("main_image", _main_image_filepath.substring(_main_image_filepath.lastIndexOf("/")+1));

		ArrayList<String> filenames = new ArrayList<>();
		for(String filepath : _image_filepaths) {
			File file = new File(filepath);
			filenames.add(file.getName());
		}
		data.put("images", filenames);

		if(_location != null) {
			/// Location contains a bunch of information we're not interested in (e.g. timestamp, speed), so we're just going to export the information that's actually useful to us.
			Map<String, Object> gps_data = new HashMap<>();
			if (_location.hasAccuracy())
				gps_data.put("accuracy", _location.getAccuracy());
			if (_location.hasAltitude())
				gps_data.put("altitude", _location.getAltitude());
			/// bearing? Could be useful. Tells us where the user is facing.
			gps_data.put("latitude", _location.getLatitude());
			gps_data.put("longitude", _location.getLongitude());

			data.put("location", gps_data);
		}

		return data;
	}
	public void loadFromMap(Map<String,Object> data) {
		setName((String) data.get("name"));
		setDescription((String) data.get("description"));
		setDirections((String) data.get("directions"));
		if(data.containsKey("main_image") && data.get("main_image") != null) {
			setMainImage((new File(_tour.getDirectory(), (String) data.get("main_image"))).getPath());
		} else
			_main_image_filepath = "";

		if(data.containsKey("images") && data.get("images") != null) {
			for(String image : (ArrayList<String>) data.get("images")) {
				addImageFilepath((new File(_tour.getDirectory(), image)).getPath());
			}
		}

		if(data.containsKey("location") && data.get("location") != null) {
			setLocation((Map<String, Object>) data.get("location"));
		}
	}


	public String getName() {
		return _name;
	}
	public void setName(String name) {
		_name = name;
	}

	public String getDescription() {
		return _description;
	}
	public void setDescription(String description) {
		_description = description;
		if(_description == null || _description.equals("null"))
			_description = "";
	}

	public String getDirections() {
		return _directions;
	}
	public void setDirections(String directions) {
		_directions = directions;
		if(_directions == null || _directions.equals("null"))
			_directions = "";
	}

	public long getId() {
		return _unique_id;
	}

	public String getMainImageFilepath() {
		return _main_image_filepath;
	}
	public void setMainImage(String filepath) {
		_main_image_filepath = filepath;
	}
	public boolean hasMainImage() {
		return (_main_image_filepath != null) && !_main_image_filepath.equals("");
	}

	public ArrayList<String> getImageFilepaths() {
		return _image_filepaths;
	}
	public void addImageFilepath(String filepath) {
		_image_filepaths.add(filepath);
		if(!hasMainImage()) {
			/// make this the main image if we don't have a main image set yet
			setMainImage(filepath);
		}
		_tour.getDetector().addToLibrary(filepath, _unique_id);
	}
	public void removeImage(String filepath) {
		_image_filepaths.remove(filepath);
		if(hasMainImage() && _main_image_filepath.equals(filepath)) {
			/// if we just deleted the main image, choose a new main image
			if(_image_filepaths.size() == 0)
				_main_image_filepath = "";
			else
				_main_image_filepath = _image_filepaths.get(0);
		}
		/// FIXME: Needs to remove the image from the ImageDetector
	}

	public Location getLocation() {
		return _location;
	}
	public void setLocation(Location location) {
		_location = location;
	}
	public void setLocation(Map<String, Object> gps_data) {
		_location = new Location("saved");
		if(gps_data.containsKey("accuracy"))
			_location.setAccuracy(((Double) gps_data.get("accuracy")).floatValue());
		if(gps_data.containsKey("altitude"))
			_location.setAltitude((Double) gps_data.get("altitude"));
		if(gps_data.containsKey("latitude"))
			_location.setLatitude((Double) gps_data.get("latitude"));
		if(gps_data.containsKey("longitude"))
			_location.setLongitude((Double) gps_data.get("longitude"));
	}

}
