package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * Fragment to display the list of TourItems in a Tour.
 */
public class TourItemListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
	private static final String TAG = "TourItemListFragment";

	private Tour _tour;
	private ListView _list_view;
	private TourItemArrayAdapter _adapter;

	/**
	 * Adapter to display TourItems in our list
	 */
	public class TourItemArrayAdapter extends ArrayAdapter<TourItem>
	{
		private final Context _context;
		private final List<TourItem> _items;

		final int INVALID_ID = -1;

		public TourItemArrayAdapter(Context context, List<TourItem> items) {
			super(context, -1, items);
			_context = context;
			_items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TourItem item = _items.get(position);

			/// Create row_view, or recycle an existing view if possible
			View row_view;
			if(convertView == null) {
				Log.v(TAG, "creating new view for position " + position);
				LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row_view = inflater.inflate(R.layout.tour_item_line, parent, false);
			} else {
				Log.v(TAG, "recycling existing view for position " + position);
				row_view = convertView;
			}

			((TextView) row_view.findViewById(R.id.tour_item_name)).setText(item.getName());
			((TextView) row_view.findViewById(R.id.tour_item_description)).setText(item.getDescription());
			if(item.getDescription().equals(""))
				row_view.findViewById(R.id.tour_item_description).setVisibility(View.GONE);
			else
				row_view.findViewById(R.id.tour_item_description).setVisibility(View.VISIBLE);

			if(item.hasMainImage()) {
				String image_filename = item.getMainImageFilename();
				ImageView image_view = (ImageView) row_view.findViewById(R.id.tour_item_thumbnail);
				Utilities.loadBitmap(image_view, image_filename, Utilities.dp_to_px(40), Utilities.dp_to_px(40));
			} else
				((ImageView) row_view.findViewById(R.id.tour_item_thumbnail)).setImageResource(R.drawable.default_thumbnail);

			row_view.findViewById(R.id.delete_tour_item).setOnClickListener(TourItemListFragment.this);

			return row_view;
		}

		@Override
		public long getItemId(int position) {
			if (position < 0 || position >= _items.size()) {
				return INVALID_ID;
			}
			return getItem(position).getId();
		}

		public List<TourItem> getList() {
			return _items;
		}

	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_tour_item_list, container, false);

		v.findViewById(R.id.add_tour_item).setOnClickListener(this);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_tour = Tour.getCurrentTour();

		_list_view = (ListView) getActivity().findViewById(R.id.list);

		ArrayList<TourItem> tour_items = _tour.getTourItems();

		/// Set up _list_view to show the items in our list.
		_adapter = new TourItemArrayAdapter(getActivity(), tour_items);
		_list_view.setAdapter(_adapter);

		_list_view.setOnItemClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.add_tour_item:
				addNewTourItem();
				break;
			case R.id.delete_tour_item: {
				final int position = _list_view.getPositionForView(v);
				if (position != ListView.INVALID_POSITION) {
					_adapter.remove(_adapter.getItem(position));
				}
			}
		}
	}

	public void onItemClick(AdapterView parent, View v, int position, long id) {
		editTourItem(position);
	}

	/**
	 * Launches EditTourItemActivity for the TourItem specified
	 * @param position_in_list position of the TourItem in the Tour's list
	 */
	private void editTourItem(int position_in_list) {
		Intent intent = new Intent(getActivity(), EditTourItemActivity.class);
		Bundle bundle = new Bundle();
		bundle.putShort("position", (short) position_in_list);
		intent.putExtras(bundle);
		startActivityForResult(intent, EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST);
	}

	private void addNewTourItem() {
		_tour.getTourItems().add(new TourItem());
		_adapter.notifyDataSetChanged();
		editTourItem(_tour.getTourItems().size() - 1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST) {
			_adapter.notifyDataSetChanged();
		}
	}


}
