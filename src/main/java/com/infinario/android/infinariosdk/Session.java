package com.infinario.android.infinariosdk;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This file has been created by igi on 3/9/15.
 */
public class Session {

    private Preferences preferences;
    private SessionListener listener;

    class PingRunnable implements Runnable {
        public void run() {
            ping();
        }
    }

    public Session(Preferences preferences, SessionListener listener) {
        this.preferences = preferences;
        this.listener = listener;
    }

    public void run() {
        ping();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new PingRunnable(), 1, Contract.SESSION_PING_INTERVAL, TimeUnit.SECONDS);
    }

    public void restart(Map<String, String> newCustomer) {
        ping(true, newCustomer);
    }

    private synchronized void ping(boolean forceRestart, Map<String, String> newCustomer) {
        long now = (new Date()).getTime();
        long sessionStart = preferences.getSessionStart();
        long sessionEnd = preferences.getSessionEnd();

        if (sessionStart == -1) {
            preferences.setSessionStart(now);
            preferences.setSessionEnd(now);

            if (listener != null) {
                listener.onSessionStart(now);
            }
        }
        else {
            if (sessionEnd + Contract.SESSION_TIMEOUT < now || forceRestart) {
                if (listener != null) {
                    listener.onSessionEnd(sessionEnd, (sessionEnd - sessionStart) / 1000L);

                    if (forceRestart) {
                        listener.onSessionRestart(newCustomer);
                    }
                }

                preferences.setSessionStart(now);
                preferences.setSessionEnd(now);

                if (listener != null) {
                    listener.onSessionStart(now);
                }
            }
            else {
                preferences.setSessionEnd(now);
            }
        }
    }

    private void ping() {
        ping(false, null);
    }

    public Map<String, Object> defaultProperties(long duration) {
        Map<String, Object> properties = Device.deviceProperties(preferences);
        String appVersionName = preferences.getAppVersionName();
        if (appVersionName != null){
            properties.put("app_version", appVersionName);
        }

        if (duration != -1) {
            properties.put("duration", duration);
        }

        return properties;
    }

    public Map<String, Object> defaultProperties() {
        return defaultProperties(-1);
    }
}

