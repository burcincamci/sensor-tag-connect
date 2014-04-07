package sample.ble.sensortag;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class CreateLogActivity extends Activity {

	Button save_button;
	Intent coming;
	String log_data;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_create_log);
		getActionBar().setHomeButtonEnabled(true);
		setContentView(R.layout.activity_create_log);
		coming = getIntent();
		log_data = coming.getStringExtra("Log_Data");
		addListenerOnButton();
	}

	public void addListenerOnButton() {
		save_button = (Button) findViewById(R.id.save_button);		 
		save_button.setOnClickListener(new OnClickListener() {
			

			@Override
			public void onClick(View v) {
				
				EditText whoText = (EditText) findViewById(R.id.who_text);
				Spinner whereSpinner = (Spinner) findViewById(R.id.where_spinner);
				Spinner whatSpinner = (Spinner) findViewById(R.id.what_spinner);
				
				String who = whoText.getText().toString();
				String where = whereSpinner.getSelectedItem().toString();
				String what = whatSpinner.getSelectedItem().toString();
				String filename = who + "_" + where + "_" + what + ".txt";
		
				FileOutputStream outputStream;

				try {
				  outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
				  outputStream.write(log_data.getBytes());
				  outputStream.close();
				} catch (Exception e) {
				  e.printStackTrace();
				}
				Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		});		
	}
	
	public boolean saveFile(Context context, String mytext, String filename){
	    Log.i("TESTE", "SAVE");
	    try {
	        FileOutputStream fos = context.openFileOutput(filename,Context.MODE_PRIVATE);
	        Writer out = new OutputStreamWriter(fos);
	        out.write(mytext);
	        out.close();
	        return true;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return false;
	    }
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
