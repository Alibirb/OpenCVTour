package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.Fragment;
import android.os.Bundle;
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
public class EditTourItemListActivityFragment extends Fragment implements View.OnClickListener {

	private ListView _list_view;

	//private ArrayList<TourItem> _tour_items;
	private Tour _tour;

	/// Create a message handling object as an anonymous class.
	private AdapterView.OnItemClickListener messageClickedHandler = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position, long id)
		{
			// Switch to TourItem-editing mode.
			Intent intent = new Intent(getActivity(), EditTourItemActivity.class);
			Bundle bundle = new Bundle();
			bundle.putShort("position", (short) position);

			//Toast.makeText(MainActivity.this, Short.toString(bundle.getShort("position")), Toast.LENGTH_SHORT).show();

			intent.putExtras(bundle);
			startActivityForResult(intent, EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST);

			/*
			TODO: see http://developer.android.com/guide/components/fragments.html and use fragments that way.
			 */
		}
	};


	private View.OnClickListener deleteButtonClickedListener = new View.OnClickListener() {
		@Override
		public void onClick(View v)
		{
			final int position = _list_view.getPositionForView(v);
			if (position != ListView.INVALID_POSITION) {
				/// Delete that TourItem
				((TourItemArrayAdapter) _list_view.getAdapter()).remove(((TourItemArrayAdapter) _list_view.getAdapter()).getItem(position));
			}

		}
	};


	public class TourItemArrayAdapter extends ArrayAdapter<TourItem>
	{
		private final Context _context;
		private final List<TourItem> _items;

		final int INVALID_ID = -1;

		public TourItemArrayAdapter(Context context, List<TourItem> items)
		{
			super(context, -1, items);
			_context = context;
			_items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			TourItem item = _items.get(position);

			LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View row_view = inflater.inflate(R.layout.tour_item_line, parent, false);

			((TextView) row_view.findViewById(R.id.tour_item_name)).setText(item.getName());
			((TextView) row_view.findViewById(R.id.tour_item_description)).setText(item.getDescription());
			if(item.getImageFilename() != null)
				((ImageView) row_view.findViewById(R.id.tour_item_thumbnail)).setImageBitmap(Utilities.decodeSampledBitmap(item.getImageFilename(), 64, 64));
			/// Todo: audio support

			/// Set up event listeners for the item's buttons
			row_view.findViewById(R.id.delete_tour_item).setOnClickListener(deleteButtonClickedListener);

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
		View v = inflater.inflate(R.layout.fragment_edit_tour_item_list, container, false);

		v.findViewById(R.id.add_tour_item).setOnClickListener(this);

		return v;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_tour = Tour.getCurrentTour();

		_list_view = (ListView) getActivity().findViewById(R.id.list);

		ArrayList<TourItem> _tour_items = _tour.getTourItems();

		/// Set up _list_view to show the items in our list.
		TourItemArrayAdapter adapter = new TourItemArrayAdapter(getActivity(), _tour_items);
		_list_view.setAdapter(adapter);

		_list_view.setOnItemClickListener(messageClickedHandler);
	}


	@Override
	public void onClick(View v)
	{
		switch(v.getId()) {
			case R.id.add_tour_item:
				addNewTourItem(v);
				break;
		}
	}

	/**
	 * Adds a new TourItem, and launches EditTourItemActivity for it.
	 * @param view
	 */
	public void addNewTourItem(View view)
	{
		/// Add empty TourItem to the Tour
		_tour.getTourItems().add(new TourItem());
		((TourItemArrayAdapter) _list_view.getAdapter()).notifyDataSetChanged();

		/// Switch to TourItem-editing mode.
		Intent intent = new Intent(getActivity(), EditTourItemActivity.class);
		Bundle bundle = new Bundle();
		bundle.putShort("position", (short) (_tour.getTourItems().size() - 1));
		intent.putExtras(bundle);
		startActivityForResult(intent, EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		/// Update our ListView if the EditTourItemActivity finished successfully.
		if(resultCode == Activity.RESULT_OK && requestCode == EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST) {
			((TourItemArrayAdapter) _list_view.getAdapter()).notifyDataSetChanged();
		}
	}


}
