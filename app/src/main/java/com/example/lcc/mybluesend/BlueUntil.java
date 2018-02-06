package com.example.lcc.mybluesend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by lcc on 2018/2/6.
 */

public class BlueUntil {
    private Context mContext;
    private BluetoothAdapter mBlue;
    // 选中发送数据的蓝牙设备
    private BluetoothDevice selectDevice;
    // 获取到选中设备的客户端串口
    private BluetoothSocket clientSocket;
    // 获取到向设备写的输出流
    private OutputStream os;
    // UUID，蓝牙建立链接需要的
    private final UUID MY_UUID = UUID.fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");
    // 为其链接创建一个名称
    private final String NAME = "Bluetooth_Socket";
    // 创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // 吐司显示传输过来的消息
            myToast((String) msg.obj);
        }
    };

    public BlueUntil(Context mContext) {
        this.mContext = mContext;
        mBlue = BluetoothAdapter.getDefaultAdapter();
    }

    //判断是否连接上了蓝牙
    public boolean isConnected() {
        if (mBlue.isEnabled()) {
            return true;
        }
        return false;
    }

    //打开蓝牙
    public void openBlue() {
        if (mBlue != null) {
            mBlue.enable();
        } else {
            myToast("您手机没有蓝牙功能");
        }
    }

    //关闭蓝牙
    public void closeBlue() {
        mBlue.disable();
    }

    //已经配对的设备数据
    public List<String> getdevices() {
        List<String> bluetoothDevices = new ArrayList<>();
        Set<BluetoothDevice> devices = mBlue.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : devices) {
                // 保存到arrayList集合中
                bluetoothDevices.add(bluetoothDevice.getName() + ":"
                        + bluetoothDevice.getAddress() + "\n");
            }
        }
        return bluetoothDevices;
    }

    //搜索周边的蓝牙设备
    public void searchBlue() {
        if (mBlue.isDiscovering()) {
            mBlue.cancelDiscovery();
        }
        mBlue.startDiscovery();
    }

    //回收
    public void destoryBlue() {
        mBlue = null;
    }

    public void creatClient(String address) {
        // 判断当前是否还是正在搜索周边设备，如果是则暂停搜索
        if (mBlue.isDiscovering()) {
            mBlue.cancelDiscovery();
        }
        // 如果选择设备为空则代表还没有选择设备
        if (selectDevice == null) {
            //通过地址获取到该设备
            selectDevice = mBlue.getRemoteDevice(address);
            //未实现 不弹框配对
            try {
                setPin(selectDevice.getClass(), selectDevice,"0"); // 手机和蓝牙采集器配对
                createBond(selectDevice.getClass(), selectDevice);
                cancelPairingUserInput(selectDevice.getClass(), selectDevice);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        // 这里需要try catch一下，以防异常抛出
        try {
            // 判断客户端接口是否为空
            if (clientSocket == null) {
                // 获取到客户端接口
                clientSocket = selectDevice
                        .createRfcommSocketToServiceRecord(MY_UUID);
                // 向服务端发送连接
                clientSocket.connect();
                // 获取到输出流，向外写数据
                os = clientSocket.getOutputStream();

            }
            // 判断是否拿到输出流
            if (os != null) {
                // 需要发送的信息
                String text = "成功发送信息";
                // 以utf-8的格式发送出去
                os.write(text.getBytes("UTF-8"));
            }
            // 吐司一下，告诉用户发送成功
            myToast("发送信息成功，请查收");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // 如果发生异常则告诉用户发送失败
            myToast("发送信息失败");
        }

    }

    public AcceptThread getAcceptThread() {
        return new AcceptThread();
    }

    // 服务端接收信息线程
    public class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;// 服务端接口
        private BluetoothSocket socket;// 获取到客户端的接口
        private InputStream is;// 获取到输入流
        private OutputStream os;// 获取到输出流

        public AcceptThread() {
            try {
                // 通过UUID监听请求，然后获取到对应的服务端接口
                serverSocket = mBlue
                        .listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        public void run() {
            try {
                // 接收其客户端的接口
                socket = serverSocket.accept();
                // 获取到输入流
                is = socket.getInputStream();

                // 无线循环来接收数据
                while (true) {
                    // 创建一个128字节的缓冲
                    byte[] buffer = new byte[128];
                    // 每次读取128字节，并保存其读取的角标
                    int count = is.read(buffer);
                    // 创建Message类，向handler发送数据
                    Message msg = new Message();
                    // 发送一个String的数据，让他向上转型为obj类型
                    msg.obj = new String(buffer, 0, count, "utf-8");
                    // 发送数据
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }


    //toast提示
    public void myToast(String mes) {
        Toast toast = Toast.makeText(mContext, mes, Toast.LENGTH_SHORT);
        toast.show();
    }

    static public boolean createBond(Class btClass, BluetoothDevice btDevice)
            throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    public boolean setPin(Class btClass, BluetoothDevice btDevice,
                          String str) throws Exception {
        try {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin",
                    new Class[]
                            {byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice,
                    new Object[]
                            {str.getBytes()});
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;

    }

    // 取消用户输入
    public boolean cancelPairingUserInput(Class btClass,
                                          BluetoothDevice device)

            throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        // cancelBondProcess()
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    // 取消配对
    static public boolean cancelBondProcess(Class btClass,
                                            BluetoothDevice device)

            throws Exception {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }
}
