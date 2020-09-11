package com.example.dronegcs;

import android.graphics.Color;
import android.support.annotation.NonNull;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.ArrayList;
import java.util.Arrays;

public class ManageOverlay {
    private NaverMap mymap;
    private ArrayList<LatLng> ppointlist = new ArrayList<LatLng>();
    private ArrayList<Marker> markerlist = new ArrayList<>();
    private ArrayList<Marker> spraymakers = new ArrayList<>();
    private ArrayList<LatLng> spraypointlist = new ArrayList<>();
    //스테이션 Marker 및 경로 Ovelay
    Marker stationMarkerM = new Marker();
    Marker stationMarker1 = new Marker();
    Marker stationMarker2 = new Marker();
    Marker stationMarker3 = new Marker();


    private PathOverlay pathline = new PathOverlay();
    private ArrayList<InfoWindow> infowindowlist = new ArrayList<>();
    private PolygonOverlay polygon = new PolygonOverlay();
    private ArrayList<Marker> boundlist = new ArrayList<>();
    protected MainActivity mainactivity;

    public ManageOverlay(NaverMap mymap,MainActivity mainactivity)
    {
        this.mainactivity = mainactivity;
        this.mymap = mymap;
    }

    public void setPPosition(LatLong latLong){
        Marker marker = new Marker();
        LatLng coord = new LatLng(latLong.getLatitude(),latLong.getLongitude());
        ppointlist.add(coord);
        markerlist.add(marker);
        marker.setWidth(40);
        marker.setHeight(70);
        marker.setPosition(coord);
        marker.setMap(mymap);
    }


    public void drawPolygon(){

        polygon.setCoords(ppointlist);
        polygon.setColor(0);

        polygon.setOutlineWidth(2);
        polygon.setMap(mymap);
    }

    public void drawSprayPoint(ArrayList<LatLong> sprayplist){


        for(Marker marker : markerlist){
            marker.setMap(null);
        }
        for(Marker marker : spraymakers){
            marker.setMap(null);
        }
        spraypointlist.clear();
        spraymakers.clear();


        for(LatLong latlong:sprayplist){
            LatLng latlng = new LatLng(latlong.getLatitude(),latlong.getLongitude());
            spraypointlist.add(latlng);
            Marker marker = new Marker();
            marker.setWidth(40);
            marker.setHeight(70);
            marker.setPosition(latlng);
            marker.setMap(mymap);

            spraymakers.add(marker);
        }
        for(int i=0;i<spraypointlist.size();i++){
            spraymakers.get(i).setTag(String.format(""+(i+1)));
            InfoWindow infoWindow = new InfoWindow();
            infoWindow.setAdapter((new InfoWindow.DefaultTextAdapter(mainactivity) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    return (CharSequence)infoWindow.getMarker().getTag();
                }
            }));
            infoWindow.open(spraymakers.get(i));
            infowindowlist.add(infoWindow);
        }

        pathline.setCoords(spraypointlist);
        pathline.setColor(Color.GREEN);
        pathline.setMap(mymap);

    }

    public void reset(){
        for(Marker marker : markerlist){
            marker.setMap(null);
        }
        for(Marker marker : spraymakers){
            marker.setMap(null);
        }
        pathline.setMap(null);
        polygon.setMap(null);
        ppointlist.clear();
        spraypointlist.clear();
    }

    public void stationMarker(){
        stationMarkerM.setPosition(new LatLng(35.942293, 126.683031));  //스테이션 Marker
        stationMarker1.setPosition(new LatLng(35.942305, 126.682317));
        stationMarker2.setPosition(new LatLng(35.941884, 126.682273));
        stationMarker3.setPosition(new LatLng(35.941861, 126.683013));

        stationMarkerM.setIcon(OverlayImage.fromResource(R.drawable.station));            //오버레이는 하나만 가능, 여러개 일시 마지막 코드만 실행
        stationMarker1.setIcon(OverlayImage.fromResource(R.drawable.stop));
        stationMarker2.setIcon(OverlayImage.fromResource(R.drawable.stop));
        stationMarker3.setIcon(OverlayImage.fromResource(R.drawable.stop));

        stationMarkerM.setMap(mymap);
        stationMarker1.setMap(mymap);
        stationMarker2.setMap(mymap);
        stationMarker3.setMap(mymap);

    }

    public void resetMarker(){
        stationMarkerM.setMap(null);
        stationMarker1.setMap(null);
        stationMarker2.setMap(null);
        stationMarker3.setMap(null);
    }
}
