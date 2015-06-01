package alicrow.opencvtour;

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
	private String _name;



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
		_name = "Unnamed tour";
	}

	/**
	 * Saves this Tour to a Map so we can export it.
	 * @return
	 */
	public Map<String,Object> saveToMap() {
		Map<String, Object> data = new HashMap<>();
		data.put("gps_enabled", _gps_enabled);
		data.put("name", _name);

		ArrayList<Map<String, Object>> item_maps = new ArrayList<>();
		for(TourItem item : _tour_items)
			item_maps.add(item.saveToMap());
		data.put("items", item_maps);

		return data;
	}
	public void loadFromMap(Map<String,Object> data) {

		setGpsEnabled((Boolean) data.get("gps_enabled"));
		setName((String) data.get("name"));

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

	public String getName() {
		return _name;
	}
	public void setName(String name) {
		_name = name;
	}

}
