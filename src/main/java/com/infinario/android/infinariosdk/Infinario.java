package com.infinario.android.infinariosdk;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This file has been created by Igor Liska on 1/8/15.
 */
public class Infinario {
    private static Infinario instance = null;

    private GoogleCloudMessaging gcm = null;
    private String token;
    private String registrationId;
    private Map<String, String> customer;
    private boolean identified = false;
    private CommandManager commandManager;
    private final Context context;
    private int commandCounter = Contract.FLUSH_COUNT;

    private Infinario(Context context, String token, String target, Map<String, String> customer) {
        this.token = token;
        this.context = context;

        if (null != target) {
            storeTarget(target.replaceFirst("/*$", ""));
        }

        commandManager = new CommandManager(context, target);

        if (automaticFlushing(context)) {
            setupPeriodicAlarm(context);
        }

        if (customer != null) {
            identify(customer);
        }
    }

    /**
     * Obtains instance of Infinario instance to work with and identifies the customer.
     *
     * @param context  application's context
     * @param token    company token obtained from Infinario admin
     * @param target   Infinario API location
     * @param customer key-value ids (cookie ID or registered ID)
     * @return Infinario instance
     */
    @SuppressWarnings("unused")
    public static Infinario getInstance(Context context, String token, String target, Map<String, String> customer) {
        if (instance == null) {
            instance = new Infinario(context, token, target, customer);
        }

        return instance;
    }

    /**
     * Obtains instance of Infinario instance to work with and identifies the customer.
     *
     * @param context  application's context
     * @param token    company token obtained from Infinario admin
     * @param target   Infinario API location
     * @param customer customer's registered ID
     * @return Infinario instance
     */
    @SuppressWarnings("unused")
    public static Infinario getInstance(Context context, String token, String target, String customer) {
        return getInstance(context, token, target, translateId(customer));
    }

    /**
     * Obtains instance of Infinario instance to work with.
     *
     * @param context  application's context
     * @param token    company token obtained from Infinario admin
     * @param target   Infinario API location
     * @return Infinario instance
     */
    @SuppressWarnings("unused")
    public static Infinario getInstance(Context context, String token, String target) {
        return getInstance(context, token, target, (Map<String, String>) null);
    }

    /**
     * Obtains instance of Infinario instance to work with and identifies the customer.
     *
     * @param context  application's context
     * @param token    company token obtained from Infinario admin
     * @param customer key-value ids (cookie ID or registered ID)
     * @return Infinario instance
     */
    @SuppressWarnings("unused")
    public static Infinario getInstance(Context context, String token, Map<String, String> customer) {
        return getInstance(context, token, null, customer);
    }

    /**
     * Obtains instance of Infinario instance to work with.
     *
     * @param context  application's context
     * @param token    company token obtained from Infinario admin
     * @return Infinario instance
     */
    @SuppressWarnings("unused")
    public static Infinario getInstance(Context context, String token) {
        return getInstance(context, token, null, (Map<String, String>) null);
    }

    /**
     * Identifies a customer with their registered ID or cookie ID. If no cookie ID is
     * supplied, it is generated automatically.
     *
     * @param customer key-value ids (cookie ID or registered ID)
     * @param properties key-value customer's properties
     */
    @SuppressWarnings("unused")
    public void identify(Map<String, String> customer, Map<String, Object> properties) {
        this.customer = customer;

        if (!customer.containsKey(Contract.COOKIE)) {
            customer.put(Contract.COOKIE, getCookieId());
        }

        identified = true;
        update(properties);
    }

    /**
     * Identifies a customer with their registered ID.
     *
     * @param customer customer's registered ID
     * @param properties key-value customer's properties
     */
    @SuppressWarnings("unused")
    public void identify(String customer, Map<String, Object> properties) {
        identify(translateId(customer), properties);
    }

    /**
     * Identifies a customer with their registered ID or cookie ID. If no cookie ID is
     * supplied, it is generated automatically.
     *
     * @param customer key-value ids (cookie ID or registered ID)
     */
    @SuppressWarnings("unused")
    public void identify(Map<String, String> customer) {
        identify(customer, new HashMap<String, Object>());
    }

    /**
     * Identifies a customer with their registered ID.
     *
     * @param customer customer's registered ID
     */
    @SuppressWarnings("unused")
    public void identify(String customer) {
        identify(customer, new HashMap<String, Object>());
    }

    /**
     * Updates customer's properties. Cannot be called prior to the identification.
     *
     * @param properties key-value customer's properties.
     * @return success of the operation
     */
    public boolean update(Map<String, Object> properties) {
        if (!identified) {
            Log.e(Contract.TAG, "Cannot update customer's properties prior to the identification.");
            return false;
        }

        if (commandManager.schedule(new Customer(customer, token, properties))) {
            if (automaticFlushing(context)) {
                setupDelayedAlarm();
            }

            return true;
        }

        return false;
    }

    /**
     * Tracks an event for a customer. Cannot be called prior to the identification.
     *
     * @param type       event's type
     * @param properties event's properties
     * @param timestamp  event's timestamp in seconds
     * @return success of the operation
     */
    public boolean track(String type, Map<String, Object> properties, Long timestamp) {
        if (!identified) {
            Log.e(Contract.TAG, "Cannot track an event prior to the identification.");
            return false;
        }

        if (commandManager.schedule(new Event(customer, token, type, properties, timestamp))) {
            if (automaticFlushing(context)) {
                setupDelayedAlarm();
            }

            return true;
        }

        return false;
    }

    /**
     * Tracks an event for a customer. Cannot be called prior to the identification.
     *
     * @param type event's type
     * @return success of the operation
     */
    public boolean track(String type) {
        return track(type, null, null);
    }

    /**
     * Tracks an event for a customer. Cannot be called prior to the identification.
     *
     * @param type       event's type
     * @param properties event's properties
     * @return success of the operation
     */
    public boolean track(String type, Map<String, Object> properties) {
        return track(type, properties, null);
    }

    /**
     * Tracks an event for a customer. Cannot be called prior to the identification.
     *
     * @param type event's type
     * @param timestamp event's timestamp in seconds
     * @return success of the operation
     */
    public boolean track(String type, Long timestamp) {
        return track(type, null, timestamp);
    }

    /**
     * Flushes the updates / events to Infinario API asynchronously.
     */
    public static void flush(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (isConnected(context)) {
                    CommandManager commandManager = new CommandManager(context, getTarget(context));
                    commandManager.flush();
                    setConnectivityMonitor(context, false);
                }
                else {
                    setConnectivityMonitor(context, true);
                }

                return null;
            }
        }.execute(null, null, null);
    }

    /**
     * Flushes the updates / events to Infinario API asynchronously.
     */
    @SuppressWarnings("unused")
    public void flush() {
        flush(context);
    }

    /**
     * Sets up periodic alarm for automatic flushing of the events. Default interval
     * is set to 6 hours. Periodic alarm doesn't wake the device (alarm type is RTC)
     * for better battery saving.
     *
     * @param context application's context
     */
    public static void setupPeriodicAlarm(Context context) {
        getAlarmManager(context).setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + Contract.UPDATE_INTERVAL,
                Contract.UPDATE_INTERVAL,
                getAlarmIntent(context, Contract.PERIODIC_ALARM));
    }

    /**
     * Cancels periodic alarm if it is enabled, otherwise does nothing.
     *
     * @param context application's context
     */
    public static void cancelPeriodicAlarm(Context context) {
        getAlarmManager(context).cancel(getAlarmIntent(context, Contract.PERIODIC_ALARM));
    }

    /**
     * Enables automatic flushing of the events / updates to Infinario.
     */
    public void enableAutomaticFlushing() {
        setAutomaticFlushing(context, true);
        setupPeriodicAlarm(context);
    }

    /**
     * Disables automatic flushing of the events / updates to Infinario.
     */
    @SuppressWarnings("unused")
    public void disableAutomaticFlushing() {
        setAutomaticFlushing(context, false);
        cancelPeriodicAlarm(context);
    }

    /**
     * Enables receiving of push notifications to the app from an Infinario scenario. Push
     * notifications cannot be enabled prior to the identification.
     *
     * @param senderId sender ID or project number obtained from Google Developers Console
     * @param iconDrawable icon for the notifications, e.g. R.drawable.icon
     */
    @SuppressWarnings("unused")
    public void enablePushNotifications(String senderId, int iconDrawable) {
        if (!identified) {
            Log.e(Contract.TAG, "Cannot enable push notifications prior to the identification.");
            return;
        }

        setPushNotification(context, true);

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices(context)) {
            gcm = GoogleCloudMessaging.getInstance(context);
            registrationId = getRegistrationId();

            storeSenderId(senderId);
            storeIcon(iconDrawable);

            if (registrationId.isEmpty()) {
                registerInBackground();
            } else {
                Log.i(Contract.TAG, "Already registered");
            }
        }
        else {
            Log.i(Contract.TAG, "No valid Google Play Services APK found.");
        }
    }

    /**
     * Enables receiving of push notifications to the app from an Infinario scenario.
     *
     * @param senderId sender ID or project number obtained from Google Developers Console
     */
    @SuppressWarnings("unused")
    public void enablePushNotifications(String senderId) {
        enablePushNotifications(senderId, R.drawable.infinario_notification_icon);
    }

    /**
     * Disables receiving of push notifications to the app from an Infinario scenario.
     */
    @SuppressWarnings("unused")
    public void disablePushNotifications() {
        setPushNotification(context, false);
    }

    /**
     * Handles intent (GCM message) from the GcmBroadcastReceiver for all Infinario instances.
     *
     * @param context application's context
     * @param intent received intent from a broadcast receiver.
     */
    public static void handlePushNotification(Context context, Intent intent) {
        if (pushNotifications(context) && checkPlayServices(context)) {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            Bundle extras = intent.getExtras();
            String messageType = gcm.getMessageType(intent);
            String senderId = getSenderId(context);

            if (!extras.isEmpty() &&
                    senderId != null &&
                    !senderId.equals("") &&
                    GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType) &&
                    extras.getString("from").equals(senderId)) {

                Log.d(Contract.TAG, "Received data: " + intent.getExtras().toString());
                sendNotification(context, intent.getExtras(), getIcon(context));
            }
        }
    }

    /**
     * Checks the state of automatic flushing.
     *
     * @param context application's context
     * @return true if flushing is enabled, false otherwise
     */
    public static boolean automaticFlushing(Context context) {
        return getPreferences(context).getBoolean(Contract.PROPERTY_AUTO_FLUSH, Contract.DEFAULT_AUTO_FLUSH);
    }

    /**
     * Checks the state of push notifications.
     *
     * @param context application's context
     * @return true if push notifications are enabled, false otherwise
     */
    public static boolean pushNotifications(Context context) {
        return getPreferences(context).getBoolean(Contract.PROPERTY_PUSH_NOTIFICATIONS, Contract.DEFAULT_PUSH_NOTIFICATIONS);
    }

    /**
     * Clears cached information from device's memory (registration id, app version,
     * cookie ID)
     */
    public void clearStoredData() {
        getPreferences(context).edit()
                .remove(Contract.PROPERTY_APP_VERSION)
                .remove(Contract.PROPERTY_REG_ID)
                .remove(Contract.COOKIE)
                .commit();
    }

    /**
     * Translates String registered ID to Map<String, String> compound ID
     * @param customer customer's registered ID
     * @return key-value customer IDs
     */
    private static Map<String, String> translateId(String customer) {
        Map<String, String> customer_ids = new HashMap<>();
        customer_ids.put(Contract.REGISTERED, customer);
       return customer_ids;
    }

    /**
     * Sets up delayed alarm for automatic flushing of events. Each call to {@code track()} or
     * {@code update()} resets the delayed alarm to accumulate several events and updates together
     * to send them in a single batch. Delayed alarm fires off if there is more than 10
     * seconds between two calls of {@code track()} or {@code update()} or the two
     * mentioned methods are called more than 50 times in a row.
     *
     * Delayed alarm is disabled if {@code automaticFlushing()} returns false.
     */
    private void setupDelayedAlarm() {
        PendingIntent alarmIntent = getAlarmIntent(context, Contract.DELAYED_ALARM);

        if (commandCounter > 0) {
            commandCounter--;

            getAlarmManager(context).set(AlarmManager.RTC,
                    System.currentTimeMillis() + Contract.FLUSH_DELAY,
                    alarmIntent);
        }
        else {
            commandCounter = Contract.FLUSH_COUNT;
            context.sendBroadcast(getIntent(context, Contract.IMMEDIATE_ALARM));
        }
    }

    /**
     * Prepares alarm intent.
     *
     * @param context application's context
     * @param requestCode {@code Contract.PERIODIC_ALARM}, {@code Contract.IMMEDIATE_ALARM} or {@code Contract.DELAYED_ALARM}
     * @return intent for AlarmReceiver
     */
    private static Intent getIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Contract.EXTRA_REQUEST_CODE, requestCode);
        return intent;
    }

    /**
     * Prepares alarm pending intent.
     *
     * @param context application's context
     * @param requestCode {@code Contract.PERIODIC_ALARM}, {@code Contract.IMMEDIATE_ALARM} or {@code Contract.DELAYED_ALARM}
     * @return pending intent for AlarmReceiver
     */
    private static PendingIntent getAlarmIntent(Context context, int requestCode) {
        return PendingIntent.getBroadcast(context, requestCode, getIntent(context, requestCode), 0);
    }

    /**
     * Gets Alarm Manager.
     *
     * @param context application's context
     * @return alarm manager instance
     */
    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK.
     *
     * @param context application's context
     */
    private static boolean checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.i(Contract.TAG, "This device is not supported.");
            return false;
        }

        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param registrationId registration ID
     */
    @SuppressLint("CommitPrefEdits")
    private void storeRegistrationId(String registrationId) {
        int appVersion = getAppVersion(context);
        Log.i(Contract.TAG, "Saving regId on app version " + appVersion);

        getPreferences(context)
                .edit()
                .putString(Contract.PROPERTY_REG_ID, registrationId)
                .putInt(Contract.PROPERTY_APP_VERSION, appVersion)
                .commit();
    }

    /**
     * Stores sender ID in preferences.
     *
     * @param senderId sender ID or project number obtained from Google Developers Console
     */
    private void storeSenderId(String senderId) {
        getPreferences(context).edit().putString(Contract.PROPERTY_SENDER_ID, senderId).commit();
    }

    /**
     * Stores notification icon preferences.
     *
     * @param iconDrawable icon for the notifications, e.g. R.drawable.icon
     */
    private void storeIcon(int iconDrawable) {
        getPreferences(context).edit().putInt(Contract.PROPERTY_ICON, iconDrawable).commit();
    }

    /**
     * Stores target (Infinario API location) in preferences.
     *
     * @param target Infinario API location
     */
    private void storeTarget(String target) {
        getPreferences(context).edit().putString(Contract.PROPERTY_TARGET, target).commit();
    }

    /**
     * Stores status of automatic flushing in preferences.
     *
     * @param context application's context
     * @param value enabled / disabled
     */
    private static void setAutomaticFlushing(Context context, boolean value) {
        getPreferences(context).edit().putBoolean(Contract.PROPERTY_AUTO_FLUSH, value).commit();
    }

    /**
     * Stores status of push notifications in preferences.
     *
     * @param context application's context
     * @param value enabled / disabled
     */
    private static void setPushNotification(Context context, boolean value) {
        getPreferences(context).edit().putBoolean(Contract.PROPERTY_PUSH_NOTIFICATIONS, value).commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    private String getRegistrationId() {
        final SharedPreferences prefs = getPreferences(context);
        String registrationId = prefs.getString(Contract.PROPERTY_REG_ID, "");

        if (registrationId.isEmpty()) {
            Log.i(Contract.TAG, "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(Contract.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);

        if (registeredVersion != currentVersion) {
            Log.i(Contract.TAG, "App version changed.");
            return "";
        }

        return registrationId;
    }

    /**
     * Gets cookie ID from preferences.
     *
     * @return cookie ID
     */
    @SuppressLint("CommitPrefEdits")
    private String getCookieId() {
        final SharedPreferences prefs = getPreferences(context);
        String cookieId = prefs.getString(Contract.COOKIE, "");

        if (cookieId.isEmpty()) {
            cookieId = UUID.randomUUID().toString();
            prefs.edit().putString(Contract.COOKIE, cookieId).commit();
        }

        return cookieId;
    }

    /**
     * Gets sender ID from preferences.
     *
     * @param context application's context
     * @return sender ID or project number obtained from Google Developers Console
     */
    private static String getSenderId(Context context) {
        return getPreferences(context).getString(Contract.PROPERTY_SENDER_ID, null);
    }

    /**
     * Gets target (Infinario API location) from preferences.
     *
     * @param context application's context
     * @return Infinario API location
     */
    private static String getTarget(Context context) {
        return getPreferences(context).getString(Contract.PROPERTY_TARGET, null);
    }

    /**
     * Gets icon from preferences.
     *
     * @param context application's context
     * @return icon resource
     */
    private static int getIcon(Context context) {
        return getPreferences(context).getInt(Contract.PROPERTY_ICON, R.drawable.infinario_notification_icon);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    registrationId = gcm.register(getSenderId(context));

                    sendRegistrationIdToBackend();
                    storeRegistrationId(registrationId);
                } catch (IOException ex) {
                    Log.e(Contract.TAG, "Error :" + ex.getMessage());
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }

                return null;
            }
        }.execute(null, null, null);
    }

    /**
     * Gets app's version from preferences.
     *
     * @param context application's context.
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @param context application's context.
     * @return Application's {@code SharedPreferences}.
     */
    private static SharedPreferences getPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(Contract.PROPERTY, Context.MODE_PRIVATE);
    }

    /**
     * Sends the registration ID to the server over HTTP, so it can send
     * messages to the app.
     */
    private void sendRegistrationIdToBackend() {
        Log.i(Contract.TAG, "Sending registration ID to backend");
        Map<String, Object> properties = new HashMap<>();
        properties.put(Contract.DB_REGISTRATION_ID, registrationId);
        update(properties);
    }

    /**
     * Displays a notification (on locked screen or in notification area).
     *
     * @param context application's context
     * @param data data from intent
     * @param iconDrawable icon for the notifications, e.g. R.drawable.icon
     */
    private static void sendNotification(Context context, Bundle data, int iconDrawable) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        if (intent != null) {
            intent.putExtras(data);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            String message = data.getString("message");

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(iconDrawable)
                            .setContentTitle(data.getString("title"))
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                            .setContentText(message)
                            .setAutoCancel(true);

            builder.setContentIntent(contentIntent);
            notificationManager.notify(Contract.NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Checks connectivity to Infinario API.
     *
     * @param context application's context
     * @return true if there is connectivity to Infinario API, false otherwise
     */
    private static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                URL url = new URL(getTarget(context) + Contract.PING_TARGET);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-Agent", "test");
                urlConnection.setRequestProperty("Connection", "close");
                urlConnection.setConnectTimeout(1000);
                urlConnection.connect();

                return (200 == urlConnection.getResponseCode());
            } catch (IOException e) {
                Log.i(Contract.TAG, "Error while checking the internet connection", e);
                return false;
            }
        }

        return false;
    }

    /**
     * Enables / disables broadcast receiver for monitoring network state changes.
     *
     * @param context application's context
     */
    private static void setConnectivityMonitor(Context context, boolean enabled) {
        ComponentName receiver = new ComponentName(context, ConnectivityReceiver.class);
        PackageManager packageManager = context.getPackageManager();

        packageManager.setComponentEnabledSetting(receiver,
                (enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
                PackageManager.DONT_KILL_APP);
    }
}
