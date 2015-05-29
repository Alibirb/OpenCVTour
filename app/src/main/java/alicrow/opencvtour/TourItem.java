package alicrow.opencvtour;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by daniel on 5/25/15.
 *
 * Represents an item on a tour.
 */
public class TourItem
{
	String _name;
	String _description;
	File _image_file;       /// FIXME: I don't know if it's working.
	Bitmap _thumbnail;     /// FIXME: not working.
	/// TODO: audio file
	/// TODO: GPS coordinates
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
	public TourItem(String name, String description)
	{
		_name = name;
		_description = description;
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
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("name", _name);
		data.put("description", _description);
		/// TODO: image file, thumbnail, et cetera.


		/// Location contains a bunch of information we're not interested in (e.g. timestamp, speed), so we're just going to export the information that's actually useful to us.
		Map<String, Object> gps_data = new HashMap<String, Object>();
		if(_location.hasAccuracy())
			gps_data.put("accuracy", _location.getAccuracy());
		if(_location.hasAltitude())
			gps_data.put("altitude", _location.getAltitude());
		/// bearing? Could be useful. Tells us where the user is facing.
		gps_data.put("latitude", _location.getLatitude());
		gps_data.put("longitude", _location.getLongitude());

		data.put("location", gps_data);

		return data;
	}
	public void loadFromMap(Map<String,Object> data) {
		setName((String) data.get("name"));
		setDescription((String) data.get("description"));

		if(data.containsKey("location")) {
			Map<String, Object> gps_data = (Map<String, Object>) data.get("location");

			setLocation(gps_data);

			/*_location = new Location("saved");
			if(gps_data.containsKey("accuracy"))
				_location.setAccuracy((Float) gps_data.get("accuracy"));
			if(gps_data.containsKey("altitude"))
				_location.setAltitude((Double) gps_data.get("altitude"));
			if(gps_data.containsKey("latitude"))
				_location.setLatitude((Double) gps_data.get("latitude"));
			if(gps_data.containsKey("longitude"))
				_location.setLongitude((Double) gps_data.get("longitude"));
				*/
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

	public Bitmap getThumbnail() {
		return _thumbnail;
	}
	public void setThumbnail(Bitmap thumb) {
		_thumbnail = thumb;
	}

	public File getImageFile() {
		return _image_file;
	}
	public void setImageFile(File image_file) {
		_image_file = image_file;
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
