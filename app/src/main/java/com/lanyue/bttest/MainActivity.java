package com.lanyue.bttest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.lanyue.bttest.util.PairBLTUtil;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String TAG="MainActivity";
    ProgressDialog progressDialog;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        检查
        boolean haveInstallPermission = getPackageManager().canRequestPackageInstalls();
        if (!haveInstallPermission) {
            startInstallPermissionSettingActivity(this);
        }
        checkPermission();
    }
    private void checkPermission() {

        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN};
        boolean coarsePermmision = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean finePermmision = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean BLUETOOTH = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        boolean BLUETOOTH_ADMIN = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;

        if (!BLUETOOTH || !BLUETOOTH_ADMIN || !coarsePermmision || !finePermmision) {
            ActivityCompat.requestPermissions(this, permissions, 11);
        }
        Button bsState_ConstraintLayout = findViewById(R.id.findBTDevice);
        bsState_ConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    startDiscovery();
            }
        });
    }

    /**
     * 开启设置安装未知来源应用权限界面
     *
     * @param context
     */
    public static void startInstallPermissionSettingActivity(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent();
        //获取当前apk包URI，并设置到intent中（这一步设置，可让“未知应用权限设置界面”只显示当前应用的设置项）
        Uri packageURI = Uri.parse("package:" + context.getPackageName());
        intent.setData(packageURI);
        //设置不同版本跳转未知应用的动作
        if (Build.VERSION.SDK_INT >= 26) {
            //intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,packageURI);
            intent.setAction(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        } else {
            intent.setAction(android.provider.Settings.ACTION_SECURITY_SETTINGS);
        }
        ((Activity) context).startActivityForResult(intent, 100);
        //ToastUtils.showLong("请开启未知应用安装权限");
    }
    /**
     * 注册异步搜索蓝牙设备的广播
     */
    private void startDiscovery() {
        // 找到设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // 注册广播
        registerReceiver(receiver, filter);
        // 搜索完成的广播
//        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        // 注册广播
//        registerReceiver(receiver, filter1);
        Log.i(TAG, "startDiscovery: 注册广播");
        startScanBluetooth();
    }

    private static List<BluetoothDevice> mBlueList = new ArrayList<>();
    //蓝牙专用
    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//蓝牙适配器代表本机的蓝牙
    private final static int SEARCH_CODE = 0x123;
    public static BTService mBluetoothService; //自定义蓝牙服务类
    /**
     * 广播接收器
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 收到的广播类型
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                // 如果是发现蓝牙设备的广播
                if (BluetoothDevice.ACTION_FOUND.equals(action) && device.getName() != null && (device.getName().contains("B19") || device.getName().contains("B20"))) {
                    // 如果采点器没配对，自动配对
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        try {
                            PairBLTUtil.createBond(device.getClass(), device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!mBlueList.contains(device)) {
                        mBlueList.add(device);
                        if (mBlueList.size() >= 1) {
                            registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
                            mBluetoothAdapter.cancelDiscovery();

                            mBluetoothService = new BTService(myHandler);
                            Log.i("BTService", "连接线程启动");
                            mBluetoothService.connect(device);
                        }
                    }
//                textView1.setText("附近的采点器设备：" + mBlueList.size() + "个\u3000\u3000");

//                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
////              刷新蓝牙列表
//                RecyclerView recyclerView = findViewById(R.id.recycler_view);
//                recyclerView.setLayoutManager(linearLayoutManager);
//                BTRecyclerViewAdapter BTRecyclerViewAdapter = new BTRecyclerViewAdapter(mBlueList);
////                    recyclerView.addItemDecoration(new DividerItemDecoration(context,DividerItemDecoration.VERTICAL));
//                recyclerView.setAdapter(BTRecyclerViewAdapter);
//刷新坐标
                    Log.i(TAG, "onReceive: " + mBlueList.size());
                    Log.i(TAG, "onReceive: " + (device.getName() + ":" + device.getAddress() + " ：" + "m" + "\n"));
                    // 搜索完成
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // 关闭进度条
                progressDialog.dismiss();

                Log.i(TAG, "onReceive: 搜索完成");
            }

        }

    };
    MyHandler myHandler;
    /**
     * 搜索蓝牙
     */
    private void startScanBluetooth() {
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 开始搜索
        mBluetoothAdapter.startDiscovery();

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage("正在搜索，请稍后！");
        progressDialog.show();

    }
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> MainActivityWeakReference;

        public MyHandler(MainActivity activity) {
            MainActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity MainActivity = MainActivityWeakReference.get();
            if (MainActivity != null) {
                switch (msg.what) {

                }
//            return false;
            }
        }
    }
}