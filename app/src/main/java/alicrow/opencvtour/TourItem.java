package alicrow.opencvtour;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by daniel on 5/25/15.
 *
 * Represents an item on a tour.
 */
public class TourItem
{
	static String TAG = "TourItem";

	String _name;
	String _description;
	String _main_image_filename = "";    /// filename of the TourItem's main image, to be displayed in thumbnails
	ArrayList<String> _image_filenames = new ArrayList<>();
	/// TODO: audio file
	Location _location;


	long _unique_id;
	static long _next_id = 0;

	public TourItem()
	{
		_name = "";
		_description = "";
		_unique_id = _next_id;
		++_next_id;
	}
	public TourItem(Map<String,Object> data) {
		_unique_id = _next_id;
		++_next_id;
		loadFromMap(data);
	}

	/**
	 * Saves this TourItem into a Map so we can export it.
	 * @return
	 */
	public Map<String,Object> saveToMap() {
		Map<String, Object> data = new HashMap<>();
		data.put("name", _name);
		data.put("description", _description);

		if(hasMainImage())
			data.put("main_image", _main_image_filename);

		data.put("images", _image_filenames);

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
		if(data.containsKey("main_image") && data.get("main_image") != null) {
			setMainImage((String) data.get("main_image"));
		} else
			_main_image_filename = "";

		if(data.containsKey("images") && data.get("images") != null) {
			for(String image : (ArrayList<String>) data.get("images")) {
				addImage(image);
			}
		}


		if(data.containsKey("location") && data.get("location") != null) {
			Map<String, Object> gps_data = (Map<String, Object>) data.get("location");
			setLocation(gps_data);
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
	}

	public long getId() {
		return _unique_id;
	}


	public String getMainImageFilename() {
		return _main_image_filename;
	}
	public void setMainImage(String filename) {
		_main_image_filename = filename;
	}
	public boolean hasMainImage() {
		return (_main_image_filename != null) && !_main_image_filename.equals("");
	}

	public ArrayList<String> getImageFilenames() {
		return _image_filenames;
	}
	public void addImage(String filename) {
		_image_filenames.add(filename);
		if(!hasMainImage()) {
			/// make this the main image if we don't have a main image set yet
			setMainImage(filename);
		}
	}
	public void removeImage(String filename) {
		_image_filenames.remove(filename);
		if(hasMainImage() && _main_image_filename.equals(filename)) {
			/// if we just deleted the main image, choose a new main image
			if(_image_filenames.size() == 0)
				_main_image_filename = "";
			else
				_main_image_filename = _image_filenames.get(0);
		}
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
