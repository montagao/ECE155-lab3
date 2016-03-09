package ca.uwaterloo.Lab3_201_03;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

public class OrientationManager {
	
	private enum Direction{
		North, South, East, West;
	}
	
	private Direction currentDirection;
	private TextView directionLabel;
	private float[] R = new float[9];
	float[] Rvalues = new float[3];
	private float[] gravity = new float[3];
	private float[] mag = new float[3];
	
	public class GravSensorEventListener implements SensorEventListener {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			gravity = event.values.clone();
			OrientationManager.this.getDirection();
		
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		
	}
	
	public class MagSensorEventListener implements SensorEventListener {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			mag = event.values.clone();
			OrientationManager.this.getDirection();
			
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		
	}
	public OrientationManager( TextView directionLabel){
		this.directionLabel = directionLabel;
		
	}
	
	public float getAzimuth(){
		// return the value of the azimuth reading in radians 
		// 0 is north, positive for clockwise rotations.
		return Rvalues[0];
	}
	
	public float radToDegrees( float radian){
		return (float) (radian*(180.0d/Math.PI));
	}
	
	public OrientationManager.Direction getDirection(){
		/* returns the orientation of the phone ,
		 * after appropriate filtering 
		 * and calibrations
		 */
		SensorManager.getRotationMatrix(R, null, gravity, mag);
		SensorManager.getOrientation(R, Rvalues);
		
	

		/* updated directionLabel to current orientation */
		directionLabel.setText(String.format("Orientation: %.2f , %.2f , %.2f", Rvalues[0]*(180.0f/(3.14159)), Rvalues[1], Rvalues[2]));

		return null;
		
	}
}
