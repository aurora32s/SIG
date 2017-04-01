package kr.ac.a20141280.kumoh.ce.connectdevice;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private BluetoothAdapter mBluetoothAdapter;
    static final int REQUEST_ENABLE_BY = 1;
    private BluetoothLeScanner mLEScanner;
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
            ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("ffffffff-ffff-ffff-ffff-fffffffffff0")).build();     // uuid로 필터링
            filters = new ArrayList<ScanFilter>();
            filters.add(scanFilter);

            //ble꺼지면 탐지하기 위해 setting에 setCallbackType 추가해야됨. api레벨이 23이라 못달았음.
            settings = new ScanSettings.Builder().build();

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
            this.deviceName = name;
            this.deviceAddress = address;
            this.deviceType = deviceType;
            this.device = device;
        }

        public String getName(){ return this.deviceName; }
        public int getDeviceType(){ return this.deviceType; }
        public BluetoothDevice getDevice(){ return this.device; }
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

    //메뉴 버튼을 위한 코드
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_bleOn) {
            Toast.makeText(getApplicationContext(), "BLE on", Toast.LENGTH_SHORT).show();

            LightUpTask lightUpTask = new LightUpTask();
            lightUpTask.execute();
        }
        else if (id == R.id.action_setProgram) {
            Toast.makeText(getApplicationContext(), "program setting", Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class LightUpTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpURLConnection conn = null;
            try {
                String body = "UserID=...&Password=...&BLE_Status=...";            // POST로 보낼 내용 정의
                URL serverUrl = new URL("http://202.31.200.180:3000/API/BLE");                 // 서버 URL 정의

                conn = (HttpURLConnection) serverUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                OutputStream os = conn.getOutputStream();
                os.write( body.getBytes("euc-kr") );
                os.flush();
                os.close();

                int status = conn.getResponseCode();
                Log.i("seung", "status code - " + Integer.toString(status));

                BufferedReader br = new BufferedReader( new InputStreamReader( conn.getInputStream(), "EUC-KR" ), conn.getContentLength() );
                Log.i("seung", "log - " + br.toString());
                br.close();


            } catch (Exception e) {
                e.printStackTrace();
                Log.i("seung", "error >> " + e.toString());

            } finally {
                if(conn != null) {
                    conn.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }
}
