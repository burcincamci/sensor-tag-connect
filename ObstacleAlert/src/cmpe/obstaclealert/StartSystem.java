package cmpe.obstaclealert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

import cmpe.obstaclealert.BackgroundService;
import cmpe.obstaclealert.model.Obstacle;
import cmpe.obstaclealert.model.ObstacleHolder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.Settings;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class StartSystem extends Activity implements LocationListener{

	public ObstacleHolder obstacles;
	public Obstacle[] allObstacles;

	private int i = 0;
	private TextView latitudeText;
	private TextView longitudeText;
	private TextView otherText;

	File dir;
	File file;
	FileOutputStream out;

	private boolean canGetLocation = false;
	private LocationManager locationManager;
	private static double latitude = 0;
	private static double longitude = 0;
	private static Location loc = null;
	private boolean isNetworkEnabled = false;
	private boolean gpsEnabled = false;
	private static final long MINIMUM_UPDATE_TIME = 0;
	private static final float MINIMUM_UPDATE_DISTANCE = 0.0f;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_system);
		startService(new Intent(getBaseContext(), BackgroundService.class));

		latitudeText = (TextView) findViewById(R.id.latitudeText);
		longitudeText = (TextView) findViewById(R.id.longitudeText);
		otherText = (TextView) findViewById(R.id.otherText);

		registerReceiver(powerButtonReceiver, powerButtonPressedIntentFilter());
		File sdCard = Environment.getExternalStorageDirectory();
		dir = new File (sdCard.getAbsolutePath() + "/logs");
		dir.mkdirs();
		file = new File(dir, "EndUser.txt");
		if (file.exists ()) file.delete ();
		try {
			out = new FileOutputStream(file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		getFirstLocation();
		if(!canGetLocation)
			showSettingsAlert();
		if(canGetLocation) {
			Location firstLoc = getFirstLocation();
			double firstLong = firstLoc.getLongitude();
			double firstLat = firstLoc.getLatitude();
			loc = firstLoc;
			longitude = firstLong;
			latitude = firstLat;
					
			updateView(String.valueOf(latitude), String.valueOf(longitude));
			getObstacles(longitude, latitude);
		}

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

		try {
			out.close();
			MediaScannerConnection.scanFile(StartSystem.this, new String[] {file.getAbsolutePath()} , null, null); //to refresh file cache
		} catch (IOException e) {
			e.printStackTrace();
		}
		stopUsingGPS();
		stopService(new Intent(getBaseContext(), BackgroundService.class));

	}

	private void updateView(String latitude, String longitude){
		i++;
		try {
			out.write((longitude + "\t" + latitude +  "\n").getBytes());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// update all data in the UI
		latitudeText.setText(latitude);
		longitudeText.setText(longitude);
		otherText.setText(i+"");
	}
	private static IntentFilter powerButtonPressedIntentFilter() {
		final IntentFilter intentFilter2 = new IntentFilter(Intent.ACTION_SCREEN_ON);		
		intentFilter2.addAction(Intent.ACTION_SCREEN_OFF);
		return intentFilter2;
	}
	private final BroadcastReceiver powerButtonReceiver = new BroadcastReceiver() {
		@SuppressLint("DefaultLocale") @Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) 
			{

				onResume();
			} 
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) 
			{
				onResume();
			}
		}
	};

	public void stopUsingGPS(){
		if(locationManager != null){
			locationManager.removeUpdates(StartSystem.this);
			locationManager = null;		
		}       
	}

	public void showSettingsAlert(){
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		// Setting Dialog Title
		alertDialog.setTitle("GPS is settings");

		// Setting Dialog Message
		alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");


		// On pressing Settings button
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(intent);
			}
		});

		// on pressing cancel button
		alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		// Showing Alert Message
		alertDialog.show();
	}
	@Override
	public void onLocationChanged(Location location) {
		loc = location;
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		updateView(String.valueOf(latitude), String.valueOf(longitude));
		getObstacles(longitude, latitude);
	}

	private void getObstacles (double longitude, double latitude) {
		Log.i("loginAttempt", "girdi");
		MyAsyncTask task = new MyAsyncTask();
		task.execute(Double.toString(longitude), Double.toString(latitude));
		try {
            String result = task.get();
            Gson gson = new Gson();
			obstacles = gson.fromJson(result, ObstacleHolder.class);
			allObstacles = obstacles.getObstacles();
			int obsCount = allObstacles.length;
			Log.i("obstacle count", "" + obsCount);
			Toast.makeText(getApplicationContext(), "obstacle count: " + obsCount,
					Toast.LENGTH_LONG).show();
			if(obsCount>0){
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(500);
			}
				
            
		} catch (InterruptedException e) {
            e.printStackTrace();
		} catch (ExecutionException e) {
            e.printStackTrace();
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
	
	public class MyAsyncTask extends AsyncTask<String, String, String> {
		
		int status = 0;
		
		@Override
		protected String doInBackground(String... params) {
			String mLongitude = params[0];
			String mLatitude = params[1];
			HttpClient httpclient = new DefaultHttpClient();
			//HttpPost httppost = new HttpPost("http://79.123.176.62:8080/ObstacleAlert/GetObstaclesByLocation/" + mLongitude + "/" + mLatitude);
			HttpPost httppost = new HttpPost("http://192.168.1.126:8080/ObstacleAlert/GetObstaclesByLocation/" + mLongitude + "/" + mLatitude);
			String result = "";
			try {
				// Add your data
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
			
			System.out.println("obstacles: "	+ result);
			
			return result;
		}

		protected void onPostExecute(String result) {

			if (status == 200) {
//				Toast.makeText(getApplicationContext(), "command sent",
//						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "command could not sent",
						Toast.LENGTH_LONG).show();
			}
		}


	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public Location getFirstLocation() {
		try {
			locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

			// getting GPS status
			gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!gpsEnabled && !isNetworkEnabled) {
				// no network provider is enabled
			} else {
				canGetLocation = true;
				// First get location from Network Provider
				if (isNetworkEnabled) {
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MINIMUM_UPDATE_TIME,MINIMUM_UPDATE_DISTANCE, this);
					Log.d("Network", "Network");
					if (locationManager != null) {
						loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (loc != null) {
							latitude = loc.getLatitude();
							longitude = loc.getLongitude();
						}
					}
				}
				// if GPS Enabled get lat/long using GPS Services
				if (gpsEnabled) {
					if (loc == null) {
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MINIMUM_UPDATE_TIME, MINIMUM_UPDATE_DISTANCE, this);
						Log.d("GPS Enabled", "GPS Enabled");
						if (locationManager != null) {
							loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (loc != null) {
								latitude = loc.getLatitude();
								longitude = loc.getLongitude();
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return loc;
	}
}