package com.infinario.android.infinariosdk;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public abstract class SessionListener {
    abstract void onSessionStart(long timestamp);
    abstract void onSessionEnd(long timestamp, long duration);
    abstract void onSessionRestart(Map<String, String> newCustomer);
}
