/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.ble.sensortag;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;


import sample.ble.sensortag.adapters.TiServicesAdapter;
import sample.ble.sensortag.info.TiInfoService;
import sample.ble.sensortag.info.TiInfoServices;
import sample.ble.sensortag.sensor.TiPeriodicalSensor;
import sample.ble.sensortag.sensor.TiSensor;
import sample.ble.sensortag.sensor.TiSensors;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BleService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceServicesActivity extends Activity implements LocationListener {
	private final static String TAG = DeviceServicesActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static final String EXTRAS_USER_TYPE = "USER_TYPE";

	final Context context = this;
	private TextView connectionState;
	private TextView dataField;

	private Button startLog;
	private Button stopLog;
	private ExpandableListView gattServicesList;
	private ExpandableListView expandableListView;
	private TiServicesAdapter gattServiceAdapter;
	private SampleExpandableListAdapter ela;
	private String deviceName;
	private String deviceAddress;
	private String userType;
	private BleService bleService;
	private boolean isConnected = false;
	private TiSensor<?> activeSensor;

	static String tmp_data = "";
	static String acc_data = "";
	static String hum_data = "";
	static String mag_data = "";
	static String prs_data = "";
	static String gyr_data = "";
	static String key_data = "";
	static String gps_data = "";
	static String activity_final = "";
	static String activity = "";
	static String shown_activity = "";
	
	String [] data;
	ArrayList <Integer> data_index = new ArrayList<Integer>();
	ArrayList <Integer> active_index = new ArrayList<Integer>();
	private LinkedList <TiSensor<?> > activeSensors = new LinkedList<TiSensor<?>>();
	File dir;
	File file;
	FileOutputStream out;
	
	File file_obs;
	FileOutputStream out_obs;



	final static int windowSizeHead = 20;
	final static int windowSizeBelt = 20;
	final static int windowSizeSocks = 20;
	final static int overlapRate = 5;

	static String [] acc_window_head = new String [windowSizeHead];
	static String [] acc_window_belt = new String [windowSizeBelt];
	static String [] acc_window_socks = new String [windowSizeSocks];

	static int pointer_head = 0;
	static int pointer_belt = 0;
	static int pointer_socks = 0;

	static int activity_threshold = 20 ; 
	static int activity_overlap = 5;

	static String [] window_activity = new String [activity_threshold];

	static int pointer_activity = 0;

	static double activity_def_accuracy = 0.8;
	static double activity_pos_accuracy = 0.6;

	static double activity_counter [] = new double[6];

	private boolean gps_selected = false;

	private boolean canGetLocation = false;
	private LocationManager locationManager;
	private static double latitude = 0;
	private static double longitude = 0;
	private static Location loc = null;
	private boolean isNetworkEnabled = false;
	private boolean gpsEnabled = false;
	private static final long MINIMUM_UPDATE_TIME = 0;
	private static final float MINIMUM_UPDATE_DISTANCE = 0.0f;

	// Code to manage Service lifecycle.
	private final ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			bleService = ((BleService.LocalBinder) service).getService();
			if (!bleService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			bleService.connect(deviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			bleService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
	//                        or notification operations.

	private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
		@SuppressLint("DefaultLocale") @Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
				isConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
				isConnected = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				clearUI();
			} else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				displayGattServices(bleService.getSupportedGattServices());
			} else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
				String temp = intent.getStringExtra(BleService.EXTRA_TEXT);
				int index  = temp.indexOf('|');
				String sensor = temp.substring(0, 3);
				String sensor_data = temp.substring(index+1); 
				String [] coordinates = new String [3]; // for accelerometer, magnetometer and gyroscope
				String [] one_two = new String[2]; // for temperature
				if(userType.equals("Trainer")) {

					// Temperature Data parsing
					String temp_tmp_data ;
					if(sensor.equalsIgnoreCase("TEM") ){
						one_two = sensor_data.split("\n");           		
						one_two[0] = one_two[0].substring(8);
						one_two[1] = one_two[1].substring(7);
						temp_tmp_data = one_two[0] + "\t" + one_two[1] ;
					}
					else{
						temp_tmp_data = tmp_data;
					}
					if(!temp_tmp_data.equals(tmp_data)) {
						tmp_data = temp_tmp_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + tmp_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Accelerometer Data parsing
					String temp_acc_data ;
					if(sensor.equalsIgnoreCase("ACC") ){
						coordinates = sensor_data.split("\n");
						for(int i = 0 ; i < 3 ; i++) {
							coordinates[i] = coordinates[i].substring(2);
						}
						temp_acc_data = coordinates[0] + "\t" + coordinates[1]+ "\t" + coordinates[2] ;
					}
					else{
						temp_acc_data = acc_data;
					}
					if(!temp_acc_data.equals(acc_data)) {
						acc_data = temp_acc_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + acc_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Humidity Data parsing
					String temp_hum_data ;
					if(sensor.equalsIgnoreCase("HUM") ){
						temp_hum_data = sensor_data  ;
					}
					else{
						temp_hum_data = hum_data;
					}
					if(!temp_hum_data.equals(hum_data)) {
						hum_data = temp_hum_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + hum_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Magnetometer Data parsing
					String temp_mag_data ;
					if(sensor.equalsIgnoreCase("MAG") ){
						coordinates = sensor_data.split("\n");
						for(int i = 0 ; i < 3 ; i++) {
							coordinates[i] = coordinates[i].substring(2);
						}
						temp_mag_data = coordinates[0] + "\t" + coordinates[1]+ "\t" + coordinates[2] ;
					}else{
						temp_mag_data = mag_data;
					}
					if(!temp_mag_data.equals(mag_data)) {
						mag_data = temp_mag_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + mag_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Pressure Data parsing
					String temp_prs_data ;
					if(sensor.equalsIgnoreCase("PRE") ){            		
						temp_prs_data = sensor_data ;
					}else{
						temp_prs_data = prs_data;
					}
					if(!temp_prs_data.equals(prs_data)) {
						prs_data = temp_prs_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + prs_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Gyrposcope Data parsing
					String temp_gyr_data ;
					if(sensor.equalsIgnoreCase("GYR") ){
						coordinates = sensor_data.split("\n");
						for(int i = 0 ; i < 3 ; i++) {
							coordinates[i] = coordinates[i].substring(2);
						}
						temp_gyr_data = coordinates[0] + "\t" + coordinates[1]+ "\t" + coordinates[2] ;
					}else{
						temp_gyr_data = gyr_data;
					}
					if(!temp_gyr_data.equals(gyr_data)) {
						gyr_data = temp_gyr_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + gyr_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Simple Keys Data Parsing
					String temp_key_data ;
					if(sensor.equalsIgnoreCase("SIM") ){
						one_two = sensor_data.split("_");           		
						temp_key_data = one_two[0] + "\t" + one_two[1] ;
					}
					else{
						temp_key_data = key_data;
					}
					if(!temp_key_data.equals(key_data)) {
						key_data = temp_key_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + "KEY" + "\t" + key_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// GPS Data Parsing
					String temp_gps_data ;
					if(canGetLocation && gps_selected){
						Location firstLoc = getFirstLocation();
						double firstLong = firstLoc.getLongitude();
						double firstLat = firstLoc.getLatitude();
						loc = firstLoc;
						longitude = firstLong;
						latitude = firstLat;
						
						temp_gps_data = longitude + "\t" + latitude ; 

					}else{
						temp_gps_data = gps_data ;
					}
					if(!temp_gps_data.equals(gps_data)) {
						gps_data = temp_gps_data;	
						try {
							out.write((System.currentTimeMillis() + "\t" + "GPS" + "\t" + gps_data + "\n").getBytes());
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					displayData(temp);
				}
				else {
					
					
					String temp_acc_data ;
					if(sensor.equalsIgnoreCase("ACC") ){
						coordinates = sensor_data.split("\n");
						for(int i = 0 ; i < 3 ; i++) {
							coordinates[i] = coordinates[i].substring(2);
						}
						temp_acc_data = coordinates[0] + "\t" + coordinates[1]+ "\t" + coordinates[2] ;
					}
					else{
						temp_acc_data = acc_data;
					}
					if(!temp_acc_data.equals(acc_data)) {
						acc_data = temp_acc_data;

						int index2  = userType.indexOf('-');
						String where = userType.substring(index2+1);


						if(where.equals("Head")) {
							int ind = pointer_head % windowSizeHead;
							acc_window_head[ind] = acc_data;
							pointer_head++;
							if(pointer_head >= windowSizeHead && pointer_head % overlapRate == 0)
								activity = decisionTree(where);	
						}

						else if (where.equals("Belt")){	
							int ind = pointer_belt % windowSizeBelt;
							acc_window_belt[ind] = acc_data;
							pointer_belt++;
							if(pointer_belt >= windowSizeBelt && pointer_belt % overlapRate == 0)
								activity = decisionTree(where);	
						}

						else {	
							int ind = pointer_socks % windowSizeSocks;
							acc_window_socks[ind] = acc_data;
							pointer_socks++;
							if(pointer_socks >= windowSizeSocks && pointer_socks % overlapRate == 0)
								activity = decisionTree(where);	
						}

						

						displayData("Activity : " + activity + " pointer_activity: " + pointer_activity + " activity_final: " + activity_final );
						int ind = pointer_activity % activity_threshold;
						window_activity[ind] = activity;
						pointer_activity++;
						int counter [] = new int [6];

						if(pointer_activity >= activity_threshold && pointer_activity % activity_overlap == 0  ) {
							for(int i = 0 ; i < activity_threshold ; i++) {
								if(window_activity[i].equals("Standing")) {
									counter[0] = counter[0] + 1;
								}
								else if(window_activity[i].equals("Walking")) {
									counter[1] = counter[1] + 1;
								}
								else if(window_activity[i].equals("Running")) {
									counter[2] = counter[2] + 1;
								}
								else if(window_activity[i].equals("Hill")) {
									counter[3] = counter[3] + 1;
								}
								else if(window_activity[i].equals("Stairs")) {
									counter[4] = counter[4] + 1;
								}
								else {
									counter[5] = counter[5] + 1;
								}
							}
							for(int i = 0; i < 6 ; i++) 
								activity_counter[i] = counter[i] / activity_threshold ;

						}
						// 0 -> standing, 1 -> walking, 2 -> running, 3 -> hill, 4 -> stairs, 5 -> empty
						int max_index = max_index(activity_counter);
						if(max_index == 0)
							activity_final = "Standing";
						else if (max_index == 1)
							activity_final = "Walking";
						else if (max_index == 2)
							activity_final = "Running";
						else if (max_index == 3) 
							activity_final = "Hill";
						else if (max_index == 4)
							activity_final = "Stairs";
						else 
							activity_final = "";
						
						
						if(canGetLocation && gps_selected){
							Location firstLoc = getFirstLocation();
							double firstLong = firstLoc.getLongitude();
							double firstLat = firstLoc.getLatitude();
							loc = firstLoc;
							longitude = firstLong;
							latitude = firstLat;
							gps_data = longitude + "\t" + latitude ; 
						}

						if(activity_counter[max_index] >= activity_def_accuracy ) {
							
							writeServer(activity_final, longitude, latitude);
						}
						else if (activity_counter[max_index] >= activity_pos_accuracy) {


							if( !shown_activity.equals(activity_final)) {
								shown_activity = activity_final;
								// 1. Instantiate an AlertDialog.Builder with its constructor
								AlertDialog.Builder builder = new AlertDialog.Builder(DeviceServicesActivity.this);
								boolean check = false;
								//								if(activity_final.equals("Standing")) {
								//									// 2. Chain together various setter methods to set the dialog characteristics
								//									builder.setMessage(R.string.standing_message).setTitle(R.string.dialog_title);
								//									check = true;
								//								}
								//								else if(activity_final.equals("Walking")) {
								//									// 2. Chain together various setter methods to set the dialog characteristics
								//									builder.setMessage(R.string.walking_message).setTitle(R.string.dialog_title);
								//									check = true;
								//								}
								//								else if(activity_final.equals("Running")) {
								//									// 2. Chain together various setter methods to set the dialog characteristics
								//									builder.setMessage(R.string.running_message).setTitle(R.string.dialog_title);
								//									check = true;
								//								}
								//								else if(activity_final.equals("Hill")) {
								//									// 2. Chain together various setter methods to set the dialog characteristics
								//									builder.setMessage(R.string.hill_message).setTitle(R.string.dialog_title);
								//									check = true;
								//								}
								if(activity_final.equals("Stairs")){
									// 2. Chain together various setter methods to set the dialog characteristics
									builder.setMessage(R.string.stairs_message).setTitle(R.string.dialog_title);
									check = true;
								}
								else {
									check = false;
								}

								if(check){
									Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
									// Vibrate for 500 milliseconds
									v.vibrate(500);
									// Add the buttons
									builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											// User clicked OK button
											writeServer(activity_final, longitude, latitude);
										}
									});
									builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											// User cancelled the dialog
											// YANLIÞ DATA!!!
										}
									});
									// 3. Get the AlertDialog from create()
									AlertDialog dialog = builder.create();
									dialog.show();
								}

							}

						}

					}

				}
			}
		}
	};
	
	@SuppressLint("DefaultLocale")
	private void writeServer (String activity, double longitude, double latitude) {
		Log.i("writeServer", "girdi");
		activity = activity.toLowerCase();
		new MyAsyncTask().execute(Double.toString(longitude),
									Double.toString(latitude),
										activity);
		
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
			String longitude = params[0];
			String latitude = params[1];
			String type = params[2];
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
					"http://79.123.176.62:8080/ObstacleAlert/AddObstacle");
			String result = "";
			try {
				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("longitude",
						longitude));
				nameValuePairs
						.add(new BasicNameValuePair("latitude", latitude));
				nameValuePairs.add(new BasicNameValuePair("type", type));
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
				Toast.makeText(getApplicationContext(), "command  sent",
						Toast.LENGTH_LONG).show();
			} else {
				if(status == 200){
					Toast.makeText(getApplicationContext(), "obstacle already exists",
							Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(getApplicationContext(), "command couldn't send",
						Toast.LENGTH_LONG).show();
				}
			}
		}


	}
	
	
	
	private static String decisionTree(String where) {
		String activity = "No activity";
		if(where.equals("Head")) {

			double [] xData = new double[windowSizeHead];
			double [] yData = new double[windowSizeHead];
			double [] zData = new double[windowSizeHead];
			String [] x_y_z = new String [3];
			for (int i = 0; i< windowSizeHead ; i ++) {
				String temp = acc_window_head[i];
				x_y_z = temp.split("\t");
				xData[i] = Double.parseDouble(x_y_z[0]);
				yData[i] = Double.parseDouble(x_y_z[1]);
				zData[i] = Double.parseDouble(x_y_z[2]);	
			}
			double maxY = max(yData);
			double varianceY = variance(yData);
			double minZ = min(zData);
			double averageX = mean(xData);
			if(maxY > -0.1016) 
				activity = "Running";
			else {
				if(varianceY <= 0.0099) 
					activity = "Standing";
				else {
					if(minZ <=-0.2109)
						activity = "Hill";
					else {
						if(maxY <= -0.7576)
							activity = "Walking";
						else {
							if(averageX <= 0.1324)
								activity = "Walking";
							else 
								activity = "Stairs";
						}
					}
				}
			}

		}
		else if(where.equals("Belt")) {

			double [] xData = new double[windowSizeBelt];
			double [] yData = new double[windowSizeBelt];
			double [] zData = new double[windowSizeBelt];
			String [] x_y_z = new String [3];
			for (int i = 0; i< windowSizeBelt ; i ++) {
				String temp = acc_window_belt[i];
				x_y_z = temp.split("\t");
				xData[i] = Double.parseDouble(x_y_z[0]);
				yData[i] = Double.parseDouble(x_y_z[1]);
				zData[i] = Double.parseDouble(x_y_z[2]);
			}
			double varianceX = variance(xData);
			double zcrX = ZCR(xData,windowSizeBelt);
			double averageZ = mean(zData);
			double averageY = mean(yData);
			double minZ = min(zData);
			if(varianceX <= 0.0065)
				activity = "Standing";
			else {
				if(zcrX > 0.05) 
					activity = "Running";
				else {
					if(averageZ <= -0.0488)
						activity = "Stairs";
					else {
						if(averageY <= 0.1301) 
							activity = "Stairs";
						else {
							if(minZ <= -0.0703) {
								if(varianceX <= 0.042) 
									activity = "Walking";
								else 
									activity = "Hill";
							}
							else {
								if(averageY <= 0.1684) 
									activity = "Stairs";
								else 
									activity = "Walking";
							}
						}
					}
				}
			}

		}
		else{

			double [] xData = new double[windowSizeSocks];
			double [] yData = new double[windowSizeSocks];
			double [] zData = new double[windowSizeSocks];
			String [] x_y_z = new String [3];
			for (int i = 0; i< windowSizeSocks ; i ++) {
				String temp = acc_window_socks[i];
				x_y_z = temp.split("\t");
				xData[i] = Double.parseDouble(x_y_z[0]);
				yData[i] = Double.parseDouble(x_y_z[1]);
				zData[i] = Double.parseDouble(x_y_z[2]);
			}
			double magnitude = magnitude(xData,yData,zData);
			double averageY = mean(yData);
			double zcrY = ZCR(yData, windowSizeSocks);
			double varianceY = variance(yData);
			double averageZ = mean(zData);
			if(magnitude <= 4.6712) 
				activity = "Standing";
			else {
				if(magnitude > 8.2855) 
					activity = "Running";
				else {
					if(averageY> -1.0391)
						activity = "Stairs";
					else {
						if(magnitude > 6.758) 
							activity = "Hill";
						else {
							if(zcrY > 0.05) 
								activity = "Stairs";
							else {
								if(varianceY <= 0.1673) 
									activity = "Hill";
								else {
									if(averageZ <= -0.3688) 
										activity = "Hill";
									else 
										activity = "Walking";
								}
							}
						}
					}
				}

			}

		}
		return activity;
	}


	/**
	 * Returns the maximum value in the array.
	 */

	public static double max(double[] v) {
		double m = v[0];
		for (int i = 1; i < v.length; i++)
			m = Math.max(m, v[i]);
		return m;
	}
	public static int max_index(double[] v) {
		int index = 0; 
		double m = v[0];
		for (int i = 1; i < v.length; i++) 
			if(m>v[i])
				index = i ;

		return index;
	}

	/**
	 * Returns the minimum value in the array.
	 */

	public static double min(double[] v) {
		double m = v[0];
		for (int i = 1; i < v.length; i++)
			m = Math.min(m, v[i]);
		return m;
	}
	/**
	 * Returns the average of an array of double.
	 */

	public static double mean(double[] v) {
		double tot = 0.0;
		for (int i = 0; i < v.length; i++)
			tot += v[i];
		return tot / v.length;
	}
	/**
	 * Returns the variance of the array of double.
	 */

	public static double variance(double[] v) {
		double mu = mean(v);
		double sumsq = 0.0;
		for (int i = 0; i < v.length; i++)
			sumsq += sqr(mu - v[i]);
		return sumsq / (v.length);
		// return 1.12; this was done to test a discrepancy with Business
		// Statistics
	}
	/**
	 * Calculates the square of a double.
	 * 
	 * @return Returns x*x
	 */

	public static double sqr(double x) {
		return x * x;
	}

	public static double magnitude (double[] x, double[] y, double[] z ) {
		int size = x.length;
		double result = 0;
		for(int i = 0; i < size ; i++){
			result += Math.sqrt((sqr(x[i]) + sqr(y[i]) + sqr(z[i])));
		}
		return result;
	}

	public static double ZCR(double[]signals,  double lengthInSecond){
		int numZC=0;
		int size=signals.length;

		for (int i=0; i<size-1; i++){
			if((signals[i]>=0 && signals[i+1]<0) || (signals[i]<0 && signals[i+1]>=0)){
				numZC++;
			}
		}                       

		return numZC/lengthInSecond;
	}
	// If a given GATT characteristic is selected, check for supported features.  This sample
	// demonstrates 'Read' and 'Notify' features.  See
	// http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
	// list of supported characteristic features.
	private final ExpandableListView.OnChildClickListener servicesListClickListner =
			new ExpandableListView.OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
				int childPosition, long id) {
			if (gattServiceAdapter == null)
				return false;

			final BluetoothGattCharacteristic characteristic = gattServiceAdapter.getChild(groupPosition, childPosition);
			final TiSensor<?> sensor = TiSensors.getSensor(characteristic.getService().getUuid().toString());

			if (activeSensor != null)
				bleService.enableSensor(activeSensor, false);

			if (sensor == null) {
				bleService.readCharacteristic(characteristic);
				return true;
			}

			if (sensor == activeSensor)
				return true;

			activeSensor = sensor;
			bleService.enableSensor(sensor, true);


			return true;
		}
	};
	private final Button.OnClickListener servicesListClickListener =
			new Button.OnClickListener() {

		@Override
		public void onClick(View v) {

			startLog.setEnabled(false);
			stopLog.setEnabled(true);
			startService(new Intent(getBaseContext(), BackgroundService.class));

			for(TiSensor<?> sensor : activeSensors){
				if(sensor != null)
					bleService.enableSensor(sensor, false);
			}
			activeSensors.clear();
			active_index.clear();

			file = new File(dir, "NewLog.txt");
			if (file.exists ()) file.delete ();
			try {
				out = new FileOutputStream(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);  
			String buffer = null; 		    
			int size = data.length;
			for (int i = 0; i < size; i++) {
				buffer = settings.getString(String.valueOf((int)i),"false");
				if(buffer.equals("true"))
					if(i != size-1){ // except GPS
						int index = data_index.get(i);
						active_index.add(index);
					}
					else if(i == size-1){ // for GPS
						gps_selected = true;
					}

			}
			if(active_index.isEmpty())
				return;

			int active_size = active_index.size();
			for(int i = 0; i < active_size; i++){
				final BluetoothGattCharacteristic characteristic = gattServiceAdapter.getChild(active_index.get(i), 0);
				final TiSensor<?> sensor = TiSensors.getSensor(characteristic.getService().getUuid().toString());
				if (sensor == null) {
					bleService.readCharacteristic(characteristic);
					return ;
				}

				bleService.enableSensor(sensor, true);
				if (sensor instanceof TiPeriodicalSensor) {
					final TiPeriodicalSensor periodicalSensor = (TiPeriodicalSensor) sensor;
					periodicalSensor.setPeriod(periodicalSensor.getMinPeriod());
				}
				bleService.updateSensor(sensor);
				activeSensors.add(sensor);


			}


		}
	};
	private final TiServicesAdapter.OnServiceItemClickListener demoClickListener = new TiServicesAdapter.OnServiceItemClickListener() {

		@Override
		public void onServiceEnabled(BluetoothGattService service, boolean enabled) {
			if (gattServiceAdapter == null)
				return;

			final TiSensor<?> sensor = TiSensors.getSensor(service.getUuid().toString());
			if (sensor == null)
				return;  

			if (activeSensors.contains(sensor))
				return;

			bleService.enableSensor(sensor, true);
			activeSensors.add(sensor);

		}

		@Override
		public void onServiceUpdated(BluetoothGattService service) {
			final TiSensor<?> sensor = TiSensors.getSensor(service.getUuid().toString());
			if (sensor == null)
				return;

			bleService.updateSensor(sensor);
		}
	};


	private void clearUI() {
		gattServicesList.setAdapter((SimpleExpandableListAdapter) null);
		dataField.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 

		setContentView(R.layout.gatt_services_characteristics); 

		data = null;

		File sdCard = Environment.getExternalStorageDirectory();
		dir = new File (sdCard.getAbsolutePath() + "/logs");
		dir.mkdirs();


		registerReceiver(powerButtonReceiver, powerButtonPressedIntentFilter());

	
		getFirstLocation();
		if(!canGetLocation)
			showSettingsAlert(); 

		final Intent intent = getIntent();
		deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		userType = intent.getStringExtra(EXTRAS_USER_TYPE);        

		((TextView) findViewById(R.id.device_address)).setText(deviceAddress); 
		gattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
		gattServicesList.setOnChildClickListener(servicesListClickListner);
		gattServicesList.setVisibility(-1);



		expandableListView = (ExpandableListView)findViewById(R.id.expandableListView1);
		file_obs = new File(dir, "Obstacles.txt");
		if (file_obs.exists ()) file_obs.delete ();
		try {
			out_obs = new FileOutputStream(file_obs);
		} catch (Exception e) {
			e.printStackTrace();
		}


		startLog = (Button) findViewById(R.id.log_start);
		startLog.setOnClickListener(servicesListClickListener);

		stopLog = (Button) findViewById(R.id.log_stop);
		stopLog.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				for(TiSensor<?> sensor : activeSensors){
					if(sensor != null)
						bleService.enableSensor(sensor, false);
				}
				activeSensors.clear();
				active_index.clear();

				try {
					out.close();
					MediaScannerConnection.scanFile(DeviceServicesActivity.this, new String[] {file.getAbsolutePath()} , null, null); //to refresh file cache
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					out_obs.close();
					MediaScannerConnection.scanFile(DeviceServicesActivity.this, new String[] {file_obs.getAbsolutePath()} , null, null); //to refresh file cache
				} catch (IOException e) {
					e.printStackTrace();
				}
				stopService(new Intent(getBaseContext(), BackgroundService.class));
				if(userType.equals("Trainer"))
					createLog();
				else {
					Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(i);
				}
			}
		});

		startLog.setEnabled(true);
		stopLog.setEnabled(false);

		connectionState = (TextView) findViewById(R.id.connection_state);   
		dataField = (TextView) findViewById(R.id.data_value);  

		getActionBar().setTitle(deviceName);   
		getActionBar().setDisplayHomeAsUpEnabled(true); 

		final Intent gattServiceIntent = new Intent(this, BleService.class);
		bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

	}




	private void createData(ExpandableListView GSL) {



		int group_count = GSL.getCount();
		ArrayList<String> data_temp = new ArrayList<String>();
		ExpandableListAdapter  GSL_Adapter = GSL.getExpandableListAdapter();    	

		for(int i = 0; i < group_count ; i++) {
			//BluetoothGattCharacteristic character  =  (BluetoothGattCharacteristic) GSL_Adapter.getChild(i, 0);
			//TiSensor<?> sensor_new = TiSensors.getSensor(character.getService().getUuid().toString());
			//TiInfoService service_new = TiInfoServices.getService(character.getService().getUuid().toString());

			TiSensor<?> sensor_new = TiSensors.getSensor(((BluetoothGattService)GSL_Adapter.getGroup(i)).getUuid().toString());
			TiInfoService service_new = TiInfoServices.getService(((BluetoothGattService)GSL_Adapter.getGroup(i)).getUuid().toString());

			String temp = "";
			if(sensor_new != null)
				temp = sensor_new.getName();
			else if(service_new != null) {
				//temp = service_new.getName();
				temp = "Unnecessary";
			}
			else{
				//temp = "Unknown";
				temp = "Unnecessary";
			}
			if(!temp.equals("Unknown") && !temp.equals("Unnecessary"))  {
				data_temp.add(temp);
				data_index.add(i); 
			}


		}
		data_temp.add("GPS") ;
		int size = data_temp.size();
		data = new String[size];
		for(int i = 0; i < size ; i++) {
			data[i] = data_temp.get(i);
		}


	}


	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
		if (bleService != null) {
			final boolean result = bleService.connect(deviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(gattUpdateReceiver);
	
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopUsingGPS();
		unbindService(serviceConnection);
		bleService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
	
			
		 menu.findItem(R.id.menu_obstacle).setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_obstacle:
			bleService.connect(deviceAddress);
			addObstacles();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void addObstacles() {
		
		
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(DeviceServicesActivity.this);

		builderSingle.setTitle("Select Obstacle Type:-");
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(DeviceServicesActivity.this,android.R.layout.select_dialog_singlechoice);
		arrayAdapter.add("Stairs");
		arrayAdapter.add("Tree");
		arrayAdapter.add("Hole");
		arrayAdapter.add("Sign");
		arrayAdapter.add("Other");
		builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builderSingle.setAdapter(arrayAdapter,new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String obsType = arrayAdapter.getItem(which);
				
				if(canGetLocation){
					Location firstLoc = getFirstLocation();
					double firstLong = firstLoc.getLongitude();
					double firstLat = firstLoc.getLatitude();
					loc = firstLoc;
					longitude = firstLong;
					latitude = firstLat;
					gps_data = longitude + "\t" + latitude ; 
				
				}
				try {
					out_obs.write((obsType + "\t" + longitude + "\t" + latitude + "\n").getBytes());
					out_obs.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				writeServer(obsType, longitude, latitude);

			}
		});
		builderSingle.show();

	}
	public void createLog() {

		Intent mIntent = new Intent(this, CreateLogActivity.class);		
		startActivity(mIntent);		

	}


	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				connectionState.setText(resourceId);
			}
		});
	}

	private void displayData(String data) {   
		if (data != null) {
			dataField.setText(data);
		}
	}

	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;

		gattServiceAdapter = new TiServicesAdapter(this, gattServices);
		gattServiceAdapter.setServiceListener(demoClickListener);
		gattServicesList.setAdapter(gattServiceAdapter);
		createData(gattServicesList);
		ela = new SampleExpandableListAdapter(context, this, data);
		expandableListView.setAdapter(ela);
		expandableListView = (ExpandableListView)findViewById(R.id.expandableListView1);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
		return intentFilter;
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
			locationManager.removeUpdates(DeviceServicesActivity.this);
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
