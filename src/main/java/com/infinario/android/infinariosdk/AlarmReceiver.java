package com.infinario.android.infinariosdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Contract.TAG, "Wake from " + intent.getExtras().getInt(Contract.EXTRA_REQUEST_CODE));

        if (Infinario.automaticFlushing(context)) {
            Log.d(Contract.TAG, "Flushing data");
            Infinario.flush(context);
        }
        else {
            Log.d(Contract.TAG, "Cancelling alarm");
            Infinario.cancelPeriodicAlarm(context);
        }
    }
}