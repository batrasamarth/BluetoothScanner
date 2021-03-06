package com.scanner.bth.bluetoothscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.scanner.bth.db.DbHelper;
import com.scanner.bth.db.Location;
import com.scanner.bth.db.LocationDevice;
import com.scanner.bth.db.LogEntry;

import java.util.List;
import java.util.UUID;


public class ScannerActivity extends ActionBarActivity implements
        ItemFragment.OnFragmentInteractionListener, DetailFragment.OnFragmentInteractionListener,
        ReportFragment.OnFragmentInteractionListener {

    // Used to update information about said device.
    public static final String LOG_ID_EXTRA = "com.scanner.bth.bluetoothscanner.MainActivity.LOG_ID_EXTRA";

    // Used to grab devices to pre-populate the list
    public static final String INTENT_EXTRA_LOCATION_ID = "com.scanner.bth.bluetoothscanner.MainActivity.location_id";

    private BluetoothAdapter mBluetoothAdapter;
    private BthScanModel mScanModel;
    private ItemFragment mScanResultFragment;

    public static final int REQUEST_ENABLE_BT = 1;

    private static final String TAG_LIST_FRAGMENT = "bth_list_fragment";
    private static final String TAG_DETAIL_FRAGMENT = "bth_detail_framgment";
    private static final String TAG_REPORT_FRAGMENT = "bth_report_fragment";

    private MenuItem mStartScanButton;
    private MenuItem mStopScanButton;
    private long mLocationId;
    private String mLogId;
    com.scanner.bth.db.Log mLog;
    private BthScanResultsModel mScanResultModel;
    private Location mLocation;

    @Override
    public BthScanResultsModel getBthScanResultsModel() {
        return mScanResultModel;
    }

    @Override
    public void generateReport() {
        ReportFragment reportFragment = ReportFragment.newInstance(UUID.fromString(mLogId));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, reportFragment)
                        // Add this transaction to the back stack
                .addToBackStack(TAG_REPORT_FRAGMENT)
                .commit();
    }


    public BthScanModel.BthScanView mScanListener = new BthScanModel.BthScanView() {

        @Override
        public void onLeScan(android.bluetooth.le.ScanResult result) {
            Log.d("SCAN", "Found an item");
            BthScanResultsModel.ScanResult scanResult = new BthScanResultsModel.ScanResult(result.getDevice(), result.getScanRecord());
            mScanResultModel.addDevice(scanResult);
        }

        @Override
        public void onScanStart() {
            Log.d("SCAN", "Starting to scan");
            // update the list everytime we scan.
        }

        @Override
        public void onScanFinish() {
            Log.d("SCAN", "Finished a scan");
            // if we have any animations, we should stop them.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        mScanResultFragment = (ItemFragment) fm.findFragmentByTag(TAG_LIST_FRAGMENT);
        mLogId = getIntent().getExtras().getString(ScannerActivity.LOG_ID_EXTRA);
        mLog = DbHelper.getInstance().getLog(UUID.fromString(mLogId));
        mLocationId = mLog.getLocationId();


        mLocation = DbHelper.getInstance().getLocation(mLog.getLocationId());
        if (mScanResultFragment == null) {

            mScanResultFragment = ItemFragment.newInstance(mLocation.getAddress());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mScanResultFragment, TAG_LIST_FRAGMENT)
                    .commit();
        }



        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mScanModel = new BthScanModel(mBluetoothAdapter);
        mScanModel.attachView(mScanListener);
        mScanResultModel = new BthScanResultsModel();



        Log.d(ScannerActivity.class.getSimpleName(), "location id: " + mLocationId);

        if (savedInstanceState == null) {

            final List<LocationDevice> devices = DbHelper.getInstance().getLocalLocationDevices(new Location(mLocationId, null, null));
            final List<LogEntry> logEntries = DbHelper.getInstance().getLogEntries(mLog, null, false);
            Log.d(ScannerActivity.class.getSimpleName(), "device count: " + devices.size());
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    for (LogEntry entry : logEntries) {
                        BeaconParser.BeaconData data = BeaconParser.read(entry.getByteRecord());
                        LocationDevice device = DbHelper.getInstance().getLocationDevice(mLocationId, data.getProximity_uuid());
                        mScanResultModel.prePopulate(entry, device);
                    }
                }
            });

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanModel.deattachView(mScanListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mStartScanButton = menu.findItem(R.id.action_search);
        mStopScanButton = menu.findItem(R.id.action_stop_search);
        mStopScanButton.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_settings: {
                return true;
            }

            case R.id.action_search: {
                Log.d("ACTION SCAN", "scan clicked in action bar");
                mScanModel.scanLeDevice(true);
                mStartScanButton.setVisible(false);
                mStopScanButton.setVisible(true);
                setSupportProgressBarIndeterminateVisibility(true);
                return true;
            }

            case R.id.action_stop_search: {
                Log.d("ACTION SCAN", "stop clicked in action bar");
                mScanModel.scanLeDevice(false);
                mStartScanButton.setVisible(true);
                mStopScanButton.setVisible(false);
                setSupportProgressBarIndeterminateVisibility(false);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClickDevice(BthScanResultsModel.ScanResult result) {
        DetailFragment detailFragment = DetailFragment.newInstance(result.getlogEntry().getId(), "test", result.getLocationDevice().getName(), 0);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, detailFragment)
                        // Add this transaction to the back stack
                .addToBackStack(TAG_DETAIL_FRAGMENT)
                .commit();
    }

    @Override
    public void reportFinished() {
        mScanResultModel.completeLog();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {



        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.bth_devices_fragment, container, false);

            return rootView;

        }
    }
}
