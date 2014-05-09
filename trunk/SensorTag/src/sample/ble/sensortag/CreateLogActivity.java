package sample.ble.sensortag;


import java.io.File;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;

import android.content.Intent;

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
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File (sdCard.getAbsolutePath() + "/logs");

				
//					
//					dir.mkdirs();
//					File file = new File(dir, filename);
//					if (file.exists ()) file.delete (); 
//					try {
//						FileOutputStream out = new FileOutputStream(file);
//						out.write(log_data.getBytes());
//						out.flush();
//						out.close();
//						MediaScannerConnection.scanFile(CreateLogActivity.this, new String[] {file.getAbsolutePath()} , null, null); //to refresh file cache
//
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				
				
				File oldfile = new File(dir, "NewLog.txt");
				File newfile = new File(dir, filename);
				
				if(oldfile.exists())
					oldfile.renameTo(newfile);
				


				Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		});		
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
