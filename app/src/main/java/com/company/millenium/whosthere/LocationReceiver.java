package com.company.millenium.whosthere;

/**
 * Created by chavdar on 1/4/16.
 */
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

public class LocationReceiver extends BroadcastReceiver {
    private static final String DEBUG_TAG = "LocationReceiver"; // for logging purposes

    private static LocationListener locationListener = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(DEBUG_TAG, "got a Broadcast");

        if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
            Location location = (Location) intent.getExtras().get(
                    LocationManager.KEY_LOCATION_CHANGED);
            Log.d(DEBUG_TAG, "location update by " + (location != null ? location.getProvider() : null));

        }

        if (intent.getAction() != null && intent.getAction().equals("com.company.millenium.cancelgps")) {
            Log.d(DEBUG_TAG, "cancel GPS");

            cancelUpdates(context);
        }
    }

    public static void doActiveUpdate(final Context context, Boolean useGPS) {
        Log.d(DEBUG_TAG, "force active location update");

        if (locationListener == null) {
            Log.d(DEBUG_TAG, "set up new LocationListener");

            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    Log.d(DEBUG_TAG, "locationListener: location changed. disabling myself");
                    LocationManager locationManager = (LocationManager) context
                            .getSystemService(Context.LOCATION_SERVICE);
                    // give location provider time to settle
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        // continue
                    }
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                    }
                    else {
                        locationManager.removeUpdates(this);
                    }
                    locationListener = null;
                }

                public void onProviderDisabled(String provider) {
                    // TODO Auto-generated method stub

                }

                public void onProviderEnabled(String provider) {
                    // TODO Auto-generated method stub

                }

                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {
                    // TODO Auto-generated method stub

                }
            };

            LocationManager locationManager = (LocationManager) context
                    .getSystemService(Context.LOCATION_SERVICE);

            SharedPreferences SP = PreferenceManager
                    .getDefaultSharedPreferences(context);
            if (/*SP.getBoolean("activeupdate", false) &&*/ useGPS) {
                // We want active lookup

                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Log.d(DEBUG_TAG, "GPS disabled, requesting network location update");
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.msg_gpsdisabled),
                            Toast.LENGTH_SHORT).show();
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, 0, 0,
                            locationListener);
                } else {
                    Log.d(DEBUG_TAG, "requesting GPS location update");

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 0, 0,
                            locationListener); // workaround,
                    // requestSingleUpdate: only
                    // API Versions >= 9
                }


            } else {
                // TODO: or do nothing active here?
                Log.d(DEBUG_TAG, "Active lookup disabled, requesting network location update");

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                }
                else {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, 0, 0,
                            locationListener);
                }
            }

			/* cancel updates after timeout with no fix */
            AlarmManager alarmManager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(context, LocationReceiver.class);
            i.setAction("com.company.millenium.cancelgps");
            PendingIntent pendingIntent = PendingIntent
                    .getBroadcast(context, 0, i,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 16000,
                    pendingIntent);
            Log.d(DEBUG_TAG, "cancelUpdates alarm set");
        }
        else {
            Log.d(DEBUG_TAG, "conflicting locationListener, skipping");
        }
    }

    public static void cancelUpdates(final Context context) {
        if (locationListener != null) {
            Log.d(DEBUG_TAG, "removing locationListener");

            LocationManager locationManager = (LocationManager) context
                    .getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
            }
            else {
                locationManager.removeUpdates(locationListener);
            }
            locationListener = null;

            // Do network lookup instead
            LocationReceiver.doActiveUpdate(context, false);
        }
        else {
            Log.d(DEBUG_TAG, "no locationListener present");
        }

    }


    public LocationReceiver() {
    }
}

