package com.scanner.bth.update;

import android.bluetooth.BluetoothClass;
import android.content.Context;

import com.scanner.bth.db.DbHelper;
import com.scanner.bth.db.Location;
import com.scanner.bth.db.LocationDevice;
import com.scanner.bth.http.Api;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shaon on 4/3/2015.
 */
public class DbUpdater {
    public static void updateDatabaseWithLocations(Context context) {
        ArrayList<Location> locations = Api.getAllLocations();
        DbHelper dbHelper = DbHelper.getInstance();
        dbHelper.deleteAllLocations();
        dbHelper.addLocations(locations);
    }

    public static void updateDatabaseWithDevices(Context context, Location location) {
        ArrayList<LocationDevice> devices = Api.getDevicesForLocation(location);
        DbHelper.getInstance().deleteDevicesForLocation(location);

        if(DbHelper.getInstance().getLocalLocationDevices(location).size() != 0) {
            throw new RuntimeException("delete failed");
        }

        DbHelper.getInstance().addDevicesToLocation(devices);
        List<LocationDevice> dbDevices = DbHelper.getInstance().getLocalLocationDevices(location);
        int size = dbDevices.size();
        if (size != devices.size()) {
            throw new RuntimeException("we're adding way more devices than we expected");
        }
    }
}
