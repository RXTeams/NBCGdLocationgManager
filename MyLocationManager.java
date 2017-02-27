package com.sunfuedu.igroup.location;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.fence.GeoFence;
import com.amap.api.fence.GeoFenceClient;
import com.amap.api.fence.GeoFenceListener;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.model.LatLng;
import com.sunfuedu.igroup.ui.base.BaseActivity;
import com.sunfuedu.igroup.ui.base.MyApplication;
import com.sunfuedu.igroup.ui.home.HomeActivity;
import com.sunfuedu.igroup.utils.LogUtil;
import com.sunfuedu.igroup.utils.MapUtils;
import com.sunfuedu.igroup.utils.RetrofitUtil;
import com.sunfuedu.igroup.utils.StringHelper;
import com.sunfuedu.igroup.viewmodel.EnclosureManagerStateModel;
import com.sunfuedu.igroup.viewmodel.EnclosureModel;
import com.sunfuedu.igroup.viewmodel.result.HttpResultModel;
import com.sunfuedu.igroup.views.RatioImageView;

import java.util.List;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static android.R.attr.filter;
import static com.amap.api.fence.GeoFenceClient.GEOFENCE_IN;
import static com.amap.api.fence.GeoFenceClient.GEOFENCE_OUT;

/**
 * Created by 泊恒 on 2016/12/11 0011.
 */

public class MyLocationManager {
    private final String TAG = "MyLocaitonManager";
    public static final String GEOFENCE_BROADCAST_ACTION = "com.location.apis.geofencedemo.broadcast";
    public static MyLocationManager instance;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    public  PendingIntent pendingIntent;
    private boolean isFirstOpenSystemGpsSetting = true;




    public static MyLocationManager getInstance(){

        if (instance == null){
            instance = new MyLocationManager();
        }
        return instance;
    }

    public PendingIntent getPendingIntent(){
        return pendingIntent;
    }

    /**
     * 初始化定位
     */
    public void initLocation() {
        if (locationClient == null) {
            locationClient = new AMapLocationClient(MyApplication.getInstance());
        }
        if (locationOption == null) {
            locationOption = new AMapLocationClientOption();
            // 设置定位模式为高精度模式
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 设置定位监听
//            locationClient.setLocationListener(locationListener);
            //设置持续定位监听
            locationOption.setOnceLocation(false);
            //设置发送定位请求时间间隔为5秒
            locationOption.setInterval(5000);
            //设置是否缓存
            locationOption.setLocationCacheEnable(true);
            //设置地理逆偏码
            locationOption.setNeedAddress(true);

            locationOption.setGpsFirst(true);
        }

        // 设置定位参数
        locationClient.setLocationOption(locationOption);
    }


    /**
     * 开启定位
     */
    public void startLocation(AMapLocationListener listener) {
        if (null == locationClient) {
            initLocation();
        }
        locationClient.startLocation();
        AMapLocationClient locationClient = getLocationClient();
        locationClient.setLocationListener(listener);
    }



    /**
     * 停止定位
     */
    public void stopLocation() {
        if (null != locationClient) {
            locationClient.stopLocation();
        }
        locationClient = null;
        locationOption = null;
    }

    /**
     * 判断定位是否是打开状态
     *
     * @return
     */
    public boolean isStartedLocation() {
        boolean flag = false;
        if (null != locationClient) {
            flag = locationClient.isStarted();
        }
        return flag;
    }

    public AMapLocationClient getLocationClient() {
        if (null == locationClient) {
            initLocation();
        }
        return locationClient;
    }

    public AMapLocationClientOption getLocationOption() {
        if (null == locationClient) {
            initLocation();
        }
        return locationOption;
    }

    /**
     *
     * @param context
     * @param lat
     * @param lng
     * @param radius
     * @param id
     * @param time
     */
    public void addGeoFence(final Context context, double lat,double lng, float radius, String id,long time){
        if (null == locationClient){
            initLocation();
        }
        IntentFilter fliter = new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION);
        fliter.addAction(GEOFENCE_BROADCAST_ACTION);
        context.registerReceiver(mGeoFenceReceiver, fliter);
        Intent intent = new Intent(GEOFENCE_BROADCAST_ACTION);
        pendingIntent = PendingIntent.getBroadcast(context, 0,
                intent, 0);
        locationClient.addGeoFenceAlert(id,lat,lng,radius,time,pendingIntent);


    }


    /**
     * 移除id为fenceId的围栏
     * @param fenceId
     */
    public void removeGeoFence(String fenceId){
        if (null != pendingIntent){
            try {
                locationClient.removeGeoFenceAlert(pendingIntent,fenceId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private BroadcastReceiver mGeoFenceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
                //解析广播内容
                //获取Bundle
                Bundle bundle = intent.getExtras();
                //获取围栏行为：
                int status = bundle.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS);
                //获取自定义的围栏标识：
//                String customId = bundle.getString(GeoFence.BUNDLE_KEY_CUSTOMID);
                //获取围栏ID:
                String fenceId = bundle.getString(GeoFence.BUNDLE_KEY_FENCEID);
                //获取当前有触发的围栏对象：
//                GeoFence fence = bundle.getParcelable(GeoFence.BUNDLE_KEY_FENCE);
                Realm realm = MyApplication.getInstance().getRealm();
                if (status == 1) {
                    // 进入围栏区域
                    EnclosureModel mEnclosureModel = realm.where(EnclosureModel.class).equalTo("eventId", fenceId).findFirst();
                    if (mEnclosureModel != null && StringHelper.isText(mEnclosureModel.getIdentification())) {
                        EnclosureManagerStateModel nowEnclosureManagerStateModel = new EnclosureManagerStateModel();
                        nowEnclosureManagerStateModel.setIdentification(mEnclosureModel.getIdentification());
                        creatOrUpdateManagerStateModel(nowEnclosureManagerStateModel, true);
                        Log.d(TAG, "进入围栏" + nowEnclosureManagerStateModel.getState());
                    }

                } else if (status == 2) {
                    // 离开围栏区域
                    EnclosureModel mEnclosureModel = realm.where(EnclosureModel.class).equalTo("eventId", fenceId).findFirst();
                    if (mEnclosureModel != null && mEnclosureModel.getState() == 1) {
                        EnclosureManagerStateModel nowEnclosureManagerStateModel = realm.where(EnclosureManagerStateModel.class).equalTo("identification", mEnclosureModel.getIdentification()).findFirst();
                        if (nowEnclosureManagerStateModel != null) {
                            if (nowEnclosureManagerStateModel.getState()) {
                                addEnclosureManager(mEnclosureModel);
                                creatOrUpdateManagerStateModel(nowEnclosureManagerStateModel, false);
                            }
                        } else {
                            addEnclosureManager(mEnclosureModel);
                            EnclosureManagerStateModel enclosureManagerStateModel = new EnclosureManagerStateModel();
                            enclosureManagerStateModel.setIdentification(mEnclosureModel.getIdentification());
                            creatOrUpdateManagerStateModel(enclosureManagerStateModel, false);

                        }
                    }



                }
            }
        }
    };


    private void addEnclosureManager(final EnclosureModel enclosureModel) {
        final Realm realm = MyApplication.getInstance().getRealm();
        String userLocaitonAddres = MyApplication.getInstance().getUserLocaitonAddres();
        RetrofitUtil.createApi(MyApplication.getInstance()).addEnclosureManager(MyApplication.getInstance().getUserModel().getIdentification(), enclosureModel.getRadius(), enclosureModel.getEventId(),userLocaitonAddres)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<HttpResultModel>() {
                    @Override
                    public void call(HttpResultModel result) {
                        EnclosureManagerStateModel enclosureManagerStateModel = realm.where(EnclosureManagerStateModel.class).equalTo("identification", enclosureModel.getIdentification()).findFirst();
                        if (result != null) {
                            if (result.getCode().equals("200")) {
                                Log.d("报警", enclosureModel.getEventId() + "成功");
                                if (enclosureManagerStateModel == null) {
                                    enclosureManagerStateModel = new EnclosureManagerStateModel();
                                    enclosureManagerStateModel.setIdentification(enclosureModel.getIdentification());
                                }
                                creatOrUpdateManagerStateModel(enclosureManagerStateModel, false);
                                Log.d(TAG, "离开围栏" + enclosureManagerStateModel.getState());
                            } else {
                                Log.d("报警", result.getMessage() + "失败");
                            }
                        } else {
                            Log.d("报警", result.getMessage() + "失败");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                        Log.d("报警", "失败");
                    }
                });
    }

    private void creatOrUpdateManagerStateModel(EnclosureManagerStateModel enclosureManagerStateModel, boolean blen) {
        Realm realm = MyApplication.getInstance().getRealm();
        realm.beginTransaction();
        enclosureManagerStateModel.setState(blen);
        realm.copyToRealmOrUpdate(enclosureManagerStateModel);
        realm.commitTransaction();
    }

    LocationListener locationListener;
 public void startAndroidGps(Activity activity){
     boolean openGps = isOpenGps(MyApplication.getInstance());
     if (openGps ) {
            if (null == locationListener){
                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        CoordinateConverter converter = new CoordinateConverter(MyApplication.getInstance());
                        converter.from(CoordinateConverter.CoordType.GPS);
                        converter.coord(latLng);
                        LatLng desLatLng = converter.convert();
//                     uploadLocation(desLatLng.latitude, desLatLng.longitude);
//                     oldLatLng = desLatLng;
//                     computedRange(new LatLng(desLatLng.latitude, desLatLng.longitude));
                        LogUtil.d("MyLocationGPS芯片", location.getLatitude() + "位置" + location.getLongitude());
                        LogUtil.d("MyLocation高德", desLatLng.latitude + "位置" + desLatLng.longitude);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                };

                startAndroidGps(MyApplication.getInstance(), locationListener);
            }

     } else {
         boolean isopenGps = isOpenGps(MyApplication.getInstance());
         if (!isopenGps && isFirstOpenSystemGpsSetting) {
             isFirstOpenSystemGpsSetting = false;
             Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
             activity.startActivityForResult(intent,0);
         }
     }
 }

    public static boolean isOpenGps(Context context) {
        boolean isOpenGps = false;
        LocationManager alm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (alm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            isOpenGps = true;
        } else {
            isOpenGps = false;
        }
        return isOpenGps;
    }

    public static void startAndroidGps(Context context,LocationListener locationListener) {
        // 获取位置管理服务
        LocationManager locationManager;
        String serviceName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) context.getSystemService(serviceName);
        // 查找到服务信息
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗
        String provider = locationManager.getBestProvider(criteria, true); // 获取GPS信息
//        updateToNewLocation(location);
        // 设置监听器，自动更新的最小时间为间隔N秒(1秒为1*1000，这样写主要为了方便)或最小位移变化超过N米
        locationManager.requestLocationUpdates(provider,  1000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,0,locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000,0,locationListener);
    }

    public void setLocationChangeListener(AMapLocationListener listener){
        AMapLocationClient locationClient = getLocationClient();
        if (isStartedLocation()){
            locationClient.setLocationListener(listener);
        }
    }
    /**
     * 初始化一个只定位一次的LocationClient
     * @param context
     * @return AMapLocationClient
     */
    public  AMapLocationClient getOneLocationClient(Context context){
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

    public  boolean missDistance(LatLng latLngOld,LatLng latLngNew){
        boolean flag = false;
        float distance = AMapUtils.calculateLineDistance(latLngOld,latLngNew);
        if (distance > 50){
            flag = false;
        }else {
            flag = true;
        }
        return flag;
    }

}
