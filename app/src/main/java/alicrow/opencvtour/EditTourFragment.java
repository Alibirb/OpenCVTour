package alicrow.opencvtour;

import android.content.Intent;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;


/**
 * Fragment to edit Tour settings.
 */
public class EditTourFragment extends Fragment implements View.OnClickListener {

	private static final String TAG = "EditTourFragment";


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_edit_tour, container, false);

		v.findViewById(R.id.tour_items).setOnClickListener(this);
		v.findViewById(R.id.enable_gps).setOnClickListener(this);
		v.findViewById(R.id.save_tour).setOnClickListener(this);
		v.findViewById(R.id.follow_tour).setOnClickListener(this);

		((CheckBox) v.findViewById(R.id.enable_gps)).setChecked(Tour.getCurrentTour().getGpsEnabled());
		((TextView) v.findViewById(R.id.tour_name)).setText(Tour.getCurrentTour().getName());

		return v;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.tour_items:
				startActivity(new Intent(getActivity(), TourItemListActivity.class));
				break;

			case R.id.enable_gps:
				Tour.getCurrentTour().setGpsEnabled(((CheckBox) v).isChecked());
				break;

			case R.id.save_tour: {
				Tour.getCurrentTour().setName(((TextView) getActivity().findViewById(R.id.tour_name)).getText().toString());
				Tour.getCurrentTour().saveToFile();
				break;
			}
			case R.id.follow_tour: {
				startActivity(new Intent(getActivity(), FollowTourActivity.class));
				break;
			}
		}
	}

}
