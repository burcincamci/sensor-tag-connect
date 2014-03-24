package sample.ble.sensortag;

import sample.ble.sensortag.adapters.TiServicesAdapter;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class CreateLogActivity extends Activity {

	Button start_button;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_create_log);
		getActionBar().setHomeButtonEnabled(true);
		setContentView(R.layout.activity_create_log);
		addListenerOnButton();
	}

	public void addListenerOnButton() {
		start_button = (Button) findViewById(R.id.start_button);		 
		start_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				EditText whoText = (EditText) findViewById(R.id.who_text);
				Spinner whereSpinner = (Spinner) findViewById(R.id.where_spinner);
				Spinner whatSpinner = (Spinner) findViewById(R.id.what_spinner);
				
				String who = whoText.getText().toString();
				String where = whereSpinner.getSelectedItem().toString();
				String what = whatSpinner.getSelectedItem().toString();
				String filename = who + "_" + where + "_" + what;
				
				writeLog(filename);			
				
			}
		});		
	}
	
	public void writeLog(String filename) {
		Intent intent = new Intent(this, LogWriting.class);
        intent.putExtra(LogWriting.LOG_FILE_NAME, filename);
        startActivity(intent);				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.create_log, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
