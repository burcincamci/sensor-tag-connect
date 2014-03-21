package sample.ble.sensortag;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class LogWriting extends Activity {

	public static final String LOG_FILE_NAME = "FILE_NAME";
	Button stop_button;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_write_log);
		setContentView(R.layout.activity_log_writing);
		String filename = getIntent().getStringExtra(LOG_FILE_NAME);		
		TextView mTextView = (TextView) findViewById(R.id.filename);
		mTextView.setText(filename);		
		addListenerOnButton();
	}
	
	public void addListenerOnButton() {
		stop_button = (Button) findViewById(R.id.stop_button);		 
		stop_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				
			}
		});		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.log_writing, menu);
		return true;
	}

}
