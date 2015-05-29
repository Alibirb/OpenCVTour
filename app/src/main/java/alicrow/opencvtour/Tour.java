package alicrow.opencvtour;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by daniel on 5/26/15.
 *
 * Class representing a virtual tour.
 * Also a bit of a hack so that different Activities can access the data they need.
 */
public class Tour
{
	static Tour _currentTour;

	private ArrayList<TourItem> _tour_items;
	private boolean _gps_enabled;



	public static Tour getCurrentTour()
	{
		if(_currentTour == null)
			_currentTour = new Tour();
		return _currentTour;
	}

	public Tour()
	{
		/// Create default list of TourItems.
		_tour_items = new ArrayList<TourItem>();
		_tour_items.add(new TourItem("Acopian", "Acopian is home to the Lafayette Computer Science Department! (And some engineers, too)"));
		_tour_items.add(new TourItem("Farinon", "Farinon contains the Lafayette College bookstore, the post office, Residence Life, and Upper and Lower Farinon dining areas."));
	}

	/**
	 * Saves this Tour to a Map so we can export it.
	 * @return
	 */
	public Map<String,Object> saveToMap() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("gps_enabled", _gps_enabled);

		ArrayList<Map<String, Object>> item_maps = new ArrayList<Map<String,Object>>();
		for(TourItem item : _tour_items)
			item_maps.add(item.saveToMap());
		data.put("items", item_maps);

		return data;
	}
	public void loadFromMap(Map<String,Object> data) {

		setGpsEnabled((Boolean) data.get("gps_enabled"));

		_tour_items.clear();
		for(Map<String,Object> map : (ArrayList<Map<String,Object>>) data.get("items"))
			_tour_items.add(new TourItem(map));

	}



	public ArrayList<TourItem> getTourItems() {
		return _tour_items;
	}

	public void setGpsEnabled(boolean enabled) {
		_gps_enabled = enabled;
	}
	public boolean getGpsEnabled() {
		return _gps_enabled;
	}

}
