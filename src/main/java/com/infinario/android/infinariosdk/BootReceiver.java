package com.infinario.android.infinariosdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            if (Preferences.get(context).getAutomaticFlushing()) {
                Log.d(Contract.TAG, "Setting up alarm after a reboot");
                Infinario.setupPeriodicAlarm(context);
            }
            else {
                Log.d(Contract.TAG, "Cancelling alarm after a reboot");
                Infinario.cancelPeriodicAlarm(context);
            }
        }
    }
}