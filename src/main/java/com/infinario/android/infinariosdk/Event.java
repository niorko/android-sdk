package com.infinario.android.infinariosdk;

import java.util.HashMap;
import java.util.Map;

/**
 * This file has been created by igi on 1/13/15.
 */
public class Event extends Command {

    protected Map<String, String> customerIds;
    protected String companyId;
    protected Map<String, Object> properties = null;
    protected String type;


    public Event(Map<String, String> customerIds, String companyId, String type, Map<String, Object> properties, Long timestamp) {
        super(Contract.EVENT_ENDPOINT, timestamp);

        this.customerIds = customerIds;
        this.companyId = companyId;
        this.properties = properties;
        this.type = type;
    }

    @Override
    protected Map<String, Object> getData() {
        Map<String, Object> data = new HashMap<>();

        data.put("customer_ids", customerIds);
        data.put("company_id", companyId);
        data.put("type", type);
        data.put("age", getAge());

        if (null != properties) {
            data.put("properties", properties);
        }

        return data;
    }
}
