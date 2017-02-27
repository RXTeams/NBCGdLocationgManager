package com.sunfuedu.igroup.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.GroundOverlay;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.sunfuedu.igroup.R;
import com.sunfuedu.igroup.ui.base.MyApplication;
import com.sunfuedu.igroup.viewmodel.MyMapData;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.realm.RealmObject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by 泊恒 on 2016/6/16 0016.
 */
public class MapUtils {
    /**
     * 通过经纬度获取地址
     * @param context Context
     * @param lat   经度
     * @param lng   纬度
     * @return
     */
    public static String getAddressFromLatLng(Context context, Double lat, Double lng , boolean isNear) throws Exception {
        String addressStr;
        GeocodeSearch geocodeSearch = new GeocodeSearch(context);
        LatLonPoint latLonPoint = new LatLonPoint(lat,lng);
        RegeocodeQuery regeocodeQuery = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                if (i == 0){
                    if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null && regeocodeResult.getRegeocodeAddress().getFormatAddress() !=null) {
                    }
                }

            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        });
        RegeocodeAddress fromLocation = geocodeSearch.getFromLocation(regeocodeQuery);
        if(isNear){
            addressStr = fromLocation.getFormatAddress()+"附近";
        }else{
            addressStr = fromLocation.getFormatAddress();
        }

        return addressStr;
    }

    /**
     * 初始化一个只定位一次的LocationClient
     * @param context
     * @return AMapLocationClient
     */
    public static AMapLocationClient getOneLocationClient(Context context){
        AMapLocationClient locationClient = null;
        AMapLocationClientOption locationOption = null;
        if (locationClient == null){
            locationClient = new AMapLocationClient(context);
        }if (locationOption == null){
            locationOption = new AMapLocationClientOption();
        }
        // 设置定位模式为高精度模式
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置只定位一次定位监听
        locationOption.setOnceLocation(true);
        //设置是否缓存
        locationOption.setLocationCacheEnable(true);
        //设置地理逆偏码
        locationOption.setNeedAddress(true);
        //设置优先GPS获取位置信息
        locationOption.setGpsFirst(false);
        // 设置定位参数
        locationClient.setLocationOption(locationOption);
        return locationClient;
    }

    public static Circle addMyGeoFenceAlertCircleToTaskAddress(AMap mAMap, LatLng latLng, double radius) {
        int strokeWidth = getStrokeWidth(1);
        Circle circle = mAMap.addCircle(new CircleOptions().center(latLng).radius(radius).strokeColor(Color.argb(100, 1, 255, 1))
                .fillColor(Color.argb(50, 1, 255, 1)).strokeWidth(strokeWidth));
        return circle;
    }

    public static Circle addMyGeoFenceAlertCircle(AMap mAMap, LatLng latLng, double radius) {
        int strokeWidth = getStrokeWidth(2);
        Circle circle = mAMap.addCircle(new CircleOptions().center(latLng).radius(radius).strokeColor(Color.argb(100, 255, 1, 1))
                .fillColor(Color.argb(0, 1, 255, 1)).strokeWidth(strokeWidth));
        return circle;
    }
    public static Circle addMyGeoFenceAlertCircle(AMap mAMap, double lat, double lng, double radius) {
        int strokeWidth = getStrokeWidth(2);
        LatLng latLng = new LatLng(lat, lng);
        Circle circle = mAMap.addCircle(new CircleOptions().center(latLng).radius(radius).strokeColor(Color.argb(100, 255, 1, 1))
                .fillColor(Color.argb(0, 1, 255, 1)).strokeWidth(strokeWidth));
        return circle;
    }

    public static Circle addCircleToTask(AMap mAMap, LatLng latLng, double radius , int colorStroke , int colorFill) {
        int strokeWidth = getStrokeWidth(1);
        Circle circle = mAMap.addCircle(new CircleOptions().center(latLng).radius(radius).strokeColor(colorStroke)
                .fillColor(colorFill).strokeWidth(strokeWidth));
        return circle;
    }

    /**
     * 将DIP转换为对应手机屏幕的像素值
     * @param dip 
     * @return
     */


    public static int getStrokeWidth(int dip) {
        final float scale = MyApplication.getInstance().getResources().getDisplayMetrics().density;
        return (int)(dip*scale+0.5f);
    }


    public static void moveMapCamera(AMap mAMap, Double lat, Double lng, boolean isFirst) {
        if (null == lat || 0 == lat || null == lng || 0 == lng){
            lat = Constant.DEFUTL_LAT;
            lng = Constant.DEFUTL_LNG;
        }
        LatLng latLng = new LatLng(lat, lng);
        float zoom = 17;
        if (!isFirst){
            zoom = mAMap.getCameraPosition().zoom;
        }
        CameraPosition cameraPosition = new CameraPosition(latLng, zoom, 0, 0);
        mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public static void moveMapCameraToTask(AMap mAMap, Double lat, Double lng, boolean isFirst) {
        LatLng latLng = new LatLng(lat, lng);
        float zoom = 17;
        if (!isFirst){
            zoom = mAMap.getCameraPosition().zoom;
        }
        CameraPosition cameraPosition = new CameraPosition(latLng, zoom, 0, 0);
        mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }



    public static double getDistance(LatLng start, LatLng end)
    {
        double lon1 = (Math.PI / 180) * start.longitude;
        double lon2 = (Math.PI / 180) * end.longitude;
        double lat1 = (Math.PI / 180) * start.latitude;
        double lat2 = (Math.PI / 180) * end.latitude;

        // 地球半径
        double R = 6371;

        // 两点间距离 km，如果想要米的话，结果*1000就可以了
        double d = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)) * R;
        return d * 1000;
    }


    /**
     * 在对应的地图上画卡通地图
     * @param aMap
     */
    public static void drawMyMap(final AMap aMap) {
        HashMap<String, LatLngBounds> stringLatLngBoundsHashMap = MyMapData.getInstance().getStringLatLngBoundsHashMap();
                if (null != stringLatLngBoundsHashMap && stringLatLngBoundsHashMap.size() > 0 ){
                    final Iterator iter = stringLatLngBoundsHashMap.entrySet().iterator();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (iter.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                String assetPath = (String) entry.getKey();
                                Bitmap bitmap = GlideUtil.getInstance().loadImageToBitMap(assetPath);
                                LatLngBounds bounds = (LatLngBounds) entry.getValue();
                                if (null != bitmap){
                                    GroundOverlay groundoverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
                                            .anchor(0.5f, 0.5f)  //设置图片的对齐方式，[0,0]是左上角，[1,1]是右下角 。
                                            .transparency(0.1f)//设置图片层的透明度。
                                            .image(BitmapDescriptorFactory
                                                    .fromBitmap(bitmap))
                                            .positionFromBounds(bounds));
                                    groundoverlay.setZIndex(-0.1F);
                                }
                            }
                        }
                    }).start();
                }
    }

    public static void mapClear(AMap aMap){
        List<Marker> mapScreenMarkers = aMap.getMapScreenMarkers();
        for (Marker mapScreenMarker :mapScreenMarkers) {
            mapScreenMarker.remove();
        }
    }
    public static void mapClearOtherMarker(AMap aMap, Marker marker){
        List<Marker> mapScreenMarkers = aMap.getMapScreenMarkers();
        for (Marker mapScreenMarker :mapScreenMarkers) {
            if (StringHelper.isText(mapScreenMarker.getId()) && !mapScreenMarker.getId() .equals(marker.getId()) ){
                mapScreenMarker.remove();
            }
        }
    }

    public void drawMyMap(final Context context, final AMap aMap, String url, final LatLngBounds bounds) {
        Observable.just(url)
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String url) {
                        try {
                            FutureTarget<File> futureTarget = Glide.with(context)
                                    .load(url)
                                    .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                            File file = futureTarget.get();
                            return BitmapFactory.decodeFile(file.getAbsolutePath());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        GroundOverlay groundoverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
                                .anchor(0.5f, 0.5f)  //设置图片的对齐方式，[0,0]是左上角，[1,1]是右下角 。
                                .transparency(0.1f)//设置图片层的透明度。
                                .image(BitmapDescriptorFactory
                                        .fromAsset("MyMap/gugong.png"))
                                .positionFromBounds(bounds));
                    }
                });
    }


    /**
     * 绘制小组成员坐标点（有分组）
     * @param context Context
     * @param mAMap AMap
     * @param lat  latitude 经度
     * @param lng   longitude   维度
     *
     */
    public static void drawMarKer(final Context context, AMap mAMap, final double lat, final double lng, final String userId, final boolean isOnline) {
        if (isOnline){
            Observable.just(mAMap)
                    .filter(new Func1<AMap, Boolean>() {
                        @Override
                        public Boolean call(AMap aMap) {
                            return isOnline;
                        }
                    })
                    .map(new Func1<AMap, Marker>() {
                        @Override
                        public Marker call(AMap aMap) {
                            return aMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                                            .decodeResource(context.getResources(), R.drawable.hd_dingwei_putong_icon)))
                                    .draggable(true));
                        }
                    }).subscribe(new Action1<Marker>() {
                @Override
                public void call(Marker marker) {
                    marker.setObject(userId);
                }
            });
        }else{
            Observable.just(mAMap)
                    .filter(new Func1<AMap, Boolean>() {
                        @Override
                        public Boolean call(AMap aMap) {
                            return !isOnline;
                        }
                    })
                    .map(new Func1<AMap, Marker>() {
                        @Override
                        public Marker call(AMap aMap) {
                            return aMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                                            .decodeResource(context.getResources(), R.drawable.hd_dingwei_wuxinhao_icon)))
                                    .draggable(true));
                        }
                    }).subscribe(new Action1<Marker>() {
                @Override
                public void call(Marker marker) {
                    marker.setObject(userId);
                }
            });
        }
    }
}
