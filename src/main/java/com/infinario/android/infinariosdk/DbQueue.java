package com.infinario.android.infinariosdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This file has been created by igi on 1/13/15.
 */
public class DbQueue {
    private SQLiteDatabase db;
    private String[] allColumns = { Contract.COLUMN_ID, Contract.COLUMN_COMMAND, Contract.COLUMN_RETRIES};

    public DbQueue(Context context) {
        DbHelper dbHelper = new DbHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public boolean schedule(Command command) {
        ContentValues values = new ContentValues();
        values.put(Contract.COLUMN_COMMAND, command.toString());
        return -1 < db.insert(Contract.TABLE_COMMANDS, null, values);
    }

    public List<Request> pop(Integer limit) {
        Cursor cursor = db.query(Contract.TABLE_COMMANDS, allColumns, null, null, null, null, Contract.COLUMN_ID + " ASC", limit.toString());
        List<Request> requests = new ArrayList<>();

        while (cursor.moveToNext()) {
            try {
                requests.add(new Request(cursor.getInt(0), cursor.getString(1), cursor.getInt(2)));
            } catch (JSONException e) {
                db.delete(Contract.TABLE_COMMANDS, Contract.COLUMN_ID + " = " + cursor.getInt(0), null);
            }
        }

        cursor.close();

        return requests;
    }

    public List<Request> pop() {
        return pop(Contract.DEFAULT_LIMIT);
    }

    public boolean isEmpty() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + Contract.TABLE_COMMANDS, null);
        boolean result = !(cursor.moveToFirst() && cursor.getInt(0) > 0);
        cursor.close();
        return result;
    }

    public void clear(Set<Integer> successful, Set<Integer> failed) {
        db.delete(Contract.TABLE_COMMANDS, Contract.COLUMN_ID + " IN (" + TextUtils.join(", ", successful) + ")", null);

        db.execSQL(
                "UPDATE " + Contract.TABLE_COMMANDS + " "
              + "SET " + Contract.COLUMN_RETRIES + " = " + Contract.COLUMN_RETRIES + " + 1 "
              + "WHERE " + Contract.COLUMN_ID + " IN (" + TextUtils.join(", ", failed) + ")");

        db.delete(Contract.TABLE_COMMANDS, Contract.COLUMN_RETRIES + " > " + Contract.MAX_RETRIES, null);
    }
}
