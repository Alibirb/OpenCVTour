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

import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class TourListActivityFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

	static String TAG = "TourListActivityFragment";

	ListView _list_view;

	public TourListActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_tour_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_list_view = (ListView) getActivity().findViewById(R.id.list);

		ArrayList<Tour> _tours = Tour.getTours();

		/// Set up _list_view to show the items in our list.
		ArrayAdapter<Tour> adapter = new ArrayAdapter<>(getActivity(), R.layout.tour_line, _tours);
		_list_view.setAdapter(adapter);

		_list_view.setOnItemClickListener(this);

		getActivity().findViewById(R.id.add_tour).setOnClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView parent, View v, int position, long id)
	{
		Log.i(TAG, "position " + position);
		Tour.setSelectedTour(Tour.getTours().get(position));

		Intent intent = new Intent(getActivity(), MainActivity.class);
		Bundle bundle = new Bundle();
		bundle.putShort("position", (short) position);

		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.add_tour:
				Tour.addNewTour();
				((ArrayAdapter) _list_view.getAdapter()).notifyDataSetChanged();
				break;
			/// TODO: delete tour button
		}
	}



}
