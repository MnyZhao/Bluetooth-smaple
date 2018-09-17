package com.mny.bluetooth_smaple.bluetooth_manager;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;


import java.util.Set;
import java.util.logging.Handler;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Crate by E470PD on 2018/9/3
 * 基础类
 * 判断是否支持蓝牙 {@link #standBuyBluetooth()}
 * 判断蓝牙是否可用{@link #isUseBluetooth()}
 * 开启蓝牙 {@link #startBluetooth(Activity)}
 * 开启蓝牙 判断方法内置{@link #startBluetooths(Activity)}
 * 获取蓝牙适配器{@link #getBluetoothAdapter()}
 * 获取已经配对过的设备{@link #getPairedDevice()}
 * 扫描蓝牙设备{@link #startScanBlueDevice(Context, BluetoothCallBackDeviceInfo, Boolean)}
 * 根据地址获取BlueToothDevice {@link #getBluetoothDevice(String)}
 * 停止扫描{@link #endScan()}
 * 非正常终止扫描比如activity Destory {@link #endScanInDestory(Context)}
 */
public class BluetoothBasisManager {
    private String TAG = "BluetoothBasisManager";
    private boolean isRegister = false;//判断是否注册 false 表示未注册 true表示已经注册
    /**
     * 表示本地蓝牙适配器（蓝牙无线装置）。 BluetoothAdapter 是所有蓝牙交互的入口点。
     * 利用它可以发现其他蓝牙设备，查询绑定（配对）设备的列表，使用已知的 MAC 地址实例化 BluetoothDevice，
     * 以及创建 BluetoothServerSocket 以侦听来自其他设备的通信。
     */
    private BluetoothAdapter bluetoothAdapter;
    /*是否返回已经连接过的蓝牙信息 默认不返回 true 便是返回 fasle 表不返回*/
    private Boolean isReturnBond = false;

    public BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }

    /**
     * 开始扫描蓝牙设备
     * 开始扫描之前要先注册查找以及查找结束的Reciver {@link #registerFoundReciver}并调用BluetoothAdapter.startDiscovery方法->
     * 开始查找
     *
     * @param context      关联上下文
     * @param bcdi         回调接口用于返回devcie 和是否扫描结束
     * @param isReturnBond 是否返回已经匹配的蓝牙device true 便是返回 fasle 表不返回
     */
    public void startScanBlueDevice(Context context, BluetoothCallBackDeviceInfo bcdi, Boolean isReturnBond) {
        /*注册开始查找以及查找结束事件*/
        registerFoundReciver(context, bcdi, isReturnBond);
        /*开始查找 如果已经开始要先停止 在开始查找避免冲突*/
        if (getBluetoothAdapter().isDiscovering()) {
            getBluetoothAdapter().cancelDiscovery();
        }
        getBluetoothAdapter().startDiscovery();
    }

    /**
     * 若是停止查找则需要调用此方法
     */
    public void endScan() {
        // 如果已经开始查找就停止
        if (getBluetoothAdapter().isDiscovering()) {
            getBluetoothAdapter().cancelDiscovery();
        }
    }

    /**
     * 如果在扫描未结束时要退出当前activity 要关闭扫描并解除注册
     */
    public void endScanInDestory(Context context) {
        endScan();
        Log.e(TAG, "unregisterReceiver: " + isRegister);
        if (isRegister) {
            isRegister = false;
            Log.e(TAG, "endScanInDestory: "+isRegister);
            context.unregisterReceiver(mReceiver);
        }

    }

    /**
     * 注册广播接收器
     *
     * @param context
     * @param bcdi         回调接口用于返回devcie  and isover
     * @param isReturnBond 是否返回已经匹配的蓝牙device true 便是返回 fasle 表不返回
     */
    private void registerFoundReciver(Context context, BluetoothCallBackDeviceInfo bcdi, Boolean isReturnBond) {
        this.bluetoothCallBackDeviceInfo = bcdi;
        this.isReturnBond = isReturnBond;
        // Register for broadcasts when a device is discovered
        //查找注册
        /*IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(mReceiver, filter);*/
        // Register for broadcasts when discovery has finished
        // 结束注册
        /*filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mReceiver, filter);*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mReceiver, filter);
        isRegister = true;
        Log.e(TAG, "registerFoundReciver: " + isRegister);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //当发现找到设备时
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                // 从Intent中获取bluetoothdevice
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                // 如果要求返回所有则不管原来匹配是否成功 否则过滤已经匹配过的
                if (isReturnBond) {
                    bluetoothCallBackDeviceInfo.setDevice(false, device);
                } else {
                    // 如果已经配对请跳过
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        bluetoothCallBackDeviceInfo.setDevice(false, device);
                    }
                }

                // 完成后
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothCallBackDeviceInfo.setDevice(true, null);
            }
        }
    };

    /**
     * 设置回调事件
     *
     * @param bluetoothCallBackDeviceInfo
     */
    public void setBluetoothCallBackDeviceInfo(BluetoothCallBackDeviceInfo bluetoothCallBackDeviceInfo) {
        this.bluetoothCallBackDeviceInfo = bluetoothCallBackDeviceInfo;
    }

    private BluetoothCallBackDeviceInfo bluetoothCallBackDeviceInfo;

    public interface BluetoothCallBackDeviceInfo {
        /*返回是否扫描完成(false表示扫描未完成 true表示扫描完成) 和 device*/
        void setDevice(boolean isOver, BluetoothDevice device);
    }

    /**
     * 判断 蓝牙是否可以使用
     * true 表示可用 false 标识不可用
     *
     * @return
     */
    public boolean isUseBluetooth() {
        return getBluetoothAdapter().isEnabled();
    }

    public static final int REQUEST_ENABLE_BT = 3;

    /**
     * 在onActivityResult中接收返回值参考如下方法startBluetoothIsOk
     * {@link #startBluetoothIsOk}
     *
     * @param activity
     */
    public void startBluetooth(Activity activity) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    /**
     * 判断是否开启 未开启则直接请求开启
     * 在onActivityResult中接收返回值参考如下方法startBluetoothIsOk
     * {@link #startBluetoothIsOk}
     * 判断是否支持蓝牙 支持判断是否启用 未启用则启用
     *
     * @param activity
     */
    public void startBluetooths(Activity activity) {
        if (standBuyBluetooth()) {
            if (isUseBluetooth()) {
                startBluetooth(activity);
            } else {
                Log.e(TAG, "startBluetooths: 蓝牙已经启动");
            }
        } else {
            Log.e(TAG, "startBluetooths: 蓝牙已经启动");
        }
    }

    /**
     * 在onActivityResult中实现
     *
     * @param activity
     * @param requestCode 请求码
     * @param resultCode  返回码
     * @return true 表示开启  false 表示未开启
     */
    public boolean startBluetoothIsOk(Activity activity, int requestCode, int resultCode) {
        boolean isok = false;
        if (requestCode == BluetoothBasisManager.REQUEST_ENABLE_BT) {
            switch (resultCode) {
                case RESULT_CANCELED:
                    Log.e(TAG, "startBluetoothIsOk: 用户拒绝");
                    isok = false;
                    break;
                case RESULT_OK:
                    Log.e(TAG, "startBluetoothIsOk: 开启成功");
                    isok = true;
                    break;
            }
        }
        return isok;
    }

    /**
     * 判断设备是否支持蓝牙
     * true 支持  false 不支持
     *
     * @return
     */
    public boolean standBuyBluetooth() {
        if (null == getBluetoothAdapter()) {
            return false;
        }
        return true;
    }
    /**
     * 获取配对的设备
     *
     * @return
     */
    public Set<BluetoothDevice> getPairedDevice() {
        return getBluetoothAdapter().getBondedDevices();
    }

    /**
     * 根据地址获取device
     * @param adress
     * @return
     */
    public BluetoothDevice getBluetoothDevice(String adress){
        return getBluetoothAdapter().getRemoteDevice(adress);
    }
}
