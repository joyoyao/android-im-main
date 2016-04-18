package com.ultrapower.mcs.engine.video;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

import java.util.ArrayList;

public class OrientationChangedNotify {
	private static OrientationChangedNotify _self = new OrientationChangedNotify();
	private   OrientationEventListener _OrientationEventListener;
	private  int _lastOrientation;
	private  ArrayList<IOrientationChangedObserver> _observers = new ArrayList<IOrientationChangedObserver>(); 
	public static OrientationChangedNotify Instances()
	{
		return _self;
	}
	protected synchronized void FireOrientationChanged(int orientation) {
	if(orientation>315||orientation<=45)
   	 {
   		 orientation = 0;
   	 }
   	 else if(orientation>45&&orientation<=135)
   	 {
   		 orientation = 90;
   	 }
   	 else if (orientation>135&&orientation<=225) {
			orientation = 180;
		}
   	 else 
   	 {
   		 orientation = 270;
   	 }
   	 int realOrientation = 90*Math.round(orientation / 90); 

   	 // Convert 360 to 0
   	 if(realOrientation == 360)
   	 {
   		 realOrientation = 0;
   	 }
   	 if (_lastOrientation!=realOrientation) {
   		 for (IOrientationChangedObserver observer : _observers) {
				observer.OnOrientationChanged(realOrientation);
			}
		}
   	 _lastOrientation = realOrientation;
	}
	public  synchronized void AddObserver(IOrientationChangedObserver observer)
	{
		if (observer==null) {
			return;
		}
		if (_observers.contains(observer)) {
			return;
		}
		_observers.add(observer);
		observer.OnOrientationChanged(_lastOrientation);
	}
	public synchronized void RemoveObserver(IOrientationChangedObserver observer)
	{
		if (observer==null) {
			return;
		}
		if (_observers.contains(observer)) {
			_observers.remove(observer);
		}
	}
	public  synchronized void Start(Context context)
	{
		if (_OrientationEventListener==null) {
			_OrientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) 
		        { 
		         @Override
		         public void onOrientationChanged(int orientation) 
		         {
		        	// Divide by 90 into an int to round, then multiply out to one of 5 positions, either 0,90,180,270,360. 
		        	 FireOrientationChanged(orientation);
		         }
		        };
		}
	    if(_OrientationEventListener.canDetectOrientation())
	    {
	        _OrientationEventListener.enable();
	    }
	}
	public synchronized  void Stop()
	{		
		if (_OrientationEventListener!=null) {
			_OrientationEventListener.disable();
		}
		_observers.clear();
		_OrientationEventListener = null;
	}
}
