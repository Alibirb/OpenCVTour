/*
 * Copyright 2015 Lafayette College
 *
 * This file is part of OpenCVTour.
 *
 * OpenCVTour is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCVTour is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenCVTour.  If not, see <http://www.gnu.org/licenses/>.
 */

package alicrow.opencvtour;

import android.content.Intent;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;

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
		v.findViewById(R.id.enforce_order).setOnClickListener(this);
		v.findViewById(R.id.save_tour).setOnClickListener(this);
		v.findViewById(R.id.share_tour).setOnClickListener(this);
		v.findViewById(R.id.follow_tour).setOnClickListener(this);

		((CheckBox) v.findViewById(R.id.enable_gps)).setChecked(Tour.getCurrentTour().getGpsEnabled());
		((CheckBox) v.findViewById(R.id.enforce_order)).setChecked(Tour.getCurrentTour().getEnforceOrder());
		((TextView) v.findViewById(R.id.tour_name)).setText(Tour.getCurrentTour().getName());
		((TextView) v.findViewById(R.id.item_range)).setText(Double.toString(Tour.getCurrentTour().getItemRange()));

		if(Tour.getCurrentTour().getGpsEnabled())
			v.findViewById(R.id.range_selection_line).setVisibility(View.VISIBLE);
		else
			v.findViewById(R.id.range_selection_line).setVisibility(View.GONE);

		return v;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.tour_items:
				startActivity(new Intent(getActivity(), TourItemListActivity.class));
				break;

			case R.id.enable_gps:
				boolean enabled = ((CheckBox) v).isChecked();
				Tour.getCurrentTour().setGpsEnabled(enabled);
				if(enabled)
					getActivity().findViewById(R.id.range_selection_line).setVisibility(View.VISIBLE);
				else
					getActivity().findViewById(R.id.range_selection_line).setVisibility(View.GONE);
				break;

			case R.id.enforce_order:
				Tour.getCurrentTour().setEnforceOrder(((CheckBox) v).isChecked());
				break;

			case R.id.save_tour: {
				Tour.getCurrentTour().setName(((TextView) getActivity().findViewById(R.id.tour_name)).getText().toString());
				Tour.getCurrentTour().setItemRange(Double.parseDouble(((TextView) getActivity().findViewById(R.id.item_range)).getText().toString()));
				Tour.getCurrentTour().saveToFile();
				break;
			}
			case R.id.share_tour: {
				String tour_name = ((TextView) getActivity().findViewById(R.id.tour_name)).getText().toString();
				Tour.getCurrentTour().setName(tour_name);
				Tour.getCurrentTour().setItemRange(Double.parseDouble(((TextView) getActivity().findViewById(R.id.item_range)).getText().toString()));
				Tour.getCurrentTour().saveToFile();

				File archive = new File(getActivity().getExternalCacheDir(), tour_name + ".zip.tour");
				Utilities.compressFolder(Tour.getCurrentTour().getDirectory().getPath(), archive.getPath(), true);

				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("application/tour");
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(archive));
				if(intent.resolveActivity(getActivity().getPackageManager()) != null) {
					startActivity(Intent.createChooser(intent, null));
				}
				break;
			}
			case R.id.follow_tour: {
				Tour.getCurrentTour().setName(((TextView) getActivity().findViewById(R.id.tour_name)).getText().toString());
				Tour.getCurrentTour().setItemRange(Double.parseDouble(((TextView) getActivity().findViewById(R.id.item_range)).getText().toString()));
				startActivity(new Intent(getActivity(), FollowTourActivity.class));
				break;
			}
		}
	}

}
