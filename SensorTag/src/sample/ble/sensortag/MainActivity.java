package sample.ble.sensortag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import sample.ble.sensortag.DeviceServicesActivity.MyAsyncTask;
import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	Button sign_in;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		addListenerOnButton();
	}

	private void addListenerOnButton() {
		sign_in = (Button) findViewById(R.id.sign_in);
		sign_in.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				EditText usernameText = (EditText) findViewById(R.id.username);
				EditText passwordText = (EditText) findViewById(R.id.password);
				
				String username = usernameText.getText().toString();
				String password = passwordText.getText().toString();
				
				loginAttempt(username, password);
				
				
				
			}
		});
		
	}
	
	@SuppressLint("DefaultLocale")
	private void loginAttempt (String username, String password) {
		Log.i("loginAttempt", "girdi");
		new MyAsyncTask().execute(username, password);
		
	}
	
	private StringBuilder inputStreamToString(InputStream is) {
        String line = "";
        StringBuilder total = new StringBuilder();
        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        // Read response until the end
        try {
                while ((line = rd.readLine()) != null) { 
                        total.append(line); 
                }
        } catch (IOException e) {
                e.printStackTrace();
        }
        // Return full string
        return total;
	}
	
	public class MyAsyncTask extends AsyncTask<String, Integer, String> {
		
		int status = 0;
		
		@Override
		protected String doInBackground(String... params) {
			String username = params[0];
			String password = params[1];
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
					"http://79.123.176.62:8080/ObstacleAlert/CheckUser");
			String result = "";
			try {
				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("username",
						username));
				nameValuePairs
						.add(new BasicNameValuePair("password", password));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				System.out.println("result:" + 213);

				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(httppost);
				result = inputStreamToString(response.getEntity().getContent())
						.toString();
				System.out.println("result:"
						+ response.getStatusLine().getStatusCode());

				status = response.getStatusLine().getStatusCode();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		}

		protected void onPostExecute(String result) {

			if (result.toString().equalsIgnoreCase("true")) {
				final Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(getApplicationContext(), "wrong user",
						Toast.LENGTH_LONG).show();
			}
		}


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
