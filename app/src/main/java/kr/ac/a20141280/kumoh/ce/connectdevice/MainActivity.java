package kr.ac.a20141280.kumoh.ce.connectdevice;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private BluetoothAdapter mBluetoothAdapter;
    static final int REQUEST_ENABLE_BY = 1;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private static final long SCAN_PERIOD = 10000;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    protected ArrayList<DeviceInfo> mArray = new ArrayList<DeviceInfo>();
    protected ListView mList;
    protected DeviceAdapter mAdapter;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getActionBar().setTitle(R.string.title_devices);

        mHandler = new Handler();

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)){
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
            }
        }

        //these device not supported bluetooth_le
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this,R.string.ble_not_supported,Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter == null){
            Toast.makeText(this,R.string.error_bluetooth_not_supported,Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }


    @Override
    protected void onResume(){
        super.onResume();

        mAdapter = new DeviceAdapter(this, R.layout.list_item);
        mList = (ListView)findViewById(R.id.devicelv);
        mList.setAdapter(mAdapter);


        mList.setOnItemClickListener(this);

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtnIntent,REQUEST_ENABLE_BY);
        }

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();
        scanLeDevice(true);
    }

    @Override
    protected void onPause(){
        super.onPause();

        scanLeDevice(false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DeviceInfo deviceInfo = mArray.get(position);
        final BluetoothDevice device = deviceInfo.getDevice();

        if(device == null){
            return;
        }

/*        Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME,device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS,device.getAddress());*/

        Intent intent = new Intent(this, ColorActivity.class);
        intent.putExtra(EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, device.getAddress());
        startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BY){
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(),"Device can use Bluetooth",Toast.LENGTH_LONG).show();

                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();
                scanLeDevice(true);
            }
        }
        else{
            Toast.makeText(getApplicationContext(),"Deivce can not use Bluetooth",Toast.LENGTH_LONG).show();
        }
    }

    private void scanLeDevice(final Boolean enable){
        if(enable){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mLeScanCallBack);
                }
            },SCAN_PERIOD);

            ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("ffffffff-ffff-ffff-ffff-fffffffffff0")).build();     // uuid로 필터링

            filters = new ArrayList<ScanFilter>();
            filters.add(scanFilter);

            settings = new ScanSettings.Builder().build();

            //mBluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
            //test _________________________________________________________________________________

            mLEScanner.startScan(filters,settings,mLeScanCallBack);
        }
        else if(!enable){
            mLEScanner.stopScan(mLeScanCallBack);
        }

        invalidateOptionsMenu();
    }

    private ScanCallback mLeScanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();

            int deviceMajorClass = device.getBluetoothClass().getMajorDeviceClass();

            //더 좋은 방법을 찾으면 수정하여 보자.
            Iterator<DeviceInfo> iterator = mArray.iterator();
            while(iterator.hasNext()){
                if(iterator.next().getDevice().equals(device))
                    return;
            }

            mArray.add(new DeviceInfo(device.getName(),device.getAddress(),deviceMajorClass,device));
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public class DeviceInfo{
        String deviceName;
        String deviceAddress;
        int deviceType;
        BluetoothDevice device;

        public DeviceInfo(String name, String address,int deviceType, BluetoothDevice device){
            this.deviceName=name;
            this.deviceAddress=address;
            this.deviceType = deviceType;
            this.device = device;
        }

        public String getName(){return this.deviceName;}
        public int getDeviceType(){return this.deviceType;}
        public BluetoothDevice getDevice(){return this.device;}
    }

    static class DeviceViewHolder{
        TextView deviceName;
        CircleImageView deviceImage;
    }

    public class DeviceAdapter extends ArrayAdapter<DeviceInfo> {

        private LayoutInflater mInflater = null;

        public DeviceAdapter(Context context, int resource){
            super(context,resource);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() { return mArray.size(); }

        @Override
        public View getView(int position, View v, ViewGroup parent){
            DeviceViewHolder viewHolder;

            if( v == null){
                v = mInflater.inflate(R.layout.list_item,parent,false);

                viewHolder = new DeviceViewHolder();
                viewHolder.deviceImage = (CircleImageView)v.findViewById(R.id.device_Image);
                viewHolder.deviceName = (TextView)v.findViewById(R.id.device_Name);

                v.setTag(viewHolder);
            }
            else{
                viewHolder = (DeviceViewHolder)v.getTag();
            }

            DeviceInfo info = mArray.get(position);

            if(info != null){
                viewHolder.deviceName.setText(info.getName());
                String id="drawable/device"+Integer.toString(info.getDeviceType());
                int imageId = getResources().getIdentifier(id,null,getPackageName());
                Drawable image = getResources().getDrawable(imageId);

                viewHolder.deviceImage.setImageDrawable(image);
            }

            return v;
        }
    }
}
