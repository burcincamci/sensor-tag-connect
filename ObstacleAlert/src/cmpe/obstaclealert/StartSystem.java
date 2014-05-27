package cmpe.obstaclealert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
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
import android.media.AudioManager;
import android.media.MediaPlayer;
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

	double coverage = 100;

	private int i = 0;
	private TextView latitudeText;
	private TextView longitudeText;
	private TextView directionText;
	private TextView obsText;
	private TextView otherText;

	File dir;
	File file;
	FileOutputStream out;

	private boolean canGetLocationNetwork = false;
	private boolean canGetLocationGPS = false;
	private LocationManager locationManager;
	private static double latitude = 0;
	private static double longitude = 0;
	private static Location loc = null;
	private boolean isNetworkEnabled = false;
	private boolean gpsEnabled = false;
	private static final long MINIMUM_UPDATE_TIME = 0;
	private static final float MINIMUM_UPDATE_DISTANCE = 0.0f;

	private static double fromLatitude = 0.0;
	private static double toLatitude = 0.0;
	private static double fromLongitude = 0.0;
	private static double toLongitude = 0.0;
	private static Location fromLocation = null;
	private static Location toLocation = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_system);
		startService(new Intent(getBaseContext(), BackgroundService.class));

		latitudeText = (TextView) findViewById(R.id.latitudeText);
		longitudeText = (TextView) findViewById(R.id.longitudeText);
		directionText = (TextView) findViewById(R.id.directionText);
		obsText = (TextView) findViewById(R.id.obsText);
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
		if(!canGetLocationGPS)
			showSettingsAlertGPS();
		if(!canGetLocationNetwork)
			showSettingsAlertNetwork();

		if(canGetLocationGPS || canGetLocationNetwork) {
			Location firstLoc = getFirstLocation();
			double firstLong = firstLoc.getLongitude();
			double firstLat = firstLoc.getLatitude();
			loc = firstLoc;
			longitude = firstLong;
			latitude = firstLat;
			fromLocation = loc ;
			fromLongitude = longitude;
			fromLatitude = latitude;
			toLocation = loc ;
			toLongitude = longitude;
			toLatitude = latitude;
			double degree = fromLocation.bearingTo(toLocation);
			String obs_type = "";
			getObstacles(toLongitude, toLatitude);
			Obstacle found = findObstacles(toLocation,degree);
			if(found != null) {

				showDialog(found);
				obs_type = found.getType() ;
			}

			updateView(String.valueOf(toLatitude), String.valueOf(toLongitude), String.valueOf(degree), obs_type);

		}

	}
	@SuppressWarnings("deprecation")
	public void showDialog(Obstacle found){

		AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
		Log.i("am.isWiredHeadsetOn()", am.isWiredHeadsetOn()+"");
		if(am.isWiredHeadsetOn()) {
			MediaPlayer mp = MediaPlayer.create(this, R.raw.ding);

			mp.start();
		}

		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		// Vibrate for 500 milliseconds
		v.vibrate(500);

		String type = found.getType() ;


		if(type.equals("stairs")) {
			Toast.makeText(getApplicationContext(), "Be careful! There are stairs!",Toast.LENGTH_LONG).show();

		}
		else if(type.equals("sign")) {
			Toast.makeText(getApplicationContext(), "Be careful! There is a traffic sign!",Toast.LENGTH_LONG).show();

		}
		else if(type.equals("hole")) {
			Toast.makeText(getApplicationContext(), "Be careful! There is a hole!",Toast.LENGTH_LONG).show();

		}
		else if(type.equals("tree")) {
			Toast.makeText(getApplicationContext(), "Be careful! There is a tree!",Toast.LENGTH_LONG).show();

		}
		else {
			Toast.makeText(getApplicationContext(), "Be careful! There is an obstacle!",Toast.LENGTH_LONG).show();

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

	private void updateView(String latitude, String longitude, String degree, String obs_type){
		i++;
		try {
			out.write((longitude + "\t" + latitude + "\t" + degree +  "\n").getBytes());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// update all data in the UI
		latitudeText.setText(latitude);
		longitudeText.setText(longitude);
		directionText.setText(degree);
		obsText.setText(obs_type);
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

	public void showSettingsAlertNetwork() 
	{

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Network Setting");
		builder.setMessage("Internet is not enabled. Do you want to go to settings menu?");
		builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) 
			{
				getFirstLocation();
				Intent intent = new Intent(Settings.ACTION_SETTINGS);
				startActivity(intent);
			}
		});

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int which) 
			{
				dialog.cancel();
			}
		});
		builder.show();
	}
	public void showSettingsAlertGPS(){

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		// Setting Dialog Title
		alertDialog.setTitle("GPS Settings");

		// Setting Dialog Message
		alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");


		// On pressing Settings button
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int which) {
				getFirstLocation();
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
		fromLocation = toLocation ;
		fromLongitude = toLongitude;
		fromLatitude = toLatitude;
		toLocation = loc ;
		toLongitude = longitude;
		toLatitude = latitude;
		double degree = fromLocation.bearingTo(toLocation);
		String obs_type = "";
		getObstacles(toLongitude, toLatitude);
		Obstacle found = findObstacles(toLocation,degree);
		if(found != null) {

			showDialog(found);
			obs_type = found.getType() ;
		}

		updateView(String.valueOf(toLatitude), String.valueOf(toLongitude), String.valueOf(degree), obs_type);


	}

	public Obstacle findObstacles(Location location, double degree){
		ArrayList<Obstacle> foundObstacles = new ArrayList<Obstacle>();
		ArrayList<Double> foundDistances = new ArrayList<Double>();
		int obsCount = allObstacles.length;
		if(obsCount != 0) {

			for(Obstacle obs : allObstacles) {

				double temp_long = obs.getLongitude();
				double temp_lat = obs.getLatitude();
				Location temp_loc = location;
				temp_loc.setLatitude(temp_lat);
				temp_loc.setLongitude(temp_long);
				double temp_degree = location.bearingTo(temp_loc);		
				if( degree-coverage <= temp_degree && degree + coverage >= temp_degree) {
					foundObstacles.add(obs);
					double temp_dist = location.distanceTo(temp_loc);
					foundDistances.add(temp_dist);		
				}
				if(foundObstacles.isEmpty()) {
					return null;
				}
				else {
					int minIndex = foundDistances.indexOf(Collections.min(foundDistances));
					Obstacle returned = foundObstacles.get(minIndex);
					return returned;
				}
			}
		}
		return null;
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
			HttpPost httppost = new HttpPost("http://79.123.176.83:8080/ObstacleAlert/GetObstaclesByLocation/" + mLongitude + "/" + mLatitude);
			//HttpPost httppost = new HttpPost("http://192.168.1.126:8080/ObstacleAlert/GetObstaclesByLocation/" + mLongitude + "/" + mLatitude);
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

			System.out.println("obstacles: "  + result);

			return result;
		}

		protected void onPostExecute(String result) {

			if (status == 200) {
				//        Toast.makeText(getApplicationContext(), "command sent",
				//            Toast.LENGTH_LONG).show();
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
			}else if (!gpsEnabled && isNetworkEnabled) 
				canGetLocationNetwork = true;
			else if (gpsEnabled && !isNetworkEnabled)
				canGetLocationGPS = true;
			else {
				canGetLocationNetwork = true;
				canGetLocationGPS = true;
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