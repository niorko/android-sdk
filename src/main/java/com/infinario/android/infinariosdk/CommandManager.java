package com.infinario.android.infinariosdk;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This file has been created by igi on 1/13/15.
 */
public class CommandManager {

    private static final String BULK_URL = "/bulk";
    private static final String TAG = "Infinario";
    private static final int MAX_RETRIES = 20;

    DbQueue queue;
    HttpHelper http;

    public CommandManager(Context context, String target) {
        queue = new DbQueue(context);
        http = new HttpHelper(target);
    }

    public boolean schedule(Command command) {
        return queue.schedule(command);
    }

    public boolean executeBatch() {
        Set<Integer> failedRequests = new HashSet<>();
        Set<Integer> successfulRequests = new HashSet<>();
        JSONArray results;
        JSONArray commands = new JSONArray();
        JSONObject payload = new JSONObject();
        List<Request> requests = queue.pop();
        HttpResponse response;
        String body;
        JSONObject data;
        Request request;
        JSONObject result;
        String status;

        if (requests.isEmpty()) {
            return false;
        }

        Log.i(TAG, "sending ids " + requests.get(0).getId() + " - " + requests.get(requests.size() - 1).getId());

        for (Request r : requests) {
            commands.put(r.getCommand());
            failedRequests.add(r.getId());
        }

        try {
            payload.put("commands", commands);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        response = http.post(BULK_URL, payload);

        try {
            body = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            body = null;
        }

        try {
            data = new JSONObject(body);
        } catch (JSONException e) {
            data = null;
        }

        if (response.getStatusLine().getStatusCode() == 200 && data != null) {
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

        Log.i(TAG, "Batch executed, " + requests.size() + " prepared, " + successfulRequests.size() + " succeeded, "
                + failedRequests.size() + " failed, rest was told to retry");

        return successfulRequests.size() > 0 || failedRequests.size() > 0;
    }

    public void flush() {
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
