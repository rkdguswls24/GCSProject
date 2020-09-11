package com.example.dronegcs;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;

import android.graphics.Color;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener{
    private boolean dronestate = false;
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
    private double dronealtitude=5;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button takeoffsetbtn;
    private final Handler handler = new Handler();
    private FusedLocationSource locationSource;
    private NaverMap mymap;
    private GuideMode guide;
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
    private boolean[] mission = {false , false};
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

    //Mission
    public ArrayList<LatLong> stationPointList = new ArrayList<LatLong>();


    @Nullable
    private LocationManager locationManager;

    @Nullable
    private LocationSource.OnLocationChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 핸드폰 맨위 시간, 안테나 타이틀 없애기

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
        LinearLayout list5= (LinearLayout)findViewById(R.id.missionContent);

        btnset =(LinearLayout)findViewById(R.id.linearLayout3);
        btnset.setVisibility(View.INVISIBLE);
        list1.setVisibility(View.INVISIBLE);
        list2.setVisibility(View.INVISIBLE);
        list3.setVisibility(View.INVISIBLE);
        list4.setVisibility(View.INVISIBLE);
        list5.setVisibility(View.INVISIBLE);
        guide = new GuideMode();
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
        mymap.setOnMapLongClickListener((pointF, latLng) -> {
            droneguide(latLng);
        });
        
        mymap.setOnMapClickListener((pointF, latLng) -> {
            LatLong latlong = new LatLong(latLng.latitude,latLng.longitude);
            polygonMission(latlong);
            abMission(latlong);

        });

    }

    // ############################################################################################임무수행###########################################################################################################
    public void GoMission(){
       //Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
       //LatLong droneLocation = new LatLong(droneGps.getPosition().getLatitude(),droneGps.getPosition().getLongitude());
       //alertUser("드론 경도:" + droneLocation.getLatitude() + "드론 위도" + droneLocation.getLongitude());

       //스테이션 좌표 설정
       LatLong stationPointM = new LatLong(35.942293, 126.683031);   //스테이션 좌표
       LatLong stationPoint1 = new LatLong(35.942305, 126.682317);
       LatLong stationPoint2 = new LatLong(35.941884, 126.682273);
       LatLong stationPoint3 = new LatLong(35.941861, 126.683013);

       stationPointList.add(stationPointM); //좌표를 arrayList에 넣는다.
       stationPointList.add(stationPoint1);
       stationPointList.add(stationPoint2);
       stationPointList.add(stationPoint3);

       manageOverlays.stationMarker();

        dronepath.setCoords(Arrays.asList(   //스테이션 경로
                new LatLng(35.942293, 126.683031),
                new LatLng(35.942305, 126.682317),
                new LatLng(35.941884, 126.682273),
                new LatLng(35.941861, 126.683013),
                new LatLng(35.942293, 126.683031)
        ));

        dronepath.setPatternImage(OverlayImage.fromResource(R.drawable.arrow));
        dronepath.setPatternInterval(70);

        dronepath.setWidth(40);
        dronepath.setOutlineWidth(10);
        dronepath.setOutlineColor(Color.TRANSPARENT);

        dronepath.setMap(mymap);

/*
       //드론버스 이동 및 착륙&이륙 알고리즘
       if(dronestate){
           AlertDialog.Builder Message = new AlertDialog.Builder(MainActivity.this);
           Message.setTitle("비행버스 임무 수행").setMessage("확인하시면 가이드모드 전환후 기체가 이동합니다.").setPositiveButton("확인", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   // Action for 'Yes' Button
                   VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                           new AbstractCommandListener() {
                               @Override
                               public void onSuccess() {
                                   ControlApi.getApi(drone).goTo(stationPointM, true, null);
                                   if (stationPointList.size() == 4) {
                                       if (droneLocation == stationPointM) {
                                           ControlApi.getApi(drone).goTo(stationPoint1, true, null);
                                           //if(사람이 존재하면){
                                           //Landing_TakeOff();
                                           //}
                                       } else if (droneLocation == stationPoint1) {
                                           ControlApi.getApi(drone).goTo(stationPoint2, true, null);
                                           //Landing_TakeOff();
                                       } else if (droneLocation == stationPoint2) {
                                           ControlApi.getApi(drone).goTo(stationPoint3, true, null);
                                           //Landing_TakeOff();
                                       } else if (droneLocation == stationPoint3) {
                                           ControlApi.getApi(drone).goTo(stationPointM, true, null);
                                           //Landing_TakeOff();
                                       }
                                   }
                               }

                               @Override
                               public void onError(int executionError) {
                                   alertUser("Mission failed: " + executionError);
                               }

                               @Override
                               public void onTimeout() {
                               }
                           });
               }
           }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   dialog.cancel();

               }
           });
           AlertDialog alert = Message.create();
           // Title for AlertDialog
           alert.setTitle("Title");
           // Icon for AlertDialog

           alert.show();
       }*/
   }
/*
   public void Landing_TakeOff(){
       fallAltitude();
       new Handler().postDelayed(new Runnable() {
           @Override
           public void run() {
               riseAltitude();
           }
       }, 5000); // 3초 지연을 준 후 시작
   }

    //기체 상승/하강 기능
    public void riseAltitude() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Altitude currentAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        if (vehicleState.isFlying()) {
            ControlApi.getApi(this.drone).climbTo(currentAltitude.getAltitude() + 4.5);
        }
    }

    public void fallAltitude() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Altitude currentAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        if (vehicleState.isFlying()) {
            if (currentAltitude.getAltitude() > 0)
                ControlApi.getApi(this.drone).climbTo(currentAltitude.getAltitude() - 4.5);
        }
    }

    public  void StopMission() {
        final Button BtnSendMission = (Button) findViewById(R.id.stopMission);

        if (BtnSendMission.getText().equals("임무중지")){
            BtnSendMission.setText("임무\n재시작");

            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("임무를 중지합니다.");
                }

                @Override
                public void onError(int executionError) {
                    alertUser("임무중지 실패 : " + executionError);
                }

                @Override
                public void onTimeout() {

                }
            });
        }else if(BtnSendMission.getText().equals("임무\n재시작")){
            BtnSendMission.setText("임무중지");

            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("임무를 재시작합니다.");
                }

                @Override
                public void onError(int executionError) {
                    alertUser("임무시작 실패 : " + executionError);
                }

                @Override
                public void onTimeout() {

                }
            });
        }
    }

    public  void EndMission(){

    }*/
    // ########################################################################################### 드론 ###############################################################################################
    //드론 위치 오버레이
    protected void updatetrack(){
        try{
            Gps dronegps = this.drone.getAttribute(AttributeType.GPS);
            LatLng droneposition = new LatLng(dronegps.getPosition().getLatitude(),dronegps.getPosition().getLongitude());

            Log.d("GPSERROR1",""+droneposition.latitude);
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.bus));
            locationOverlay.setPosition(droneposition);
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(droneposition));
        }catch(NullPointerException e){
            Log.d("GPSERROR","GPS POSITION NULL");
            locationOverlay = mymap.getLocationOverlay();
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.bus));
            locationOverlay.setPosition(new LatLng(35.945378,126.682110));
            //locationOverlay.setAnchor(new PointF((float)0.5,(float)0.5));
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(new LatLng(35.945378,126.682110)));
        }
    }
    // ################################################################################################### 추가기능 ################################################################################################
    //이륙고도 설정
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
    //비행모드 변경
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
    // ########################################################################################### 드론 기본 구동 동작 ################################################################################################
    //드론과 디바이스 연결
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
    //Arming
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

    //드론 이륙
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

    //드론 착륙
    public void onLandButtonTap(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            alertUser("착륙 중...");
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

    //최종
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
    // ################################################################################################## 맵 옵션 ##########################################################################################
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
    // ################################################################################################## 기체속성 ##########################################################################################
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
    // ################################################################################################## Helper/ETC ##########################################################################################
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
        alertlist.add(message);

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(alertlist.size()-1);
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

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }
    // ###################################################################################### 기체 상태에 따른 이벤트 실행 ##########################################################################################
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
                dronestate = mydronestate();
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
                delMarker();
                pathline();
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

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }
    // ###################################################################################### 버튼 클릭 -> 이벤트 실행 ##########################################################################################
    //button event list
    public void btn_event(View v){
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
                LinearLayout missiondrawlist = (LinearLayout)findViewById(R.id.missiondrawer);
                if(missionlist)
                    missiondrawlist.setVisibility(View.VISIBLE);
                else
                    missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.nomission:
                resetMarker();
                polygonPointList.clear();
                missionClear();

                LinearLayout missiondrawlist1 = (LinearLayout)findViewById(R.id.missiondrawer);
                missiondrawlist1.setVisibility(View.INVISIBLE);
                break;
            case R.id.PolygonMission:
                mission[1] = true;

                LinearLayout missiondrawlist2 = (LinearLayout)findViewById(R.id.missiondrawer);
                missiondrawlist2.setVisibility(View.INVISIBLE);
                break;
            case R.id.ABMission:
                mission[0] = true;

                LinearLayout missiondrawlist3 = (LinearLayout)findViewById(R.id.missiondrawer);
                missiondrawlist3.setVisibility(View.INVISIBLE);
                break;
            case R.id.PerformMission:
                GoMission();

                LinearLayout missiondrawlist4 = (LinearLayout)findViewById(R.id.missiondrawer);
                missiondrawlist4.setVisibility(View.INVISIBLE);

                LinearLayout missionContent1 = (LinearLayout)findViewById(R.id.missionContent);
                missionContent1.setVisibility(View.VISIBLE);
                break;
            case R.id.stopMission:
                //StopMission();
                break;
            case R.id.endMission:
                //EndMission();
                stationPointList.clear();
                manageOverlays.resetMarker();
                dronepath.setMap(null);
                LinearLayout missionContent2 = (LinearLayout)findViewById(R.id.missionContent);
                missionContent2.setVisibility(View.INVISIBLE);
                break;
        }
    }
    // ###################################################################################### 가이드 모드 #######################################################################################
    class GuideMode {
        LatLng mGuidedPoint; //가이드모드 목적지 저장
        Marker mMarkerGuide = new Marker(); //GCS 위치 표마커 옵션

        void DialogSimple(final Drone drone, final LatLong point) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
            alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Action for 'Yes' Button
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                            new AbstractCommandListener() {
                                @Override

                                public void onSuccess() {

                                    ControlApi.getApi(drone).goTo(point, true, null);
                                }
                                @Override
                                public void onError(int i) {

                                }
                                @Override
                                public void onTimeout() {
                                }
                            });
                }
            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();

                }
            });
            AlertDialog alert = alt_bld.create();
            // Title for AlertDialog
            alert.setTitle("Title");
            // Icon for AlertDialog

            alert.show();
        }
        public boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
            LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                    guidedState.getCoordinate().getLongitude());
            return target.distanceTo(recentLatLng) <= 1;
        }
    }

    //guidemode
    public boolean mydronestate(){
        State vehiclestate = this.drone.getAttribute(AttributeType.STATE);
        if(vehiclestate.isArmed())
            return true;
        else
            return false;
    }

    public void droneguide(LatLng latLng){

        if(dronestate){
            guide.mGuidedPoint = latLng;
            guide.mMarkerGuide.setPosition(latLng);
            guide.mMarkerGuide.setMap(mymap);
            guide.DialogSimple(this.drone,new LatLong(latLng.latitude,latLng.longitude));
        }



    }
    public void delMarker(){
        try{
            if(guide.CheckGoal(this.drone,guide.mGuidedPoint))
            {
                guide.mMarkerGuide.setMap(null);
                VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지 도착");
                    }

                    @Override
                    public void onError(int executionError) {

                    }

                    @Override
                    public void onTimeout() {

                    }
                });

            }
        }catch(NullPointerException e){
            Log.d("NONMARKER","no marker exist");
        }

    }
    //경로선
    public void pathline(){
        Gps dronegps = this.drone.getAttribute(AttributeType.GPS);
        LatLng droneposition = new LatLng(dronegps.getPosition().getLatitude(),dronegps.getPosition().getLongitude());
        try{
            pathcoords.add(droneposition);
            dronepath.setCoords(pathcoords);

            dronepath.setMap(mymap);

            Log.d("DRONEPATH","list size:"+pathcoords.size());
        }catch(NullPointerException e){
            Log.d("DRONEPATH","gps position list is null");
        }


    }
    // ###################################################################################### A-B 지점 / 폴리곤 임무 ##########################################################################################
    //drawpolygon
    //알고리즘
    public void polygonMission(LatLong latLong){
        if(mission[1]==true){
            mission[0] = false;
            addPolygonPoint(latLong);
        }
    }
    public void abMission(LatLong latLong){
        double angle1 = 1, angle2 = 1;
        int direction = 1;
        if(mission[0]==true)
        {
            polygonPointList.add(latLong);

            manageOverlays.setPPosition(latLong);

            mission[1] = false;
            if (polygonPointList.size() == 2) {
                angle1 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(0), polygonPointList.get(1));
                LatLong newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(1), angle1 - (90 * direction), 100);
                //addPolygonPoint(newPoint);
                addABPoint(newPoint);


                //angle2 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(1), polygonPointList.get(0));
                newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(0), angle1 - 90, 100);
                //addPolygonPoint(newPoint);
                addABPoint(newPoint);
            }
        }

    }
    public void missionClear(){
        mission[0] = false;
        mission[1] = false;
    }
    public void addABPoint(LatLong latLong){
        polygonPointList.add(latLong);

        manageOverlays.setPPosition(latLong);
        sprayAngle = MathUtils.getHeadingFromCoordinates(polygonPointList.get(0), polygonPointList.get(1));;
        try{
            makeGrid();
        }catch(Exception e){
            Log.d("myCheck","예외처리 : " + e.getMessage());
        }
    }

    public void addPolygonPoint(LatLong latLong) {



        polygonPointList.add(latLong);

        manageOverlays.setPPosition(latLong);


        if (polygonPointList.size() == 1) {

        }



        if (polygonPointList.size() > 2) {
            manageOverlays.drawPolygon();
            sprayAngle = makeSprayAngle();

            try {
                makeGrid();
                // makeBound();
            } catch(Exception e) {
                Log.d("myCheck","예외처리 : " + e.getMessage());
            }
        }
    }
    //폴리곤 라인 가장 긴 길이 찾아서 각도 반환
    protected double makeSprayAngle() {
        Polygon poly = makePoly();
        double angle = 0;
        double maxDistance = 0;
        List<LineLatLong> lineLatLongList = poly.getLines();
        for (LineLatLong lineLatLong : lineLatLongList) {
            double lineDistance = MathUtils.getDistance2D(lineLatLong.getStart(), lineLatLong.getEnd());
            if(maxDistance < lineDistance) {
                maxDistance = lineDistance;
                angle = lineLatLong.getHeading();
            }
        }
        Log.d("mycheck",""+angle);
        return angle;
    }

    private Polygon makePoly() {
        Polygon poly = new Polygon();
        List<LatLong> latLongList = new ArrayList<>();
        for(LatLong latLong : polygonPointList) {
            latLongList.add(latLong);
        }
        poly.addPoints(latLongList);
        return poly;
    }

    public void makeGrid() throws Exception {
        if(this == null) throw new Exception("PolygonSpray retreiving MapActivity returns null");

        List<LatLong> polygonPoints = new ArrayList<>();
        for(LatLong latLong : polygonPointList) {
            polygonPoints.add(latLong);
        }

        List<LineLatLong> circumscribedGrid = new CircumscribedGrid(polygonPoints, this.sprayAngle, sprayDistance).getGrid();
        List<LineLatLong> trimedGrid = new Trimmer(circumscribedGrid, makePoly().getLines()).getTrimmedGrid();

        for (int i = 0; i < trimedGrid.size(); i++) {
            LineLatLong line = trimedGrid.get(i);
            if(line.getStart().getLatitude() > line.getEnd().getLatitude()) {
                LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
                trimedGrid.set(i, line1);
            }
        }

        LatLong dronePosition = new LatLong(37.5670135, 126.9783740);
        double dist1 = MathUtils.pointToLineDistance(trimedGrid.get(0).getStart(), trimedGrid.get(0).getEnd(), dronePosition);
        double dist2 = MathUtils.pointToLineDistance(trimedGrid.get(trimedGrid.size()-1).getStart(), trimedGrid.get(trimedGrid.size()-1).getEnd(), dronePosition);

        if (dist2 < dist1) {
            Collections.reverse(trimedGrid);
            double distStart = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getStart());
            double distEnd = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getEnd());
            if (distStart > distEnd) {
                for (int i = 0; i < trimedGrid.size(); i++) {
                    LineLatLong line = trimedGrid.get(i);
                    LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
                    trimedGrid.set(i, line1);
                }
            }
        }

        for (int i = 0; i < trimedGrid.size(); i++) {
            LineLatLong line = trimedGrid.get(i);
            if (i % 2 != 0) {
                line = new LineLatLong(line.getEnd(), line.getStart());
                trimedGrid.set(i,line);
            }
        }

        sprayPointList.clear();
        for(LineLatLong lineLatLong : trimedGrid) {
            sprayPointList.add(lineLatLong.getStart());
            sprayPointList.add(lineLatLong.getEnd());
        }

        manageOverlays.drawSprayPoint(sprayPointList);

    }
    public void resetMarker(){
        manageOverlays.reset();
    }
}


