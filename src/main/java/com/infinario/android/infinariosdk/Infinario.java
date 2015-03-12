package com.infinario.android.infinariosdk;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
    private Preferences preferences;
    private Session session = null;
    private IabHelper iabHelper = null;

    private Infinario(Context context, String token, String target, Map<String, String> customer) {
        this.token = token;
        this.context = context.getApplicationContext();

        preferences = Preferences.get(context);
        preferences.setToken(token);

        if (null != target) {
            preferences.setTarget(target.replaceFirst("/*$", ""));
        }

        commandManager = new CommandManager(context, target);

        if (preferences.getAutomaticFlushing()) {
            setupPeriodicAlarm(context);
        }

        iabHelper = new IabHelper(context);
        iabHelper.startSetup(null);

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
        customer.put(Contract.COOKIE, preferences.getCookieId());

        if (session == null) {
            this.customer = customer;
            identified = true;
            setupSession();
        }
        else {
            session.restart(customer);
        }

        Map<String, Object> identificationProperties = Device.deviceProperties();

        if (customer.containsKey(Contract.REGISTERED)) {
            identificationProperties.put(Contract.REGISTERED, customer.get(Contract.REGISTERED));
        }

        track("identification", identificationProperties);

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
            if (preferences.getAutomaticFlushing()) {
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
            if (preferences.getAutomaticFlushing()) {
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

    public void trackPurchases(int resultCode, Intent data) {
        if (!iabHelper.setupDone() || data == null) {
            return;
        }

        int responseCode = IabHelper.getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

        if (resultCode == Activity.RESULT_OK && responseCode == 0) {
            if (purchaseData == null) {
                Log.d(Contract.TAG, "purchaseData is null");
                return;
            }

            try {
                JSONObject o = new JSONObject(purchaseData);
                final String productId = o.optString("productId");
                final Long purchaseTime = o.optLong("purchaseTime");

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Log.d(Contract.TAG, "Purchased item " + productId + " at " + purchaseTime);

                        SkuDetails details;

                        try {
                            details = iabHelper.querySkuDetails("inapp", productId);

                            if (details == null && iabHelper.subscriptionsSupported()) {
                                details = iabHelper.querySkuDetails("subs", productId);
                            }
                        }
                        catch (RemoteException | JSONException e) {
                            return null;
                        }

                        if (details != null) {
                            Map<String, Object> properties = Device.deviceProperties();

                            properties.put("brutto", details.getPrice());
                            properties.put("currency", details.getCurrency());
                            properties.put("item_id", productId);
                            properties.put("item_title", details.getTitle());

                            track("hard_purchase", properties, purchaseTime);
                        }

                        return null;
                    }
                }.execute(null, null, null);
            }
            catch (JSONException e) {
                Log.e(Contract.TAG, "Cannot parse purchaseData");
            }
        }
    }

    /**
     * Flushes the updates / events to Infinario API asynchronously.
     */
    public static void flush(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (isConnected(context)) {
                    CommandManager commandManager = new CommandManager(context, Preferences.get(context).getTarget());
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
    @SuppressWarnings("UnusedDeclaration")
    public void enableAutomaticFlushing() {
        preferences.setAutomaticFlushing(true);
        setupPeriodicAlarm(context);
    }

    /**
     * Disables automatic flushing of the events / updates to Infinario.
     */
    @SuppressWarnings("unused")
    public void disableAutomaticFlushing() {
        preferences.setAutomaticFlushing(false);
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

        preferences.setPushNotifications(true);

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices(context)) {
            gcm = GoogleCloudMessaging.getInstance(context);
            registrationId = preferences.getRegistrationId();

            preferences.setSenderId(senderId);
            preferences.setIcon(iconDrawable);

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
        preferences.setPushNotifications(false);
    }

    /**
     * Handles intent (GCM message) from the GcmBroadcastReceiver for all Infinario instances.
     *
     * @param context application's context
     * @param intent received intent from a broadcast receiver.
     */
    public static void handlePushNotification(Context context, Intent intent) {
        Preferences preferences = Preferences.get(context);

        if (preferences.getPushNotifications() && checkPlayServices(context)) {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            Bundle extras = intent.getExtras();
            String messageType = gcm.getMessageType(intent);
            String senderId = preferences.getSenderId();

            if (!extras.isEmpty() &&
                    senderId != null &&
                    !senderId.equals("") &&
                    GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType) &&
                    extras.getString("from").equals(senderId)) {

                Log.d(Contract.TAG, "Received data: " + intent.getExtras().toString());
                sendNotification(context, intent.getExtras(), preferences.getIcon());
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void clearStoredData() {
        preferences.clearStoredData();
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

    private void setupSession() {
        session = new Session(preferences, new SessionListener() {
            @Override
            void onSessionStart(long timestamp) {
                Log.d(Contract.TAG, "session started");

                track("session_start", Session.defaultProperties(), timestamp);
            }

            @Override
            void onSessionEnd(long timestamp, long duration) {
                Log.d(Contract.TAG, "session finished, duration = " + duration);

                track("session_end", Session.defaultProperties(duration), timestamp);
            }

            @Override
            void onSessionRestart(Map<String, String> newCustomer) {
                customer = newCustomer;
            }
        });

        session.run();
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
     * Registers the application with GCM servers asynchronously.
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    registrationId = gcm.register(preferences.getSenderId());

                    sendRegistrationIdToBackend();
                    preferences.setRegistrationId(registrationId);
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
                URL url = new URL(Preferences.get(context).getTarget() + Contract.PING_TARGET);
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
