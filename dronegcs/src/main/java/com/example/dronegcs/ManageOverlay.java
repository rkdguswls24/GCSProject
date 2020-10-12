package com.example.dronegcs;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;

public class ManageOverlay {
    private NaverMap mymap;

    //스테이션 Marker 및 경로 Ovelay
    Marker stationMarkerM = new Marker();
    Marker stationMarker1 = new Marker();
    Marker stationMarker2 = new Marker();
    Marker stationMarker3 = new Marker();

    protected MainActivity mainactivity;

    public ManageOverlay(NaverMap mymap,MainActivity mainactivity)
    {
        this.mainactivity = mainactivity;
        this.mymap = mymap;
    }

    public void reset(){
        stationMarkerM.setMap(null);
        stationMarker1.setMap(null);
        stationMarker2.setMap(null);
        stationMarker3.setMap(null);

    }
    public void stationMarker(){
        stationMarkerM.setPosition(new LatLng(35.942268, 126.678938));  //스테이션 Marker
        stationMarker1.setPosition(new LatLng(35.942160, 126.678924));
        stationMarker2.setPosition(new LatLng(35.942147, 126.679024));
        stationMarker3.setPosition(new LatLng(35.942242, 126.679056));

        stationMarkerM.setIcon(OverlayImage.fromResource(R.drawable.station));            //오버레이는 하나만 가능, 여러개 일시 마지막 코드만 실행
        stationMarker1.setIcon(OverlayImage.fromResource(R.drawable.stop));
        stationMarker2.setIcon(OverlayImage.fromResource(R.drawable.stop));
        stationMarker3.setIcon(OverlayImage.fromResource(R.drawable.stop));

        stationMarkerM.setMap(mymap);
        stationMarker1.setMap(mymap);
        stationMarker2.setMap(mymap);
        stationMarker3.setMap(mymap);
    }
}