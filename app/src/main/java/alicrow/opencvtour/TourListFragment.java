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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eyeem.recyclerviewtools.adapter.WrapAdapter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.List;


/**
 * Fragment to list available Tours
 */
public class TourListFragment extends Fragment implements View.OnClickListener {
	private static final String TAG = "TourListFragment";

	private RecyclerView _recycler_view;
	private TourAdapter _adapter;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_tour_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		/// Must wait until OpenCV is initialized before loading the tours (since we load image descriptors).
		BaseLoaderCallback _loader_callback = new BaseLoaderCallback(getActivity()) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
					case LoaderCallbackInterface.SUCCESS:
					{
						Log.i(TAG, "OpenCV loaded successfully");

						/// Add footer so the floating action button doesn't cover up the list.
						_adapter = new TourAdapter(Tour.getTours(getActivity()));
						WrapAdapter wrap_adapter = new WrapAdapter(_adapter);
						wrap_adapter.addFooter(getActivity().getLayoutInflater().inflate(R.layout.empty_list_footer, _recycler_view, false));
						_recycler_view.setAdapter(wrap_adapter);
					} break;
					default:
					{
						super.onManagerConnected(status);
					} break;
				}
			}
		};

		_recycler_view = (RecyclerView) getActivity().findViewById(R.id.recycler_view);
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, getActivity(), _loader_callback);
		_recycler_view.setLayoutManager(new LinearLayoutManager(getActivity()));

		getActivity().findViewById(R.id.fab).setOnClickListener(this);
		getActivity().findViewById(R.id.help_button).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.fab:
				Tour.setSelectedTour(Tour.addNewTour());
				startActivityForResult(new Intent(getActivity(), EditTourActivity.class), EditTourActivity.EDIT_TOUR_REQUEST);
				break;
			case R.id.help_button:
				startActivity(new Intent(getActivity(), HelpActivity.class));
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == EditTourActivity.EDIT_TOUR_REQUEST) {
			_adapter.notifyDataSetChanged();
		}
	}

	/**
	 * Adapter to display tours
	 */
	class TourAdapter extends RecyclerView.Adapter<TourAdapter.ViewHolder> {
		final List<Tour> _tours;

		public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
			public final TextView _text_view;
			public final ImageView _edit_button;

			public ViewHolder(RelativeLayout v) {
				super(v);

				_text_view = (TextView) v.findViewById(R.id.tour_name);
				_edit_button = (ImageView) v.findViewById(R.id.edit_tour);

				_text_view.setOnClickListener(this);
				_edit_button.setOnClickListener(this);
			}

			@Override
			public void onClick(View view) {
				int position = getAdapterPosition();
				Tour.setSelectedTour(_tours.get(position));
				switch(view.getId()) {
					case R.id.edit_tour:
						startActivity(new Intent(getActivity(), EditTourActivity.class));
						break;
					case R.id.tour_name:
						startActivity(new Intent(getActivity(), FollowTourActivity.class));
						break;
				}

			}
		}

		public TourAdapter(List<Tour> tours) {
			_tours = tours;
		}

		// Create new views (invoked by the layout manager)
		@Override
		public TourAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			// create a new view
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tour_line, parent, false);
			// set the view's size, margins, paddings and layout parameters

			return new ViewHolder((RelativeLayout) v);
		}

		// Replace the contents of a view (invoked by the layout manager)
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			// - get element from your dataset at this position
			// - replace the contents of the view with that element
			holder._text_view.setText(_tours.get(position).getName());
			if(!_tours.get(position).getEditable()) {
				/// Disable the edit button
				holder._edit_button.setVisibility(View.GONE);
				holder._edit_button.setClickable(false);
			}
		}

		// Return the size of your dataset (invoked by the layout manager)
		@Override
		public int getItemCount() {
			return _tours.size();
		}

	}

}
