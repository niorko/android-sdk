package com.infinario.android.infinariosdk;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This file has been created by igi on 1/13/15.
 */
public class CommandManager {

    private static final int MAX_RETRIES = 20;

    DbQueue queue;
    HttpHelper http;
    Preferences preferences;
    Object lockFlush;

    public CommandManager(Context context, String target) {
        queue = new DbQueue(context);
        http = new HttpHelper(target);
        preferences = Preferences.get(context);
        lockFlush = new Object();
    }

    public boolean schedule(Command command) {
        return queue.schedule(command);
    }

    public boolean executeBatch() {
        if (!preferences.ensureCookieId()) {
            Log.d(Contract.TAG, "Failed to negotiate cookie ID");
            return false;
        }

        Set<Integer> failedRequests = new HashSet<>();
        Set<Integer> successfulRequests = new HashSet<>();
        JSONArray results;
        JSONArray commands = new JSONArray();
        JSONObject payload = new JSONObject();
        List<Request> requests = queue.pop();
        JSONObject data;
        Request request;
        JSONObject result;
        String status;

        if (requests.isEmpty()) {
            return false;
        }

        Log.i(Contract.TAG, "sending ids " + requests.get(0).getId() + " - " + requests.get(requests.size() - 1).getId());

        for (Request r : requests) {
            commands.put(setCookieId(setAge(r.getCommand())));
            failedRequests.add(r.getId());
        }

        try {
            payload.put("commands", commands);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        data = http.post(Contract.BULK_URL, payload);

        if (data != null) {
            try {
                results = data.getJSONArray("results");
            } catch (JSONException e) {
                results = null;
            }

            if (results != null) {
                for (int i = 0; i < requests.size() && i < results.length(); ++i) {
                    try {
                        request = requests.get(i);
                        result = results.getJSONObject(i);
                        status = result.getString("status").toLowerCase();

                        if (status.equals("ok")) {
                            failedRequests.remove(request.getId());
                            successfulRequests.add(request.getId());
                        } else if (status.equals("retry")) {
                            failedRequests.remove(request.getId());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        queue.clear(successfulRequests, failedRequests);

        Log.i(Contract.TAG, "Batch executed, " + requests.size() + " prepared, " + successfulRequests.size() + " succeeded, "
                + failedRequests.size() + " failed, rest was told to retry");

        return successfulRequests.size() > 0 || failedRequests.size() > 0;
    }

    public void flush() {
        synchronized (lockFlush){
            int retries = MAX_RETRIES;

            while (retries > 0) {
                if (!executeBatch()) {
                    if (queue.isEmpty()) {
                        break;
                    } else {
                        --retries;
                    }
                }
            }
        }
    }

    private JSONObject setCookieId(JSONObject command) {
        try {
            JSONObject data = command.getJSONObject("data");

            if (data.has("ids") && data.getJSONObject("ids").getString("cookie").isEmpty()) {
                data.getJSONObject("ids").put("cookie", preferences.getCampaignCookieId());
            }

            if (data.has("customer_ids") && data.getJSONObject("customer_ids").getString("cookie").isEmpty()) {
                data.getJSONObject("customer_ids").put("cookie", preferences.getCampaignCookieId());
            }
        }
        catch (JSONException ignored) {
        }

        return command;
    }

    private JSONObject setAge(JSONObject command) {
        try {
            long timestamp = command.getJSONObject("data").getLong("age");
            command.getJSONObject("data").put("age", ((new Date()).getTime() - timestamp) / 1000L);
        }
        catch (JSONException ignored) {
        }

        return command;
    }
}
