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

		BaseLoaderCallback _loader_callback = new BaseLoaderCallback(getActivity()) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
					case LoaderCallbackInterface.SUCCESS:
					{
						Log.i(TAG, "OpenCV loaded successfully");
						_adapter = new TourAdapter(Tour.getTours());
						WrapAdapter wrap_adapter = new WrapAdapter(_adapter);
						wrap_adapter.addFooter( getActivity().getLayoutInflater().inflate(R.layout.empty_list_footer, _recycler_view,   false));
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
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.fab:
				Tour.setSelectedTour(Tour.addNewTour());
				startActivityForResult(new Intent(getActivity(), EditTourActivity.class), EditTourActivity.EDIT_TOUR_REQUEST);
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == EditTourActivity.EDIT_TOUR_REQUEST) {
			_adapter.notifyDataSetChanged();
		}
	}

	class TourAdapter extends RecyclerView.Adapter<TourAdapter.ViewHolder> {
		final List<Tour> _tours;

		public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
			public final TextView _text_view;

			public ViewHolder(RelativeLayout v) {
				super(v);
				_text_view = (TextView) v.findViewById(R.id.tour_name);
				_text_view.setOnClickListener(this);
				v.findViewById(R.id.edit_tour).setOnClickListener(this);
			}

			@Override
			public void onClick(View view) {
				int position = getAdapterPosition();
				Tour.setSelectedTour(Tour.getTours().get(position));
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
		}

		// Return the size of your dataset (invoked by the layout manager)
		@Override
		public int getItemCount() {
			return _tours.size();
		}

	}

}
