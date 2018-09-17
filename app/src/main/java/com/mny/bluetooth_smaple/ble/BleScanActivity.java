package com.mny.bluetooth_smaple.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.mny.bluetooth_smaple.R;
import com.mny.bluetooth_smaple.bluetooth_manager.BleManager;

public class BleScanActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * StartScan
     */
    private Button mBtnStart;
    /**
     * StopScan
     */
    private Button mBtnStop;

    BleManager mBleManager;
    private ListView mLvBle;
    LeDeviceListAdapter mLeDeviceListAdapter;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static final int REQUEST_ENABLE_BT = 1;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);
        mBleManager = new BleManager( 10000);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        initView();
    }

    private void initView() {
        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnStart.setOnClickListener(this);
        mBtnStop = (Button) findViewById(R.id.btn_stop);
        mBtnStop.setOnClickListener(this);
        mLvBle = (ListView) findViewById(R.id.lv_ble);
        mLvBle.setAdapter(mLeDeviceListAdapter);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_start:
                mLeDeviceListAdapter.clear();
                mBleManager.startLeDevice(leScanCallback);
                break;
            case R.id.btn_stop:
                mBleManager.stopLeDevice(leScanCallback);
                break;
        }
    }

    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    Log.e("BleScanActivity", "run: scanRecord.toString()");
                    System.out.println("BleScanActivity.onLeScan" + new String(scanRecord));
                }
            });

        }
    };
}
