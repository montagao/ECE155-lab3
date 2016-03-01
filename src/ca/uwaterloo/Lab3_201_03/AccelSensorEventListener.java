package ca.uwaterloo.Lab3_201_03;

import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import ca.uwaterloo.sensortoy.LineGraphView;

class AccelSensorEventListener implements SensorEventListener {
	
		public enum stepState{
			atRest, startStep, stepPeak, stepDescent, stepRebound
		}
		
		TextView output;
		TextView stepView;
		TextView stateView;
		LineGraphView graph;
		
		
		private boolean recordStats = false;
		private boolean overflow = false; 
		
		private stepState currentState;
		private stepState currentStateOld;
		
		private FileOutputStream ZValStream;
		private FileOutputStream YValStream;
		private FileOutputStream TimeValStream;
		
		
		private int stepCount = 0;
		private int	stepCountOld = 0; 
		private long stateStartTime;
		private long timeElapsed;

		private float[] recordVals = new float[3];
		
		private float[] lowPassOut;
		
		
		private Context context;

		private String sensorString;
		private String sensorValString;
		private String sensorRecordValString = "x: 0 y: 0 z: 0";

		public AccelSensorEventListener(Context _context, TextView outputView, TextView _stepView, TextView _stateView,
				LineGraphView _graph, FileOutputStream y,FileOutputStream  z, FileOutputStream time,boolean _recordStats )
		{
			context = _context;
			recordStats = _recordStats;
			output = outputView;
			stepView = _stepView;
			stateView = _stateView;
			graph = _graph;
			currentState = stepState.atRest;
			currentStateOld = stepState.atRest;
			
			ZValStream = y;
			YValStream = z;
			TimeValStream = time;
			
			
			// Change initial label to correspond to Sensor being recorded.
			sensorString = "\nAcclerometer Reading:";
		}
		
		// Resets all record values to 0;
		public void clearRecords()
		{
			for (int i = 0; i < recordVals.length; i++)
			{
				recordVals[i] = 0;
			}
		}

		public void onAccuracyChanged(Sensor s, int i) {}
		
		public float[] lowPassFilter(float[] in, float[] out)
		{
			float a = 0.25f;
			if (out == null ) return in;
			for ( int i = 0; i <in.length; i++ ) {
				out[i] = out[i] + a *(in[i] - out[i]);
				//out[i] += (in[i] - out[i]) / a;
			}
			return out;
		}
		
		// for all sensors beside Light sensor, displays the x: y: z: values associated with it
		// and the absolute value records of each 
		
		public void onSensorChanged(SensorEvent se) {
			lowPassOut = lowPassFilter(se.values.clone(), lowPassOut);
			se.values[0] = lowPassOut[0];
			se.values[1] = lowPassOut[1];
			se.values[2] = lowPassOut[2];

			// raw data graph
			//graph.addPoint(se.values);
			
			// low pass filter graph
			graph.addPoint(lowPassOut);
			
			if (recordStats == true) {
				try {
					ZValStream.write(String.format("%.2f \n" ,lowPassOut[2]).getBytes());
					YValStream.write(String.format("%.2f \n" ,lowPassOut[1]).getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			

			// old state machine implementation, no elimation of false positives
			switch (currentStateOld)
			{
				case atRest: 
					if ( lowPassOut[2] > 0.35) {
						currentStateOld = stepState.startStep;
					}
					break;
				case startStep:
					if ( lowPassOut[2] > 1.9f ) {
						currentStateOld = stepState.stepPeak;
					}
					break;
				case stepPeak:
					if ( lowPassOut[2] < 1.9f) {
						currentStateOld = stepState.stepDescent;
					}
					break;
				case stepDescent:
					if ( lowPassOut[2] < -0.95f){
						currentStateOld = stepState.stepRebound;
					}
					break;
				case stepRebound:
					if ( lowPassOut[2] > -0.35) {
						stepCountOld++;
						currentStateOld = stepState.atRest;
					}
					break;
			}

			switch (currentState)
			{
				case atRest: 
					if ( (lowPassOut[2] > 0.35) && (Math.abs(lowPassOut[1]) > 0.1f)) {
						currentState = stepState.startStep;
						timeElapsed = System.currentTimeMillis();
					}
					stateView.setText("At rest");
					break;
				case startStep:

					if ( lowPassOut[2] < 0.35 )
						currentState = stepState.atRest;
					else if ( (lowPassOut[2] > 1.3f && lowPassOut[2] < 7) && (Math.abs(lowPassOut[1]) > 0.35f && Math.abs(lowPassOut[1]) < 2.7f )) {
						currentState = stepState.stepPeak;
					}
					stateView.setText("Startstep");
					break;
				case stepPeak:
					if ( lowPassOut[2] > 7.6f){
						overflow = true;
						currentState = stepState.stepDescent;
					}
					else if ( lowPassOut[2] < 1.3f) {
						currentState = stepState.stepDescent;
					}
					stateView.setText("stepPeak");
					break;
				case stepDescent:
					if  ( lowPassOut[2] > 1.3f){
						currentState = stepState.stepPeak;
					} else if ( lowPassOut[2] < -0.95f  && lowPassOut[1] < 0 ){
						currentState = stepState.stepRebound;
					}
					stateView.setText("step descent");
					break;
				case stepRebound:
					//if (checkTimeExceeds())
					//	break;
					if ( lowPassOut[2] > -0.35f) {
						
						// how much time has passed since startStep was initiated
						timeElapsed = System.currentTimeMillis() - timeElapsed;
						if ( timeElapsed > 90 && !overflow){
							// time for step was less than 90ms
							// unreasonable for human being, reset state without updating counter
							stepCount++;
							if (recordStats){
								try {
									TimeValStream.write(String.format("%d\n", timeElapsed).getBytes() );
								} catch (IOException e) {
									// oh dear, something went wrong!
								}
							}
						} 
						currentState = stepState.atRest;
						timeElapsed = 0;
						overflow = false;

					}
					stateView.setText("stepRebound");
					break;
			}
			
			if ( Math.abs(lowPassOut[1]) > 9.1f ) {
				currentState = stepState.atRest;
				
			}
			stepView.setText(String.format("Old Step Count: %d \nNew Step Count: %d", stepCountOld, stepCount));
		
			sensorValString = String.format("\n x: %.2f y: %.2f z: %.2f", se.values[0], se.values[1], se.values[2]);
			
			
			for (int i = 0; i < 3; i ++){
				if (Math.abs(se.values[i]) >Math.abs(recordVals[i])) {
					recordVals[i] = se.values[i];
				}
			}
			sensorRecordValString = String.format("\nRecord: x: %.2f y: %.2f z: %.2f", recordVals[0],  recordVals[1], recordVals[2]);

			output.setText(String.valueOf(sensorString+sensorValString + sensorRecordValString)); 
		}
			
		public void resetCounter()
		{
			stepCount = 0;
			stepCountOld = 0;
		}
}
