package com.company.millenium.whosthere;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by chavdar on 1/4/16.
 */
public class LaunchReceiver extends WakefulBroadcastReceiver {

    public static final String ACTION_PULSE_SERVER_ALARM =
            "com.company.millenium.whosthere.ACTION_PULSE_SERVER_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context,
                MonitorService.class);
        context.startService(serviceIntent);
    }
}
