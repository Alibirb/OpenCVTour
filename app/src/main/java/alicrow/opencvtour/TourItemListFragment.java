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

import android.app.Activity;
import android.content.Intent;
import android.app.Fragment;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
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
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import java.util.ArrayList;
import java.util.List;


/**
 * Fragment to display the list of TourItems in a Tour.
 */
public class TourItemListFragment extends Fragment implements View.OnClickListener, com.eyeem.recyclerviewtools.adapter.OnItemClickListenerDetector.OnItemClickListener {
	private static final String TAG = "TourItemListFragment";

	private Tour _tour;
	private TourItemAdapter _adapter;

	/**
	 * Adapter to display TourItems in our list
	 */
	public class TourItemAdapter extends RecyclerView.Adapter<TourItemAdapter.ViewHolder> implements DraggableItemAdapter<TourItemAdapter.ViewHolder>
	{
		private final List<TourItem> _items;

		final int INVALID_ID = -1;

		public class ViewHolder extends AbstractDraggableItemViewHolder implements View.OnClickListener {
			public final TextView _name;
			public final TextView _description;
			public final ImageView _thumbnail;
			public final RelativeLayout _container;

			public ViewHolder(RelativeLayout v) {
				super(v);
				_name = (TextView) v.findViewById(R.id.tour_item_name);
				_description = (TextView) v.findViewById(R.id.tour_item_description);
				_thumbnail = (ImageView) v.findViewById(R.id.tour_item_thumbnail);
				_container = (RelativeLayout) v.findViewById(R.id.container);

				v.findViewById(R.id.delete_tour_item).setOnClickListener(this);
			}

			@Override
			public void onClick(View view) {
				int position = getAdapterPosition();
				switch(view.getId()) {
					case R.id.delete_tour_item:
						if (position != RecyclerView.NO_POSITION) {
							_adapter.getList().remove(position);
							_adapter.notifyDataSetChanged();
						}
						break;
				}
			}

		}

		public TourItemAdapter(List<TourItem> items) {
			_items = items;
			setHasStableIds(true);
		}

		@Override
		public void onMoveItem(int fromPosition, int toPosition) {
			Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");

			if (fromPosition == toPosition)
				return;

			TourItem removed = _items.remove(fromPosition);
			_items.add(toPosition, removed);

			notifyItemMoved(fromPosition, toPosition);
		}

		@Override
		public boolean onCheckCanStartDrag(ViewHolder holder, int position, int x, int y) {
			return true;
		}

		@Override
		public ItemDraggableRange onGetItemDraggableRange(ViewHolder holder, int position) {
			// no drag-sortable range specified
			return null;
		}


		// Create new views (invoked by the layout manager)
		@Override
		public TourItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			// create a new view
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tour_item_line, parent, false);
			// set the view's size, margins, paddings and layout parameters

			return new ViewHolder((RelativeLayout) v);
		}

		// Replace the contents of a view (invoked by the layout manager)
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			TourItem item = _items.get(position);

			holder._name.setText(item.getName());
			holder._description.setText(item.getDescription());
			if(item.getDescription().equals(""))
				holder._description.setVisibility(View.GONE);
			else
				holder._description.setVisibility(View.VISIBLE);

			if(item.hasMainImage()) {
				String image_filepath = item.getMainImageFilepath();
				ImageView image_view = holder._thumbnail;
				Utilities.loadBitmap(image_view, image_filepath, Utilities.dp_to_px(40), Utilities.dp_to_px(40), getActivity());
			} else
				holder._thumbnail.setImageResource(R.drawable.default_thumbnail);

			// set background resource (target view ID: container)
			final int dragState = holder.getDragStateFlags();

			if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0)) {
				int bgResId;

				if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
					bgResId = R.drawable.bg_item_dragging_active_state;
				} else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
					bgResId = R.drawable.bg_item_dragging_state;
				} else {
					bgResId = R.drawable.bg_item_normal_state;
				}
				holder._container.setBackgroundResource(bgResId);
			}
		}

		@Override
		public int getItemCount() {
			return _items.size();
		}

		@Override
		public long getItemId(int position) {
			if (position < 0 || position >= _items.size()) {
				return INVALID_ID;
			}
			return _items.get(position).getId();
		}

		public List<TourItem> getList() {
			return _items;
		}

	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_tour_item_list, container, false);

		v.findViewById(R.id.fab).setOnClickListener(this);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_tour = Tour.getCurrentTour();

		RecyclerView recycler_view = (RecyclerView) getActivity().findViewById(R.id.recycler_view);
		recycler_view.setLayoutManager(new LinearLayoutManager(getActivity()));

		ArrayList<TourItem> tour_items = _tour.getTourItems();

		_adapter = new TourItemAdapter(tour_items);

		/// We're using two different RecyclerView adapter libraries, which were not designed to work together. CrazyWrapAdapter provides a WrapAdapter from eyeem's library that also implements DraggableItemAdapter from h6ah4i's library (by passing to the TourItemAdapter that actually handles that stuff).
		class CrazyWrapAdapter extends WrapAdapter implements DraggableItemAdapter<TourItemAdapter.ViewHolder> {

			public CrazyWrapAdapter(RecyclerView.Adapter wrappedAdapter) {
				super(wrappedAdapter);
			}

			@Override public void onMoveItem(int fromPosition, int toPosition) {
				((DraggableItemAdapter) wrapped).onMoveItem(fromPosition, toPosition);
			}

			@Override public boolean onCheckCanStartDrag(TourItemAdapter.ViewHolder holder, int position, int x, int y) {
				return ((DraggableItemAdapter) wrapped).onCheckCanStartDrag(holder, position, x, y);
			}

			@Override public ItemDraggableRange onGetItemDraggableRange(TourItemAdapter.ViewHolder holder, int position) {
				return ((DraggableItemAdapter) wrapped).onGetItemDraggableRange(holder, position);
			}
		}

		CrazyWrapAdapter wrap_adapter = new CrazyWrapAdapter(_adapter);
		wrap_adapter.setOnItemClickListener(recycler_view, this);
		wrap_adapter.addFooter(getActivity().getLayoutInflater().inflate(R.layout.empty_list_footer, recycler_view, false));


		/// Stuff for drag and drop functionality:

		RecyclerViewDragDropManager drag_drop_manager = new RecyclerViewDragDropManager();
		drag_drop_manager.setInitiateOnLongPress(true);
		//noinspection deprecation
		drag_drop_manager.setDraggingItemShadowDrawable((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z3));

		RecyclerView.Adapter drag_drop_adapter = drag_drop_manager.createWrappedAdapter(wrap_adapter); // wrap for dragging

		final GeneralItemAnimator animator = new RefactoredDefaultItemAnimator();

		recycler_view.setAdapter(drag_drop_adapter);  // requires *wrapped* adapter
		recycler_view.setItemAnimator(animator);

		// Shadow for pre-Lollipop devices (Lollipop has built-in elevation stuff)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			//noinspection deprecation
			recycler_view.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z1)));
		}

		drag_drop_manager.attachRecyclerView(recycler_view);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.fab:
				addNewTourItem();
				break;
		}
	}

	@Override
	public void onItemClick(RecyclerView parent, View v, int position, long id) {
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
		_tour.addNewTourItem();
		_adapter.notifyDataSetChanged();
		editTourItem(_tour.getTourItems().size() - 1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST) {
			/// EditTourItemActivity has returned. We may need to update our RecyclerView with the item's new properties.
			_adapter.notifyDataSetChanged();
		}
	}


}
