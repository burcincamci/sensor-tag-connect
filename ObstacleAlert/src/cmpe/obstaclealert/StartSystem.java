package cmpe.obstaclealert;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import com.google.gson.Gson;

import cmpe.obstaclealert.model.Obstacle;
import cmpe.obstaclealert.model.ObstacleHolder;
import android.view.View;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StartSystem extends Activity {

	public ObstacleHolder obstacles;
	public Obstacle[] allObstacles;
	private TextView enterInputLabel;
	private EditText enterInput;
	private Button submitButton;
	private TextView nameList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_system);

		enterInputLabel = (TextView) findViewById(R.id.enterInputLabel);
		enterInput = (EditText) findViewById(R.id.enterInput);
		submitButton = (Button) findViewById(R.id.submitButton);
		nameList = (TextView) findViewById(R.id.nameList);
	
		
		submitButton.setOnClickListener(new View.OnClickListener() {
	           
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
               String name = enterInput.getText().toString();
            	if( name  != "" ){
            		writeServer(name);
            		getObstacles(name);
            		updateView();
            	}
               
                
            }
        });

	}

	private void updateView(){
		
		String nameListstr = "NAMES\n";
		int count = allObstacles.length;
		for (int i = 0 ; i < count ; i++){
			nameListstr = nameListstr + allObstacles[i] + "\n";
		}
		
		nameList.setText(nameListstr);
	}

	private void getObstacles (String name) {
		Log.i("loginAttempt", "girdi");
		MyAsyncTask task = new MyAsyncTask();
		task.execute(name);
		try {
			String result = task.get();
			Gson gson = new Gson();
			obstacles = gson.fromJson(result, ObstacleHolder.class);
			allObstacles = obstacles.getObstacles();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}


	private void writeServer (String name) {
		Log.i("writeServer", "girdi");
		new MyAsyncTask().execute(name);
		
	}
	public class MyAsyncTask extends AsyncTask<String, String, String> {

		int status = 0;

		@Override
		protected String doInBackground(String... params) {
			String mName = params[0];

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://localhost:8080/assign/Obstacles/" + mName );
			
			String result = "";
			try {

				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("name",mName));
			
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

			System.out.println("obstacles: "  + result);

			return result;
		}
		protected void onPostExecute(String result) {

			if (result.toString().equalsIgnoreCase("true")) {
				Toast.makeText(getApplicationContext(), "command  sent",
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "command couldn't send",
						Toast.LENGTH_LONG).show();
			}
			if (status == 200) {
				//        Toast.makeText(getApplicationContext(), "command sent",
				//            Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "command could not sent",
						Toast.LENGTH_LONG).show();
			}
		}


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
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start_system, menu);
		return true;
	}
	@Override
	protected void onResume() {
		super.onResume();

	}
	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}