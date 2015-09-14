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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class EditTourActivity extends AppCompatActivity {

	public static final int EDIT_TOUR_REQUEST = 0x002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_tour);
	}
}
