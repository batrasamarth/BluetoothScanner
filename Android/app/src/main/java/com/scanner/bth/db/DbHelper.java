package com.scanner.bth.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sb.db.Column;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shaon on 3/19/2015.
 */
public class DbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "FeedReader.db";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(new LogTable().sqlCreateTable());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(new LogTable().sqlDeleteTable());
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public List<Log> getLogs(Column sortBy, boolean asc) {
        List<Log> logList = new ArrayList<Log>();
        LogTable table = LogTable.getSingleton();
        // Select All Query
        String [] columns = table.getColumnNamesArray();

        String orderBy = sortBy == null ? null : (sortBy.getKey() + (asc == true ? " ASC" : " DESC"));
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(table.getName(), columns, null, null, null, null, orderBy);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                // Adding contact to list
                logList.add(table.deserialize(cursor));
            } while (cursor.moveToNext());
        }

        // return contact list
        db.close();
        return logList;
    }

    public Log getLog(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        LogTable table = LogTable.getSingleton();
        Cursor cursor = db.query(table.getName(), table.getColumnNamesArray(), LogTable._ID.getKey() + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        } else {
            throw new RuntimeException("could not get the requested log: " + id);
        }

        Log log = LogTable.getSingleton().deserialize(cursor);
        db.close();
        return log;
    }

    public long createLog(String owner) {
        long now = System.currentTimeMillis();
        Log log = new Log(0, now, now, owner, 0L, false);

        SQLiteDatabase db = this.getWritableDatabase();
        LogTable table = new LogTable();
        long rowId = db.insert(table.getName(), null, table.serialize(log));
        db.close();
        return rowId;
    }

    public List<Location> getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        LocationTable table = LocationTable.getSingleton();

        // Select All Query
        String [] columns = table.getColumnNamesArray();

        Cursor cursor = db.query(table.getName(), columns, null, null, null, null, null);
        List<Location> results = new ArrayList<Location>();

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                // Adding contact to list
                results.add(table.deserialize(cursor));
            } while (cursor.moveToNext());
        }
        db.close();
        return results;
    }

}
