package com.infinario.android.infinariosdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Contract.TAG, "Network connectivity change");

        if (intent.getExtras() != null) {
            final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

            if (ni != null && ni.isConnectedOrConnecting()) {
                Log.i(Contract.TAG, "Network " + ni.getTypeName() + " connected");

                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                alarmIntent.putExtra(Contract.EXTRA_REQUEST_CODE, Contract.IMMEDIATE_ALARM);
                context.sendBroadcast(alarmIntent);
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                Log.d(Contract.TAG, "There's no network connectivity");
            }
        }
    }
}