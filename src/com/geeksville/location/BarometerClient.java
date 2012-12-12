/*******************************************************************************
 * Gaggle is Copyright 2010 by Geeksville Industries LLC, a California limited liability corporation. 
 * 
 * Gaggle is distributed under a dual license.  We've chosen this approach because within Gaggle we've used a number
 * of components that Geeksville Industries LLC might reuse for commercial products.  Gaggle can be distributed under
 * either of the two licenses listed below.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. 
 * 
 * Commercial Distribution License
 * If you would like to distribute Gaggle (or portions thereof) under a license other than 
 * the "GNU General Public License, version 2", contact Geeksville Industries.  Geeksville Industries reserves
 * the right to release Gaggle source code under a commercial license of its choice.
 * 
 * GNU Public License, version 2
 * All other distribution of Gaggle must conform to the terms of the GNU Public License, version 2.  The full
 * text of this license is included in the Gaggle source, see assets/manual/gpl-2.0.txt.
 ******************************************************************************/
package com.geeksville.location;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.geeksville.gaggle.GaggleApplication;
import com.geeksville.location.baro.DummyBarometerClient;

/// FIXME - add a basic vario http://www.paraglidingforum.com/viewtopic.php?p=48465
public class BarometerClient extends Observable implements SharedPreferences.OnSharedPreferenceChangeListener, IBarometerClient, Observer {

  @SuppressWarnings("unused")
  private static final String TAG = "BarometerClient";

  private IBarometerClient baroClient = null;
  private int baroClientType;

	private Context context;

	private static BarometerClient instance;

	private BarometerClient(Context context) {
		this.context = context;
		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(this);
		create();
	}
	
	public static BarometerClient initInstance(){
		return getInstance();
	}

	public static BarometerClient getInstance() {
		if (instance == null) {
			instance = new BarometerClient(GaggleApplication.getContext());
		}
		return instance;
	}
  
  /**
   * All users of barometer share the same (expensive) instance
   * 
   * @return null for if not available
   */
  public IBarometerClient create() {

    SensorClient.initManager(context);

	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

	final String vario_source = prefs.getString("vario_source", null);
	int vario_src;

	if (vario_source == null){
		return null;
	} else {
		vario_src = Integer.parseInt(vario_source);
	}
	
	
	// if barometer already exists: remember all it's observers:
	boolean barometerChanged = false;
	if (baroClient != null && baroClientType != vario_src){
		barometerChanged = true;
		baroClient.deleteObserver(this);
	}
	
	// (re)instantiate the varioclient:
	if (baroClient == null || baroClientType != vario_src){
		barometerChanged = true;
		switch (vario_src){
		case 0:
			if (AndroidBarometerClient.isAvailable()){
				baroClient = new AndroidBarometerClient(context);
				baroClientType = vario_src;
			}
			break;
		case 1: //CNES
			if (CNESBarometerClient.isAvailable()){
				baroClient = new CNESBarometerClient(context);
				baroClientType = vario_src;
			}
			break;
		case 2:
			// FlyNet
			if (FlynetBarometerClient.isAvailable()){
				baroClient = new FlynetBarometerClient(context);
				baroClientType = vario_src;
			}
			break;
		case 3:
			// Test BT
			break;
		case 4:
			baroClient = new DummyBarometerClient(context);
			baroClientType = vario_src;
			break;
		}
	}
	
	// if teh barometer has changed (and there were observers on the old baroclient): resassign them to the new one:
	if(barometerChanged && baroClient !=null){
		Observable client = (Observable) baroClient;
		// reassign the observers
		client.addObserver(this);
	}
	return baroClient;
  }
  
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		create();
	}

	public IBarometerClient getBaroClient() {
		return baroClient;
	}

	@Override
	public void setAltitude(float meters) {
		if(getBaroClient()!=null){
			getBaroClient().setAltitude(meters);
		}		
	}

	@Override
	public float getAltitude() {
		if(getBaroClient()!=null){
			return getBaroClient().getAltitude();
		}		
		return 0;
	}

	@Override
	public float getPressure() {
		if(getBaroClient()!=null){
			return getBaroClient().getPressure();
		}		
		return 0;
	}

	@Override
	public float getBattery() {
		if(getBaroClient()!=null){
			getBaroClient().getBattery();
		}		
		return 0;
	}

	@Override
	public float getBatteryPercent() {
		if(getBaroClient()!=null){
			getBaroClient().getBatteryPercent();
		}		
		return 0;
	}

	@Override
	public String getStatus() {
		if(getBaroClient()!=null){
			getBaroClient().getStatus();
		}		
		return null;
	}

	@Override
	public float getVerticalSpeed() {
		if(getBaroClient()!=null){
			return getBaroClient().getVerticalSpeed();
		}		
		return 0;
	}

	@Override
	public void improveLocation(Location l) {
		if(getBaroClient()!=null){
			getBaroClient().improveLocation(l);
		}		
	}

	@Override
	public void update(Observable observable, Object data) {
		setChanged();
		notifyObservers(data);
	}

}
