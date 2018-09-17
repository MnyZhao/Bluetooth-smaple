package com.mny.bluetooth_smaple.classic_bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.mny.bluetooth_smaple.R;
import com.mny.bluetooth_smaple.animation_manager.Anim_manager;
import com.mny.bluetooth_smaple.bluetooth_manager.BluetoothBasisManager;

import java.util.Set;

public class ScanActivity extends AppCompatActivity {
    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final String TAG = "SCAN";
    private ListView lvPaired, lvUnPaired;
    ArrayAdapter<String> pairedAdapter;
    ArrayAdapter<String> unPairedAdapter;
    Anim_manager anim_manager;
    BluetoothBasisManager blThManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        blThManager = new BluetoothBasisManager();
        initView();
        if (blThManager.standBuyBluetooth()) {//判断是否支持
            if (!blThManager.isUseBluetooth()) {//判断是否启用
                blThManager.startBluetooth(this);//启动蓝牙
            } else {
                Toast.makeText(this, "蓝牙已经启动", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        lvPaired = findViewById(R.id.lv_paired_devices);
        lvUnPaired = findViewById(R.id.lv_unpaired_devices);
        pairedAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        unPairedAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        lvPaired.setAdapter(pairedAdapter);
        lvUnPaired.setAdapter(unPairedAdapter);
        lvPaired.setOnItemClickListener(onItemClickListener);
        lvUnPaired.setOnItemClickListener(onItemClickListener);
        Set<BluetoothDevice> deviceSet = blThManager.getPairedDevice();
        for (BluetoothDevice device : deviceSet) {
            pairedAdapter.add(device.getName() + "\n" + device.getAddress());
        }
    }

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                if (unPairedAdapter != null) {
                    unPairedAdapter.clear();
                }

                anim_manager = new Anim_manager(item);
                anim_manager.showAnimate(item, ScanActivity.this);
                blThManager.startScanBlueDevice(ScanActivity.this, backDeviceInfo, false);
                Toast.makeText(ScanActivity.this, getResources().getString(R.string.search), Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }
    BluetoothBasisManager.BluetoothCallBackDeviceInfo backDeviceInfo = new BluetoothBasisManager.BluetoothCallBackDeviceInfo() {
        @Override
        public void setDevice(boolean isOver, BluetoothDevice device) {
            if (!isOver) {
                unPairedAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.e(TAG, "name: " + device.getName() + "adress :" + device.getAddress());
            } else {
                if (unPairedAdapter.getCount() == 0) {
                    unPairedAdapter.add("No Device");
                }
                Log.e(TAG, "setDevice: FINISHED");
                anim_manager.hideAnimate();
            }
        }
    };

    @Override
    protected void onDestroy() {
        blThManager.endScanInDestory(ScanActivity.this);
        super.onDestroy();
    }
}
