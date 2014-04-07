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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
public class DeviceServicesActivity extends Activity {
    private final static String TAG = DeviceServicesActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
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
    private BleService bleService;
    private boolean isConnected = false;

    private TiSensor<?> activeSensor;
    private GPSService gps;
    double latitude ;
    double longitude ;
    private Location loc;
    static String gps_data = "";
    static String acc_data = "";
    static String mag_data = "";
    private boolean gps_enabled = false;
    String [] data;
    ArrayList <Integer> active_index = new ArrayList<Integer>();
    static String log_data = "";
    private LinkedList <TiSensor<?> > activeSensors = new LinkedList<TiSensor<?>>();
    
    
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
            	String [] coordinates = new String [3];
            
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
            		log_data += System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + acc_data + "\n";
            	}
            	
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
            		log_data += System.currentTimeMillis() + "\t" + sensor.toUpperCase() + "\t" + mag_data + "\n";
            	}
            	
            	String temp_gps_data ;
            	if(gps.canGetLocation() && gps_enabled){
	                    
	                   latitude = gps.getLatitude();
	                   longitude = gps.getLongitude();
	                   // Location loc can be used if necessary
	                   loc = gps.getLocation();
	                   temp_gps_data = longitude + "\t" + latitude ; 

	            }else{
	            	   temp_gps_data = gps_data ;
	            }
            	if(!temp_gps_data.equals(gps_data)) {
            		gps_data = temp_gps_data;	
            		log_data += System.currentTimeMillis() + "\t" + "GPS" + "\t" + gps_data + "\n";
            	}
            	
            	
            	
                displayData(temp);
            }
        }
    };

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
    				
    				for(TiSensor<?> sensor : activeSensors){
    					if(sensor != null)
    						bleService.enableSensor(sensor, false);
    				}
    				activeSensors.clear();
    				active_index.clear();
    				
    				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);  
    				String buffer = null; 		    
    				int size = data.length;
    				for (int i = 0; i < size; i++) {
    					buffer = settings.getString(String.valueOf((int)i),"false");
    					if(buffer.equals("true"))
    						if(i != size-1)
    							active_index.add(i);
    						else if(i == size-1){
    							
    							gps_enabled = true;
    							  if(gps.canGetLocation()){
    				                    
    				                   latitude = gps.getLatitude();
    				                   longitude = gps.getLongitude();
    				                   // Location loc can be used if necessary
    				                   loc = gps.getLocation();
    				                   gps_data = longitude + "\t" + latitude ; 
    				               }else{
    				            	   gps_data = "No data!" ; 
    				               }
    							  log_data += System.currentTimeMillis() + "\t" + "GPS" + "\t" + gps_data + "\n";
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


        gps = new GPSService(this);
        if(!gps.canGetLocation()){
        	gps.showSettingsAlert();
        }   

      
       

       
      
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        ((TextView) findViewById(R.id.device_address)).setText(deviceAddress); //+
        gattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        gattServicesList.setOnChildClickListener(servicesListClickListner);
        gattServicesList.setVisibility(-1);
        
      
    
        expandableListView = (ExpandableListView)findViewById(R.id.expandableListView1);
  
       
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
     		   createLog();
     		   
     	   }
        });
        
        

        
        connectionState = (TextView) findViewById(R.id.connection_state);   
        dataField = (TextView) findViewById(R.id.data_value);  

        getActionBar().setTitle(deviceName);   
        getActionBar().setDisplayHomeAsUpEnabled(true); 

        final Intent gattServiceIntent = new Intent(this, BleService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }
    private String [] createData(ExpandableListView GSL) {
    	

   
    	int group_count = GSL.getCount();
     	
    	
    	String [] data = new String [group_count+1];
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
    			else if(service_new != null)
    				temp = service_new.getName();
    			else
    				temp = "Unknown";
    			
    			data[i] = temp;
    			
    		
    	}
    	data[group_count] = "GPS";
    	for(String s: data)
    		Log.i("a", s);
    	
    	
    	return data;
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
        gps.stopUsingGPS();
        unbindService(serviceConnection);
        bleService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (isConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                bleService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                bleService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
 
    public void createLog() {
    	
    	Intent mIntent = new Intent(this, CreateLogActivity.class);
    	mIntent.putExtra("Log_Data", log_data);
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

    private void displayData(String data) {   // bunu koymadm
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
        data = createData(gattServicesList);
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
}
