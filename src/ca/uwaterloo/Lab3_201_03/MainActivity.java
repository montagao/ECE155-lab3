package ca.uwaterloo.Lab3_201_03;

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Build;
import ca.uwaterloo.Lab3_201.R;
import ca.uwaterloo.Lab3_201_03.OrientationManager.GravSensorEventListener;
import ca.uwaterloo.Lab3_201_03.OrientationManager.MagSensorEventListener;
import ca.uwaterloo.sensortoy.*;
import mapper.*;

public class MainActivity extends Activity {
	
	static LineGraphView graph;
	static MapLoader mapLoader = new MapLoader();

	public Mapper map;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		map = new Mapper(getApplicationContext(),640, 600,25, 25);
		registerForContextMenu(map);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, v, menuInfo);
		map.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override 
	public boolean onContextItemSelected(MenuItem item ){
		return super.onContextItemSelected(item) || map.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public class PlaceholderFragment extends Fragment {
		

		private FileOutputStream testZStream;
		private FileOutputStream testYStream;
		private FileOutputStream stepTimeStream;
		private FileOutputStream testRStream;
		private boolean isPaused = false;

		public PlaceholderFragment() {
			try{
				testZStream = openFileOutput("testz.txt", Context.MODE_PRIVATE); 
				testYStream = openFileOutput("testy.txt", Context.MODE_PRIVATE); 
				stepTimeStream = openFileOutput("testt.txt" , Context.MODE_PRIVATE);
				testRStream = openFileOutput("testR.txt" , Context.MODE_PRIVATE);
			}
			catch (Exception e) {
			}
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.layout1);
			layout.setOrientation(LinearLayout.VERTICAL);
			
			try {
				map.setMap(mapLoader.loadMap(getExternalFilesDir(null), "E2-3344.svg"));
				
				graph = new LineGraphView(getActivity(), 100, Arrays.asList("x", "y", "z"));
				layout.addView(graph);
				graph.setVisibility(View.VISIBLE);
				
				
			} catch ( NullPointerException e){
				Log.d("exception", "null pointer!");
			}
			
			
			// using label id to add view
			//TextView lightValuesOut = (TextView) rootView.findViewById(R.id.label1);
			
			// programmatically adding labels
			TextView stepCount = addNewLabel("", rootView, layout);
			TextView accelValuesOut = addNewLabel("", rootView, layout);
			TextView orientation = addNewLabel("", rootView, layout);
			
			stepCount.setTextSize(35);
			
			// Create sensor manager and Sensor references for each applicable sensor
			final SensorManager sensorManager = (SensorManager) rootView.getContext().getSystemService(SENSOR_SERVICE);
			final Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
			final Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			final Sensor magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			
			OrientationManager orientationManager = new OrientationManager(orientation);
			
			final SensorEventListener accelListener = new AccelSensorEventListener(getApplicationContext(), accelValuesOut,stepCount,
					graph, testYStream, testZStream, stepTimeStream, testRStream,  true, orientationManager );
			
			final GravSensorEventListener gravEventListener =  orientationManager.new GravSensorEventListener();
			final MagSensorEventListener magEventListener =  orientationManager.new MagSensorEventListener();

			sensorManager.registerListener(accelListener, accelSensor, SensorManager.SENSOR_DELAY_GAME);
			sensorManager.registerListener(gravEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
			sensorManager.registerListener(magEventListener, magSensor, SensorManager.SENSOR_DELAY_GAME);
			
			// add clear button for class
			final Button clearButton = new Button(rootView.getContext());
			clearButton.setText("Clear");
			layout.addView(clearButton);
			clearButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					((AccelSensorEventListener) accelListener).clearRecords();
					((AccelSensorEventListener) accelListener).resetCounter();
				}
			});

			// add clear button for class


			final Button pauseButton = new Button(rootView.getContext());
			pauseButton.setText("Pause");

			layout.addView(pauseButton);
			pauseButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					try {
						if (!isPaused) {
							sensorManager.unregisterListener(magEventListener);
							sensorManager.unregisterListener(gravEventListener);
							sensorManager.unregisterListener(accelListener);
							pauseButton.setText("Resume");
							isPaused = true;
						} else  {
							sensorManager.registerListener(accelListener, accelSensor, SensorManager.SENSOR_DELAY_GAME);
							sensorManager.registerListener(gravEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
							sensorManager.registerListener(magEventListener, magSensor, SensorManager.SENSOR_DELAY_GAME);
							pauseButton.setText("Pause");
							isPaused = false;
						}
							
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});	
			layout.addView(map);
			map.setVisibility(View.VISIBLE);
			
			return rootView;
		
		}
		
		public TextView addNewLabel(String label,View rootView, LinearLayout layout)
		{
			TextView l = new TextView(rootView.getContext());
			l.setText(label);
			l.setTextSize((float) 15.0);
			layout.addView(l);
			
			return l;
		}
		
	}
}
