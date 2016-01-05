package com.company.millenium.whosthere;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by chavdar on 1/4/16.
 */
public class MonitorService extends Service {

    private LoggerLoadTask mTask;
    private String mPulseUrl;
    private AlarmManager alarms;
    private PendingIntent alarmIntent;
    private ConnectivityManager cnnxManager;
    private final int timeAlarm = 1 * 60 * 1000;
    private final String TAG = "MonitorService";

    @Override
    public void onCreate() {
        super.onCreate();
        cnnxManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intentOnAlarm = new Intent(
                LaunchReceiver.ACTION_PULSE_SERVER_ALARM);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentOnAlarm, 0);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        // reload our data
        if (mPulseUrl == null) {
            mPulseUrl = getString(R.string.urlPulse);
        }
        try {
            NetworkInfo ni = cnnxManager.getActiveNetworkInfo();
            if (ni != null || ni.isAvailable() || ni.isConnected()) {
                Log.d(TAG, "Im here!");
                LocationReceiver.doActiveUpdate(getApplicationContext(), true);


            }
        } catch (Exception e) {
            Log.d(TAG, "No network");
        }
        String session = getCookie(Constants.SERVER_URL, "session");
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(Constants.SERVER_URL + "update-location");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (session == null) {
                // we couldn't get a cookie, try later
                try {
                    throw new NoCookieException();
                } catch (NoCookieException e) {
                    e.printStackTrace();
                }
            }
            Log.d("TAG", getInternetData());
            conn.setRequestProperty("Cookie", "session=" + session);
            String token = getInternetData();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            String parameters = "csrf_token=" + token
                    + "longitude=" + "42.0"
                    + "latitude=" + "30.0"
                    + "accuracy=" + "100";

            outputStream.writeBytes(parameters);
            outputStream.flush();
            outputStream.close();

            //Get Response
            InputStream is = conn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            Log.d(TAG, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert conn != null;
            conn.disconnect();

        }


        executeLogger();
    }

    private void executeLogger() {
        if (mTask != null
                && mTask.getStatus() != LoggerLoadTask.Status.FINISHED) {
            return;
        }
        mTask = (LoggerLoadTask) new LoggerLoadTask().execute();
    }

    private class LoggerLoadTask extends AsyncTask<Void, Void, Void> {

        // TODO: create two base service urls, one for debugging and one for live.
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                // if we have no data connection, no point in proceeding.
                NetworkInfo ni = cnnxManager.getActiveNetworkInfo();
                if (ni == null || !ni.isAvailable() || !ni.isConnected()) {
                    return null;
                }
                // / grab and log data

            } catch (Exception e) {
                Log.d(TAG, "connection error");
            } finally {
                // always set the next wakeup alarm.
                int interval = 10;
                long timeToAlarm = SystemClock.elapsedRealtime() + interval
                        * 1000;
                alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToAlarm,
                        alarmIntent);
            }
            return null;
        }
    }

    public String getCookie(String siteName, String CookieName) {
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        String cookies = cookieManager.getCookie(siteName);
        if (cookies != null) {
            String[] temp = cookies.split(";");
            for (String ar1 : temp) {
                if (ar1.contains(CookieName)) {
                    String[] temp1 = ar1.split("=");
                    return temp1[1];
                }
            }
        }
        return null;
    }

    public String getInternetData() throws Exception {
        URL url = new URL(Constants.SERVER_URL + "csrf-token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.getDoInput();
        InputStream inputStream = conn.getInputStream();
        conn.disconnect();
        return inputStream.toString();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
