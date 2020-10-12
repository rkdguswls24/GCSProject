package com.example.dronegcs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaCodec;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.services.android.impl.core.helpers.geoTools.LineLatLong;
import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.droidplanner.services.android.impl.core.survey.grid.CircumscribedGrid;
import org.droidplanner.services.android.impl.core.survey.grid.Trimmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener{
    protected Drone drone;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private boolean connectDrone = false;
    private boolean armstatus = false;
    private boolean altitudeset = false;
    private boolean maplock = false;
    private boolean mapoption = false;
    private boolean mapcads =false;
    private boolean mapfollow = true;
    private boolean togglebtn = false;
    private boolean missionlist = false;
    private LinearLayout btnset;
    private LinearLayout armingbtn;
    private LinearLayout setlist;
    private double dronealtitude=5.5;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button takeoffsetbtn;
    private final Handler handler = new Handler();
    private FusedLocationSource locationSource;
    private NaverMap mymap;
    private ArrayList<LatLng> pathcoords = new ArrayList<>();
    private PathOverlay dronepath;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private Spinner modeSelector;
    private LocationOverlay locationOverlay;
    private ArrayList<String> alertlist = new ArrayList<>();
    private RecyclerView recyclerView;
    private SimpleTextAdapter adapter;

    //polygon
    private boolean mission = false;
    public ArrayList<LatLong> polygonPointList = new ArrayList<LatLong>();
    public ArrayList<LatLong> sprayPointList = new ArrayList<>();
    private MainActivity mainActivity;
    private ManageOverlay manageOverlays;
    private LatLong pointA = null;
    private LatLong pointB = null;
    private double sprayDistance = 5.5f;
    private int maxSprayDistance = 50;
    private int capacity = 0;
    private double sprayAngle;
    private int missioncount=0;

    //Bluetooth
    public BluetoothAdapter mBluetoothAdapter;
    public Set<BluetoothDevice> mDevices;
    private BluetoothSocket bSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private BluetoothDevice mRemoteDevice;
    public boolean onBT = false;
    public byte[] sendByte = new byte[4];
    public TextView tvBT;
    public ProgressDialog asyncDialog;
    private static final int REQUEST_ENABLE_BT = 1;
    private Button BTButton;

    @Nullable
    private LocationManager locationManager;

    @Nullable
    private LocationSource.OnLocationChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.flymode);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }

        });
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }


        if (!connectDrone) {
            armingbtn = (LinearLayout) findViewById(R.id.connectmenu);
            armingbtn.setVisibility(View.INVISIBLE);
        }
        if(armstatus==false){

            setlist= (LinearLayout)findViewById(R.id.takeoffsetlist);
            setlist.setVisibility(View.INVISIBLE);
        }
        takeoffsetbtn = (Button)findViewById(R.id.takeoffset);
        takeoffsetbtn.setText("고도"+dronealtitude);
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        LinearLayout list4 = (LinearLayout)findViewById(R.id.missiondrawer);

        btnset =(LinearLayout)findViewById(R.id.linearLayout3);
        btnset.setVisibility(View.INVISIBLE);
        list1.setVisibility(View.INVISIBLE);
        list2.setVisibility(View.INVISIBLE);
        list3.setVisibility(View.INVISIBLE);
        list4.setVisibility(View.INVISIBLE);
        dronepath = new PathOverlay();

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new SimpleTextAdapter(alertlist);
        recyclerView.setAdapter(adapter);

        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        BTButton = (Button)findViewById(R.id.btnBTCon);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mymap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mymap = naverMap;
        manageOverlays = new ManageOverlay(this.mymap,this);
        // 네이버 로고 위치 변경
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLogoMargin(2080, 0, 0, 925);

        // 나침반 제거
        uiSettings.setCompassEnabled(false);

        // 축척 바 제거
        uiSettings.setScaleBarEnabled(false);

        // 줌 버튼 제거
        uiSettings.setZoomControlEnabled(false);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 핸드폰 맨위 시간, 안테나 타이틀 없애기

        //임무 전송 버튼
        Button btnMission = (Button)findViewById(R.id.sendmission);
        btnMission.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                if(polygonPointList.size()>0) {
                    setMission();
                }else
                    alertUser("need Marker");
            }
        });
        //임무 시작 버튼
        Button btnStartmission = (Button)findViewById(R.id.startmission);
        btnStartmission.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                if(btnStartmission.getText().equals("임무시작")){
                    changetoAutomode();
                    btnStartmission.setText("임무중지");
                }
                else if(btnStartmission.getText().equals("임무중지")){
                    abortmission();
                    btnStartmission.setText("임무시작");
                }
            }
        });

        //Bluetooth 활성 & 페어링 된 기기 검색
        BTButton.setOnClickListener(new Button.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if (!onBT) { //Connect
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter == null) { //장치가 블루투스를 지원하지 않는 경우.
                        Toast.makeText(getApplicationContext(), "Bluetooth 지원을 하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
                    } else { // 장치가 블루투스를 지원하는 경우.
                        if (!mBluetoothAdapter.isEnabled()) {
                            // 블루투스를 지원하지만 비활성 상태인 경우
                            // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            // 블루투스를 지원하며 활성 상태인 경우
                            // 페어링된 기기 목록을 보여주고 연결할 장치를 선택.
                            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                            if (pairedDevices.size() > 0) {
                                // 페어링 된 장치가 있는 경우.
                                selectDevice();
                            } else {
                                // 페어링 된 장치가 없는 경우.
                                Toast.makeText(getApplicationContext(), "먼저 Bluetooth 설정에 들어가 페어링을 진행해 주세요.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }else{ //DisConnect
                    try {
                        BTSend.interrupt();   // 데이터 송신 쓰레드 종료
                        mInputStream.close();
                        mOutputStream.close();
                        bSocket.close();
                        onBT = false;
                        BTButton.setText("connect");
                    } catch(Exception ignored) { }

                }
            }
        });
    }
    //======================================================================================<<블루투스 연결>>=============================================================================================
    public void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        final int mPairedDeviceCount = mDevices.size();

        if (mPairedDeviceCount == 0) {
            //  페어링 된 장치가 없는 경우
            Toast.makeText(getApplicationContext(),"장치를 페어링 해주세요!",Toast.LENGTH_SHORT).show();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 장치 선택");


        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<>();
        for(BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("취소");    // 취소 항목 추가

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == mPairedDeviceCount) {
                    // 연결할 장치를 선택하지 않고 '취소'를 누른 경우
                    //finish();
                }
                else {
                    // 연결할 장치를 선택한 경우
                    // 선택한 장치와 연결을 시도함
                    connectToSelectedDevice(items[item].toString());
                }
            }
        });

        builder.setCancelable(false);    // 뒤로 가기 버튼 사용 금지
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void connectToSelectedDevice(final String selectedDeviceName) {
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);

        //Progress Dialog
        asyncDialog = new ProgressDialog(MainActivity.this);
        asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        asyncDialog.setMessage("블루투스 연결중..");
        asyncDialog.show();
        asyncDialog.setCancelable(false);

        Thread BTConnect = new Thread(new Runnable() {
            public void run() {
                try {
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //HC-06 UUID
                    // 소켓 생성
                    bSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);

                    // RFCOMM 채널을 통한 연결
                    bSocket.connect();

                    // 데이터 송수신을 위한 스트림 열기
                    mOutputStream = bSocket.getOutputStream();
                    mInputStream = bSocket.getInputStream();

                    runOnUiThread(new Runnable() {
                        @SuppressLint({"ShowToast", "SetTextI18n"})
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),selectedDeviceName + " 연결 완료",Toast.LENGTH_LONG).show();
                            tvBT.setText(selectedDeviceName + " Connected");
                            BTButton.setText("disconnect");
                            asyncDialog.dismiss();
                        }
                    });
                    onBT = true;
                }catch(Exception e) {
                    // 블루투스 연결 중 오류 발생
                    runOnUiThread(new Runnable() {
                        @SuppressLint({"ShowToast", "SetTextI18n"})
                        @Override
                        public void run() {
                            tvBT.setText("연결 오류 -- BT 상태 확인해주세요.");
                            asyncDialog.dismiss();
                            Toast.makeText(getApplicationContext(),"블루투스 연결 오류",Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            }
        });
        BTConnect.start();
    }

    public BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : mDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    Thread BTSend  = new Thread(new Runnable() {
        public void run() {
            try {
                mOutputStream.write(sendByte);    // 프로토콜 전송
            } catch (Exception e) {
                // 문자열 전송 도중 오류가 발생한 경우.
            }
        }
    });

    //fixme : 데이터 전송
    public void sendbtData(int btLightPercent) throws IOException {
        //sendBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte[] bytes = new byte[4];
        bytes[0] = (byte) 0xa5;
        bytes[1] = (byte) 0x5a;
        bytes[2] = 1; //command
        bytes[3] = (byte) btLightPercent;
        sendByte = bytes;
        BTSend.run();
    }
    //======================================================================================<<자율주행 미션>>=============================================================================================
    //마커&좌표 넣기
    public void customMission(){
        LatLong stationPointM = new LatLong(35.942268, 126.678938);   //스테이션 좌표
        LatLong stationPoint1 = new LatLong(35.942160, 126.678924);
        LatLong stationPoint2 = new LatLong(35.942147, 126.679024);
        LatLong stationPoint3 = new LatLong(35.942242, 126.679056);

        polygonPointList.add(stationPointM); //좌표를 arrayList에 넣는다.
        polygonPointList.add(stationPoint1);
        polygonPointList.add(stationPoint2);
        polygonPointList.add(stationPoint3);

        manageOverlays.stationMarker();

        dronepath.setCoords(Arrays.asList(   //스테이션 경로
                new LatLng(35.942268, 126.678938),
                new LatLng(35.942160, 126.678924),
                new LatLng(35.942147, 126.679024),
                new LatLng(35.942242, 126.679056),
                new LatLng(35.942268, 126.678938)
        ));

        dronepath.setPatternImage(OverlayImage.fromResource(R.drawable.arrow));
        dronepath.setPatternInterval(70);

        dronepath.setWidth(40);
        dronepath.setOutlineWidth(10);
        dronepath.setOutlineColor(Color.TRANSPARENT);

        dronepath.setMap(mymap);

        alertUser("스테이션M 좌표:" + polygonPointList.get(0).getLatitude());
        alertUser("스테이션1 좌표:" + polygonPointList.get(1).getLatitude());
        alertUser("스테이션2 좌표:" + polygonPointList.get(2).getLatitude());
        alertUser("스테이션3 좌표:" + polygonPointList.get(3).getLatitude());
    }

    //setmission
    public void setMission(){
        Mission mMission = new Mission();
        for(int i=0;i<polygonPointList.size();i++){
            Waypoint waypoint = new Waypoint();
            waypoint.setDelay(1);

            LatLongAlt latLongAlt = new LatLongAlt(polygonPointList.get(i).latitude,polygonPointList.get(i).longitude,dronealtitude);
            waypoint.setCoordinate(latLongAlt);

            mMission.addMissionItem(waypoint);
        }
        MissionApi.getApi(this.drone).setMission(mMission,true);
        //MissionApi.getApi(this.drone).setMissionSpeed(0.5);
    }

    //startmission
    public void changetoAutomode(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO,new SimpleCommandListener(){
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }


            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드 변경 실패.");
            }
        });
    }
    //stop mission
    public void abortmission(){
        MissionApi.getApi(this.drone).pauseMission(null);
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER,new SimpleCommandListener(){
            @Override
            public void onSuccess(){
                alertUser("Loiter mode");

            }
            @Override
            public void onError(int executionError) {
                alertUser("Loiter 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패.");
            }
        });
    }

    //gettoplatform
    public void getPlat(){
        alertUser("getPlat 실행");

//        Speed ltspeed =  this.drone.getAttribute(AttributeType.SPEED);
//        ltspeed.setGroundSpeed(0.5);
//        alertUser("하강 속도:" + ltspeed.getGroundSpeed() + "m/s");

        ChangeGuideMode();
        alertUser("Change to GuideMode");

        ControlApi.getApi(this.drone).climbTo(3);
        alertUser("approaching to the platform...");

        try{
            Thread.sleep(3000); //wait for onboard signal
            ControlApi.getApi(this.drone).climbTo(dronealtitude);
            alertUser("leaving...Platform");

            Thread.sleep(5000);
            changetoAutomode();
            alertUser("Change to AutoMode");

        }catch(InterruptedException e){
            alertUser("sleep denied");
            ControlApi.getApi(this.drone).climbTo(dronealtitude);

            changetoAutomode();
            alertUser("Change to AutoMode");
    }
    }
/*
    private void pauseMission() {
        MissionApi.getApi(this.drone).pauseMission(null);
    }*/

private void ChangeGuideMode() {
    VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
        @Override
        public void onSuccess() {
            alertUser("Guide 모드로 변경 중...");
        }

        @Override
        public void onError(int executionError) {
            alertUser("Guide 모드 변경 실패 : " + executionError);
        }

        @Override
        public void onTimeout() {
            alertUser("Guide 모드 변경 실패");
        }
    });
}
    private void ChangeToLoiterMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Loiter 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Loiter 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패");
            }
        });
    }
    //========================================================================================<<드론 이벤트>>===========================================================================================
    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                break;
            case AttributeEvent.STATE_UPDATED:

                break;
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;
            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;
            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();

                break;
            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;
            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;
            case AttributeEvent.GPS_POSITION:
                updatetrack();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                updateVolt();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;
            case AttributeEvent.GPS_COUNT:

                updateNumberOfSatellites();
                break;
            case AttributeEvent.MISSION_SENT:
                alertUser("mission upload succ");
                break;
            case AttributeEvent.MISSION_ITEM_REACHED:
                alertUser("목표지점 도달 getPlat 실행 준비");
                getPlat();
                missioncount++;
                alertUser("현재 missincount:" + missioncount);
                if(missioncount==polygonPointList.size())
                {
                    missioncount = 0;
                    changetoAutomode();
                }
                //getPlat();
            case AttributeEvent.MISSION_UPDATED:
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    //button event list
    public void btn_event(View v){
        LinearLayout missiondrawlist = (LinearLayout)findViewById(R.id.missiondrawer);
        switch(v.getId()){
            case R.id.connect:
                onBtnConnectTap();
                break;
            case R.id.arm:
                onArmButtonTap();
                break;
            case R.id.land:
                onLandButtonTap();
                break;
            case R.id.takeoffset:
                takeoffsetTap();
                break;
            case R.id.drone_asec:
                onAsecTap();
                break;
            case R.id.drone_desc:
                onDescTap();
                break;
            case R.id.maplockbtn:

                LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);

                onlistbtnTap(list);
                break;
            case R.id.mapoptionbtn:

                LinearLayout list1 = (LinearLayout)findViewById(R.id.mapoptionlayer);
                onlistbtnTap(list1);
                break;
            case R.id.mapcadastral:

                LinearLayout list2 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
                onlistbtnTap(list2);
                break;
            case R.id.maplock:
                mapfollow = true;
                mapfollowTap();
                break;
            case R.id.mapmove:
                mapfollow = false;
                mapfollowTap();
                break;
            case R.id.basicmap:
                onMapOptionTap(R.id.basicmap);
                break;
            case R.id.satellitemap:
                onMapOptionTap(R.id.satellitemap);
                break;
            case R.id.hybridmap:
                onMapOptionTap(R.id.hybridmap);
                break;
            case R.id.cadaston:
                onCadastTap(R.id.cadaston);
                break;
            case R.id.cadastoff:
                onCadastTap(R.id.cadastoff);
                break;
            case R.id.toggle:
                onToggleTap();
                break;
            case R.id.mission:
                missionlist = !missionlist;

                if(missiondrawlist.getVisibility()==View.INVISIBLE)
                    missiondrawlist.setVisibility(View.VISIBLE);
                else
                    missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.nomission:
                resetMarker();
                polygonPointList.clear();
                dronepath.setMap(null);
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.custom:
                customMission();
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;

        }
    }
    //=============================================================================================<<Helper>>>>=====================================================================================================
    public void resetMarker(){
        manageOverlays.reset();
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
        alertlist.add(message);

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(alertlist.size()-1);
    }

    public void onLandButtonTap(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        }
    }

    public void takeoffsetTap(){

        altitudeset = !altitudeset;
        if(altitudeset)
        {
            setlist.setVisibility(View.VISIBLE);
        }
        else
            setlist.setVisibility(View.INVISIBLE);
    }
    public void onAsecTap(){

        if(dronealtitude<10){
            dronealtitude += 0.5;
            takeoffsetbtn.setText("이륙고도"+dronealtitude);
        }
    }
    public void onDescTap(){

        if(dronealtitude>3){
            dronealtitude -= 0.5;
            takeoffsetbtn.setText("이륙고도"+dronealtitude);
        }

    }
    public void alertMessage(){
        Drone mydrone = this.drone;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("arming alert");
        builder.setMessage("모터를 가동합니다");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                VehicleApi.getApi(mydrone).arm(true, false, new SimpleCommandListener() {
                    @Override
                    public void onError(int executionError) {
                        alertUser("Unable to arm vehicle.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Arming operation timed out.");
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    public void takeoffAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Drone mydrone = this.drone;
        builder.setTitle("takeoff alert");
        builder.setMessage("기체가 상승합니다 안전거리 유지 바랍니다.");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ControlApi.getApi(mydrone).takeoff(dronealtitude,new AbstractCommandListener(){
                    @Override
                    public void onSuccess() {
                        alertUser("Taking off...");
                    }

                    @Override
                    public void onError(int i) {
                        alertUser("Unable to take off.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Unable to take off.");
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();

            }
        });

        AlertDialog alertdialog = builder.create();
        alertdialog.show();
    }
    public void onArmButtonTap() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isArmed()) {
            // Take off
            takeoffAlert();

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            alertMessage();
            // Connected but not Armed

        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    protected void updatetrack(){
        try{

            Gps dronegps = this.drone.getAttribute(AttributeType.GPS);
            LatLng droneposition = new LatLng(dronegps.getPosition().getLatitude(),dronegps.getPosition().getLongitude());

            Log.d("GPSERROR1",""+droneposition.latitude);
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.gcsmarker));
            locationOverlay.setPosition(droneposition);
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(droneposition));
        }catch(NullPointerException e){
            Log.d("GPSERROR","GPS POSITION NULL");
            // locationOverlay = mymap.getLocationOverlay();
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.gcsmarker));
            locationOverlay.setPosition(new LatLng(35.945378,126.682110));
            //locationOverlay.setAnchor(new PointF((float)0.5,(float)0.5));
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(new LatLng(35.945378,126.682110)));

        }
        //
        //mymap.setLocationTrackingMode(LocationTrackingMode.Follow);
    }
    protected void updateNumberOfSatellites() {
        TextView numberOfSatellitesTextView = (TextView)findViewById(R.id.satenum);
        Gps droneNumberOfSatellites = this.drone.getAttribute(AttributeType.GPS);

        numberOfSatellitesTextView.setText(String.format("%d", droneNumberOfSatellites.getSatellitesCount()));
    }
    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.arm);

        if (!this.drone.isConnected()) {
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            armingbtn.setVisibility(View.VISIBLE);
        }


        if (vehicleState.isArmed()) {
            // Take off

            armButton.setText("TAKE-OFF");

        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect((TowerListener) this);

    }
    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();

    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.connect);
        if (isConnected) {
            connectButton.setText("Disco");
            connectDrone = false;
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            connectButton.setText("Conn");
            connectDrone = true;
            armingbtn.setVisibility(View.VISIBLE);
        }
    }

    public void onBtnConnectTap() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }

    }

    public void onToggleTap(){
        togglebtn = !togglebtn;
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        if(togglebtn){
            btnset.setVisibility(View.VISIBLE);
        }
        else{
            maplock = false;
            mapoption = false;
            mapcads = false;
            list1.setVisibility(View.INVISIBLE);
            list2.setVisibility(View.INVISIBLE);
            list3.setVisibility(View.INVISIBLE);
            btnset.setVisibility(View.INVISIBLE);
        }

    }
    public void onCadastTap(int id){
        Button cadastbtn = (Button)findViewById(R.id.mapcadastral);
        LinearLayout list = (LinearLayout)findViewById(R.id.mapcadstrallayer);

        switch(id){
            case R.id.cadaston:
                mymap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                cadastbtn.setText("지적도on");
                break;
            case R.id.cadastoff:
                mymap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                cadastbtn.setText("지적도off");
                break;
        }
        mapcads = false;
        list.setVisibility(View.INVISIBLE);
    }
    public void onMapOptionTap(int id){
        Button mapoptionbtn = (Button)findViewById(R.id.mapoptionbtn);
        LinearLayout list = (LinearLayout)findViewById(R.id.mapoptionlayer);

        switch(id)
        {
            case R.id.basicmap:
                mymap.setMapType(NaverMap.MapType.Basic);
                mapoptionbtn.setText("기본지도");
                break;
            case R.id.satellitemap:
                mymap.setMapType(NaverMap.MapType.Satellite);
                mapoptionbtn.setText("위성지도");
                break;
            case R.id.hybridmap:
                mymap.setMapType(NaverMap.MapType.Hybrid);
                mapoptionbtn.setText("hybrid");
                break;

        }
        mapoption = false;
        list.setVisibility(View.INVISIBLE);
    }
    public void mapfollowTap(){
        Button lockbtn = (Button)findViewById(R.id.maplockbtn);
        LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);

        if(mapfollow)
            lockbtn.setText("맵 잠금");
        else
            lockbtn.setText("맵 이동");

        maplock = false;
        list.setVisibility(View.INVISIBLE);
    }

    public void onlistbtnTap(LinearLayout list){
        if(list.getVisibility() == View.INVISIBLE){
            list.setVisibility(View.VISIBLE);
        }
        else{
            list.setVisibility(View.INVISIBLE);
        }
    }


    protected void updateAltitude() {

        TextView altitudeTextView = (TextView) findViewById(R.id.altitude);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");



    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speed);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");


    }

    protected void updateVolt(){
        TextView voltTextView = (TextView)findViewById(R.id.volt);
        Battery droneVolt = this.drone.getAttribute(AttributeType.BATTERY);
        voltTextView.setText(String.format("%3.2f",droneVolt.getBatteryVoltage())+"V");
    }

    protected void updateYaw(){
        double yawvalue=0;
        TextView yawTextView = (TextView)findViewById(R.id.YAW1);
        Attitude droneyaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if(droneyaw.getYaw()<0)
            yawvalue = droneyaw.getYaw()+360;
        else
            yawvalue = droneyaw.getYaw();

        yawTextView.setText(String.format("%3.1f",yawvalue));
        locationOverlay.setBearing((float) droneyaw.getYaw());
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

}


