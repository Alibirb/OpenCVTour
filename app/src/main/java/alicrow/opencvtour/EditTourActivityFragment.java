package alicrow.opencvtour;

import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Fragment listing options to edit a Tour.
 */
public class EditTourActivityFragment extends Fragment implements View.OnClickListener {


	public EditTourActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_edit_tour, container, false);

		v.findViewById(R.id.tour_items).setOnClickListener(this);
		v.findViewById(R.id.enable_gps).setOnClickListener(this);
		v.findViewById(R.id.save_tour).setOnClickListener(this);
		v.findViewById(R.id.load_tour).setOnClickListener(this);

		return v;
	}

	@Override
		public void onClick(View v) {
		switch(v.getId()) {
			case R.id.tour_items:
				Intent intent = new Intent(getActivity(), EditTourItemListActivity.class);
				startActivity(intent);
				break;

			case R.id.enable_gps:
				boolean checked = ((CheckBox) v).isChecked();
				Tour.getCurrentTour().setGpsEnabled(checked);
				break;

			case R.id.save_tour: {
				Map<String, Object> data = Tour.getCurrentTour().saveToMap();
				Yaml yaml = new Yaml();
				try {
					String state = Environment.getExternalStorageState();
					Log.i("taggy-thingy", state);

					File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);   /// FIXME: It should NOT be in Pictures. But there's no better option here, except DIRECTORY_DOCUMENTS, which is only available starting in Android 4.4
					Log.i("taggy-thingy", "got public directory");
					File file = new File(dir, "testing_yaml_thingy.yaml");
					Log.i("taggy-thingy", "initialized file");
					FileWriter writer = new FileWriter(file);
					Log.i("taggy-thingy", "created FileWriter");
					yaml.dump(data, writer);
					Log.i("taggy-thingy", "made YAML dump");
					writer.close();
					Toast.makeText(getActivity(), "Successfully saved YAML file", Toast.LENGTH_SHORT).show();


				} catch (IOException e) {
					Toast.makeText(getActivity(), "Could not save YAML file", Toast.LENGTH_SHORT).show();
					Log.e("taggy-thingy", e.toString());
				}
				break;
			}
			case R.id.load_tour: {
				try {
					Yaml yaml = new Yaml();
					String state = Environment.getExternalStorageState();
					Log.i("taggy-thingy", state);

					File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);   /// FIXME: It should NOT be in Pictures. But there's no better option here, except DIRECTORY_DOCUMENTS, which is only available starting in Android 4.4
					Log.i("taggy-thingy", "got public directory");
					File file = new File(dir, "testing_yaml_thingy.yaml");
					Log.i("taggy-thingy", "initialized file");
					FileReader reader = new FileReader(file);

					Map<String, Object> data = (Map<String, Object>) yaml.load(reader);
					Tour.getCurrentTour().loadFromMap(data);

					Toast.makeText(getActivity(), "Successfully loaded YAML file", Toast.LENGTH_SHORT).show();
					///TODO: Update display
				} catch (IOException e) {
					Toast.makeText(getActivity(), "Could not load YAML file", Toast.LENGTH_SHORT).show();
					Log.e("taggy-thingy", e.toString());
				}
				break;
			}


		}
	}

}
