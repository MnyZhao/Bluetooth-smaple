package com.mny.bluetooth_smaple.classic_bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mny.bluetooth_smaple.R;
import com.mny.bluetooth_smaple.ble.BleScanActivity;
import com.mny.bluetooth_smaple.bluetooth_manager.BluetoothBasisManager;
import com.mny.bluetooth_smaple.bluetooth_manager.BluetoothConnectManager;
import com.mny.bluetooth_smaple.bluetooth_manager.Constants;

public class MainActivity extends AppCompatActivity {
    private ListView lvView;
    private EditText editText;
    private Button button;
    private TextView tvStates;
    public static final String TAG = "MainActivity";
    /* 请求码*/
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    BluetoothConnectManager blcManager;
    BluetoothBasisManager blThManager;
    private String mConnectedDeviceName = null;
    ArrayAdapter<String> mConversationArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blcManager = new BluetoothConnectManager(mHandler);
        blThManager = new BluetoothBasisManager();

        initView();
    }

    private void initView() {
        lvView = findViewById(R.id.lv_msg);
        editText = findViewById(R.id.input);
        button = findViewById(R.id.send);
        tvStates = findViewById(R.id.tv_status);
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        lvView.setAdapter(mConversationArrayAdapter);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check that we're actually connected before trying anything
                if (blcManager.getState() != BluetoothConnectManager.STATE_CONNECTED) {
                    Toast.makeText(MainActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editText.getText().toString().trim().length() > 0) {
                    blcManager.write(editText.getText().toString().getBytes());
                    editText.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "发送内容不能为空", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (blThManager.standBuyBluetooth()) {//判断是否支持
            if (!blThManager.isUseBluetooth()) {//判断是否启用
                Toast.makeText(this, "蓝牙未启动先启动蓝牙", Toast.LENGTH_SHORT).show();
                blThManager.startBluetooth(this);//启动蓝牙
            } else {
                if (blcManager != null) {
                    // Only if the state is STATE_NONE, do we know that we haven't started already
                    if (blcManager.getState() == BluetoothConnectManager.STATE_NONE) {
                        // Start the Bluetooth chat services
                        blcManager.start();
                    }
                }
                Toast.makeText(this, "蓝牙已经启动", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CONNECT_DEVICE_SECURE) {
            if (resultCode == Activity.RESULT_OK) {
                String deviceAdress = data.getStringExtra(ScanActivity.EXTRA_DEVICE_ADDRESS);
                Log.e(TAG, "onActivityResult: 启动连接");
                blcManager.connect(blThManager.getBluetoothDevice(deviceAdress));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.skip, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_skip:
                Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE_SECURE);
                Toast.makeText(MainActivity.this, getResources().getString(R.string.search), Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_gole:
                Intent intent1 = new Intent(MainActivity.this, BleScanActivity.class);
                startActivity(intent1);
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blcManager != null) {
            blcManager.stop();
        }
    }

    private void setStatus(String msg) {
        tvStates.setText(msg);
    }

    private void setStatus(int msg) {
        tvStates.setText(msg);
    }

    Handler mHandler = new Handler() {
        @SuppressLint("StringFormatInvalid")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothConnectManager.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothConnectManager.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothConnectManager.STATE_LISTEN:
                        case BluetoothConnectManager.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // 读取信息
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
