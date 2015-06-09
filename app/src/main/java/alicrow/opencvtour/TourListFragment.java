package alicrow.opencvtour;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/**
 * Fragment to list available Tours
 */
public class TourListFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {
	private static final String TAG = "TourListFragment";

	private ListView _list_view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_tour_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_list_view = (ListView) getActivity().findViewById(R.id.list);
		_list_view.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.tour_line, Tour.getTours()));
		_list_view.setOnItemClickListener(this);

		getActivity().findViewById(R.id.add_tour).setOnClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView parent, View v, int position, long id) {
		Tour.setSelectedTour(Tour.getTours().get(position));
		startActivity(new Intent(getActivity(), EditTourActivity.class));
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.add_tour:
				Tour.setSelectedTour(Tour.addNewTour());
				startActivityForResult(new Intent(getActivity(), EditTourActivity.class), EditTourActivity.EDIT_TOUR_REQUEST);
				break;
			/// TODO: "delete tour" button
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == EditTourActivity.EDIT_TOUR_REQUEST) {
			((ArrayAdapter) _list_view.getAdapter()).notifyDataSetChanged();
		}
	}

}
