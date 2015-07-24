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
