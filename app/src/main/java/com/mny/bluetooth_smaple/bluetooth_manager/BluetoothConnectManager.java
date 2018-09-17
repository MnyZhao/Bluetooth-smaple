package com.mny.bluetooth_smaple.bluetooth_manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Crate by E470PD on 2018/9/5
 * 连接管理类
 * 创建连接  创建管理连接
 * 逻辑-> 双方设备设备A 设备B
 * A，B 都要启动一个服务线程 AcceptThread 用来检测接收连接请求并处理成功失败操作
 * 若A作为客户端启动连接线程ConnectThread 去连接B 则B作为服务端 当接收到A的请求 并接受A的匹配
 * 匹配成功后 B A 各自启动自己的管理连接线程 ConnectedThread 并在其中获取输入输出流用做数据传递
 * 方法介绍
 * 整体通过handler 发送消息并更新ui
 * 启动服务端{@link #start()}  服务端代码参考{@link AcceptThread}
 * 终止连接的方法在onDestory中执行{@link #stop()}
 * 启动客户端连接{@link #connect(BluetoothDevice)}  客户端代码参考{@link ConnectThread}
 * 启动连接管理用来通讯{@link #connected(BluetoothSocket, BluetoothDevice)} 管理线程代码参考{@link ConnectedThread}
 * 写入数据的方法{@link #write(byte[])}
 */
public class BluetoothConnectManager {
    private static final String TAG = "BluetoothConnectManager";
    // 创建连接时记录的名称
    private static final String NAME_SECURE = "BluetoothChatSecure";
    //应用程序唯一UUID 用于获取连接socket
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // 连接状态
    private int mState;
    private int mNewState;
    //指示当前连接状态的常量
    public static final int STATE_NONE = 0;       // 默认没连接
    public static final int STATE_LISTEN = 1;     // 正在监听传入连接
    public static final int STATE_CONNECTING = 2; // 正在启动传出连接
    public static final int STATE_CONNECTED = 3;  // 现在连接到远程设备
    private BluetoothAdapter bluetoothAdapter;

    private AcceptThread mSecureAcceptThread;//服务线程
    private ConnectThread mConnectThread;//连接线程
    private ConnectedThread mConnectedThread;//管理线程
    private Handler mHandler;

    public BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }

    public BluetoothConnectManager() {
    }

    public BluetoothConnectManager(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * 该线程在监听传入连接时运行。它的行为类似于服务器端客户端。它一直运行，直到接受连接（或直到取消）。
     */
    private class AcceptThread extends Thread {
        // 本地服务连接
        private final BluetoothServerSocket mmServerSocket;

        // 默认secure 全部为true
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // 创建监听的socket 根据UUID
            try {
                tmp = getBluetoothAdapter().listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket :  监听失败", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.i(TAG, "Socket : 开始接收线程" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            //如果我们没有连接，请接收服务器socket
            while (mState != STATE_CONNECTED) {
                try {
                    //这是一个阻塞调用，只会在成功连接或异常时返回
                    //当 accept() 返回 BluetoothSocket 时，表示已连接好 客户端要调用connect()才能连接成功
                    socket = mmServerSocket.accept();
                    Log.i(TAG, "run: 接收成功");
                } catch (IOException e) {
                    Log.e(TAG, "Socket : 接收连接失败", e);
                    break;
                }

                //如果连接被接受 socket 不为空则表示接收成功 要启动管理连接线程
                if (socket != null) {
                    synchronized (BluetoothConnectManager.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //情况正常。启动连接的管理线程
                                Log.i(TAG, "run: 启动管理线程");
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                //未准备好或已连接。终止新socket连接。
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "无法关闭不需要的连接", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "socket : 结束接收");
        }

        public void cancel() {
            Log.d(TAG, "Socket : cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket : 关闭服务器失败", e);
            }
        }
    }

    /**
     * 尝试建立连接
     * 尝试与设备建立传出连接时，此线程会运行。它直接通过连接成功或失败
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            //获取BluetoothSocket以获得与给定BluetoothDevice的连接
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: 创建连接失败", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "创建连接线程来连接服务端 ");
            setName("ConnectThread");
            //始终取消扫描，因为它会降低连接速度
            getBluetoothAdapter().cancelDiscovery();
            //连接到BluetoothSocket
            try {
                //这是一个阻塞调用，只会在成功连接或异常时返回 mmSocket调用connect() 连接
                mmSocket.connect();
                Log.i(TAG, "run: 连接成功 后要启动管理线程");
            } catch (IOException e) {
                // 关闭socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "无法关闭socket " +
                            " socket 连接失败", e2);
                }
                //连接失败后需操作
                connectionFailed();
                return;
            }
            //重置ConnectThread因为我们已经完成了连接
            synchronized (BluetoothConnectManager.this) {
                mConnectThread = null;
            }
            //启动管理连接线程
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭socket连接失败", e);
            }
        }
    }

    /**
     * 管理连接
     * 此线程在与远程设备连接期间运行。它处理所有传入和传出传输。
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.i(TAG, "create ConnectedThread: 创建管理线程");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            //获取BluetoothSocket输入和输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            //连接时继续监听InputStream 当有一方断开或者出现异常进入Error
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    //从InputStream中读取数据
                    bytes = mmInStream.read(buffer);
                    //发送消息更新UI
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "断开", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * 写入已连接的OutStream。
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                //发送数据
                mmOutStream.write(buffer);
                // 发送数据更新UI
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭socket失败", e);
            }
        }
    }

    /**
     * 创建连接 客户端
     * 启动ConnectThread以启动与远程设备的连接。
     *
     * @param device The BluetoothDevice to connect 设备信息
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.i(TAG, "connect to: " + device);
        // 取消尝试建立连接的任何线程
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // 取消当前正在运行连接的任何管理线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //启动线程以连接给定设备
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        Log.i(TAG, "connecting...");
        //  更新ui  此时 是连接中 connecting...
        updateUserInterfaceTitle();
    }

    /**
     * 管理连接
     * 启动ConnectedThread以开始管理蓝牙连接
     *
     * @param socket The BluetoothSocket on which the connection was made 连接的socket
     * @param device The BluetoothDevice that has been connected 设备信息
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.i(TAG, "connected, Socket 连接成功 要启动管理线程");
        //取消已经完成连接的线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //取消当前正在运行连接的任何管理线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //取消接受线程，因为我们只想连接到一个设备
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        //启动线程管理连接,并执行传输
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        //连接成功了 更改设备名称
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title 此时应该是connected连接成功
        updateUserInterfaceTitle();
    }

    /**
     * 启动监听模式以便接收连接请求
     * 。具体来说，启动AcceptThread开始
     * 侦听（服务器）模式下的会话。由Activity onResume（）调用
     */
    public synchronized void start() {
        Log.i(TAG, "start 开启监听模式");
        //取消尝试建立连接的任何线程 为最新的做准备
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 取消当前正在运行连接的任何管理线程 为最新的做准备
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //启动线程以侦听BluetoothServerSocket 为最新的做准备
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
        }
        // Update UI title
        //更新标题  此时应该无连接 not connected
        updateUserInterfaceTitle();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     * 连接失败尝试重新启动监听模式 并更新UI
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        //连接失败将状态变为默认
        mState = STATE_NONE;
        // Update UI title 此时更新UI
        updateUserInterfaceTitle();
        //重新启动监听模式
        BluetoothConnectManager.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     * 指示连接已丢失并通知UI活动。
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // 状态改为默认
        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
        // 重新启动监听模式
        BluetoothConnectManager.this.start();
    }

    /**
     * Update UI title according to the current state of the chat connection
     * 更新连接状态
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "更新状态() " + mNewState + " -> " + mState);
        mNewState = mState;

        // 读取handler状态以便更新
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     * 返回连接状态
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 以不同步的方式写入ConnectedThread
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        //写入到输出流
        r.write(out);
    }
}
