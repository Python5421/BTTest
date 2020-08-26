package com.lanyue.bttest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

//import android.bluetooth.BluetoothServerSocket;
//import com.example.book11_6_1.message.AppToBSMessage;
//import com.example.book11_6_1.util.BytesToStrings;
//import java.nio.ByteBuffer;
//import java.util.LinkedList;
//import java.util.Queue;

/**
 * 蓝牙服务
 * 方法：
 * 1)构造函数 BTService(Context context, Handler myHandler)
 * 2)多设备连接 synchronized void connect(BluetoothDevice device, int index) 目前只启动一个 index=1
 * 3)连接线程类（内部类）     ConnectThread     private class ConnectThread extends Thread
 * 4)连接后线程类（内部类）  ConnectedThread
 * 5)连接失败
 * 6)连接丢失
 * 7)启动
 * 8)连接 synchronized void connected(BluetoothSocket socket, BluetoothDevice device, int index)
 * 9)设置状态
 * 10)接受线程
 * 11)获取当前状态
 * 12)停止
 */
public class BTService {
    private static final String TAG = "BTService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//目前使用串口UUID
    boolean IsConnect = false;//是否连接
    //    private Context context;
    private BluetoothAdapter myAdapter;//本地蓝牙适配器
    private Handler myHandler;
    private ConnectThread myConnectThread;// 连接一个设备的进程

    public BSConnected_Receive_Thread bSConnected_Receive_Thread;// 已经连接之后的管理进程


    /**
     * 构造函数
     *
     * @param inputHandler
     */
    public BTService(Handler inputHandler) {
        this.myHandler = inputHandler;
        myAdapter = BluetoothAdapter.getDefaultAdapter();//获取蓝牙适配器
        //mState = STATE_NONE; //当前连接状态：未连接
    }

    /**
     * 连接
     * 参数 index 是 硬件设备的id ，随便设的，目的在于当 同时连接多个硬件设备的时候，根据此id进行区分
     * 连接一个蓝牙时，如果有连接线程的话 将该设备 的蓝牙连接线程关闭，
     *
     * @param device
     */
    public synchronized void connect(BluetoothDevice device) {

        if (bSConnected_Receive_Thread != null) {
            bSConnected_Receive_Thread = null;
        }
        if (myConnectThread != null) {
            myConnectThread.cancel();
            myConnectThread = null;
        }
        myConnectThread = new ConnectThread(device);
        myConnectThread.start();
    }

    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    //-------------连接线程---------------------------------------------------------------------------------

    /**
     * 连接各个设备的线程（内部类）
     * 连接成功，取到socket后即可关闭线程
     */
    private class ConnectThread extends Thread {
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);// Get a BluetoothSocket for a connection with the given BluetoothDevice
            } catch (IOException e) {
                Log.e("连接线程内部", "socket未获取成功");
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            setName("连接线程");
            //当连接成功，取消蓝牙适配器搜索蓝牙设备的操作，因为搜索操作非常耗时
            Log.i(TAG, "关闭蓝牙搜索");
            myAdapter.cancelDiscovery();// 关闭搜索因为他会使连接耗时
            try {
                mmSocket.connect();// 官方API启动连接
                Log.e(TAG, "启动socket连接");
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread 连接失败");
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Socket关闭失败");
                }
                return;
            }
            synchronized (BTService.this) {// 连接完成后，重置连接线程
                myConnectThread = null;
                Log.e("连接线程内部", "连接线程关闭");
            }
            if (mmDevice.getName().contains("B19") || mmDevice.getName().contains("B20")) {
                Log.e("连接线程内部", "启动基站数据传输线程");
                IsConnect = true;
                baseStation_Receive_Connected(mmSocket);//如果是基站自动启动开始连接数据

            }

        }

        public void cancel() {
            try {
                mmSocket.close();
                IsConnect = false;
            } catch (IOException ignored) {
            }
        }

    }


    //-------------基站连接成功后线程---------------------------------------------------------------------------------

    private OutputStream bSConnected_Send_OutputStream; //用来点击按钮给BC传数据
    static InputStream is = null;
    static int length;
    static byte[] data = null;
    static byte[] bytesData = null;
    static ByteBuf buf = Unpooled.compositeBuffer();//无限制长度
    static ByteBuffer frameBytes = null;
    static int bufflenth;
    static int bufflenth1;
    static int count = 0;
    int beginReader;//记录包头标志

    /**
     * 基站(Base Station)连接后开始接收数据的线程  尝试修改的第一版
     */
    class BSConnected_Receive_Thread extends Thread {

        private BluetoothSocket BSSocketForRead;

        //构造方法
        public BSConnected_Receive_Thread(BluetoothSocket socket1) {
            BSSocketForRead = socket1;//将类外的Socket传入
            // 获取 BluetoothSocket 输入输出流
            try {
//                is = BSSocketForRead.getInputStream();
                bSConnected_Send_OutputStream = BSSocketForRead.getOutputStream();
                if (bSConnected_Send_OutputStream == null) {
                    Log.e("线程初始化", "bSConnected_Send_OutputStream为空");
                } else {
                    Log.e("线程初始化", "bSConnected_Send_OutputStream不为空");
                }
            } catch (IOException e) {
                Log.i("与基站连接后的输入流", "被中断");
            }
        }

        @Override
        public void run() {
            try {
                //获得串口的输入流
                is = BSSocketForRead.getInputStream();
                long a1 = System.currentTimeMillis();

                while (true) {
                    long a2 = System.currentTimeMillis();
                    //获得数据长度
                    bufflenth1 = is.available();
                    byte[] temp = new byte[bufflenth1];
                    //System.out.println("循环次数"+count+",InputStream里数据长度"+bufflenth);
                    // 获取包头开始的index
                    beginReader = buf.readerIndex();
                    // 标记包头开始的index
                    buf.markReaderIndex();
                    if (buf.readableBytes() > 0) { //证明数据有变化
                        // 读到了协议的开始标志，结束while循环
                        if (buf.readableBytes() == 172) {
                            if (buf.readByte() == (byte) 0xEB) {//如果跳过长度字节后一个为标志位
                                if (buf.readByte() == (byte) 0x90) {//如果长度符合完整报文长度继续执行。
//                                    buf.resetReaderIndex();//还原到包头位置
                                    buf.readBytes(temp);//读取包头标志，因为默认包头已经读过

                                } else {//如果不符合长度要求reset并return，等待剩余数据到来
                                    buf.resetReaderIndex();
                                }
                            }
                        }
                        System.out.println("循环次数：" + count +
                                        ",InputStream里数据长度：" + bufflenth +
//                            ",与上一包的时间差：" + a3 +
                                        ",字节数组长度：" + temp.length +
                                        ",buf长度" + buf.readableBytes()
                        );
                    }
//                    if (bufflenth1 > 0) { //证明数据有变化
//
//                        if (count == 0) {
//                            continue;
//                        }
//                        is.read(temp);
//                        buf.writeBytes(temp);
////                        buf.writeByte(is.read());//每次读取一个字节
////                        is.read();
//                        long a3 = a2 - a1;
//                        a1 = a2;
//
//                        if (buf.readableBytes() >= 172) {
//                            buf.readBytes(new byte[bufflenth1]);
//                            buf.discardReadBytes();
//                        }


                bufflenth = bufflenth1;
                count = count + 1;
            }
        } catch(
        IOException e)

        {
            e.printStackTrace();
        }
    }

}

    public void outputStreamTest(byte[] appToBSBytes) {
        try {
            if (bSConnected_Send_OutputStream != null) {
                bSConnected_Send_OutputStream.write(appToBSBytes);
            } else {
                Log.e(TAG, "outputStreamTest出现异常");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "outputStreamTest出现异常");
        }
    }


    public synchronized void baseStation_Receive_Connected(BluetoothSocket
                                                                   bluetoothSocket) {
        Log.i("BTService", "与基站连接后线程");
        //关闭连接线程
        if (myConnectThread != null) {
            Log.i("", "");
            myConnectThread.cancel();
            myConnectThread = null;
        }
        bSConnected_Receive_Thread = new BSConnected_Receive_Thread(bluetoothSocket);
        bSConnected_Receive_Thread.start();
    }


}
