package alicrow.opencvtour;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


public class HelpActivity extends AppCompatActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		findViewById(R.id.about_button).setOnClickListener(this);
		findViewById(R.id.report_issue).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.about_button: {
				startActivity(new Intent(this, com.h6ah4i.android.example.advrecyclerview.about.AboutActivity.class));
				break;
			}
			case R.id.report_issue: {
				/// Open the issues page in the browser
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.report_issue_url))));
				break;
			}
		}
	}
}
