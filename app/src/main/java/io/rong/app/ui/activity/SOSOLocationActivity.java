package io.rong.app.ui.activity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sea_monster.common.ParcelUtils;
import com.tencent.lbssearch.TencentSearch;
import com.tencent.lbssearch.httpresponse.BaseObject;
import com.tencent.lbssearch.httpresponse.HttpResponseListener;
import com.tencent.lbssearch.object.Location;
import com.tencent.lbssearch.object.param.Geo2AddressParam;
import com.tencent.lbssearch.object.result.Geo2AddressResultObject;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.GeoPoint;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapActivity;
import com.tencent.tencentmap.mapsdk.map.MapView;

import org.apache.http.Header;

import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.message.LocationMessage;

/**
 * Created by DragonJ on 14/11/21.
 */

@SuppressLint("ClickableViewAccessibility")
public class SOSOLocationActivity extends MapActivity implements
        TencentLocationListener, OnClickListener, Handler.Callback, View.OnTouchListener {

    MapView mMapView;
    Button mButton = null;
    LocationMessage mMsg;
    Handler mHandler;
    Handler mWorkHandler;
    TextView mTitle;
    /**
     * 当前地图地址的poi
     */
    private HandlerThread mHandlerThread;

    private final static int RENDER_POI = 1;
    private final static int SHWO_TIPS = 2;

    @Override
    /**
     *显示地图，启用内置缩放控件，并用MapController控制地图的中心点及Zoom级别
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_ac_soso_map);

        mHandlerThread = new HandlerThread("LocationThread");
        mHandlerThread.start();
        mWorkHandler = new Handler(mHandlerThread.getLooper());
        mHandler = new Handler(this);

        mMapView = (MapView) findViewById(android.R.id.widget_frame);
        mTitle = (TextView) findViewById(android.R.id.title);
        mButton = (Button) this.findViewById(android.R.id.button1);

        if (getIntent().hasExtra("location")) {
            mMsg = getIntent().getParcelableExtra("location");
        }

        if (mMsg != null)
            mButton.setVisibility(View.GONE);
        mButton.setOnClickListener(this);

        mMapView.getUiSettings().setAnimationEnabled(true);// 地图缩放、平移动画开关

        if (mMsg == null) {
            GeoPoint point = new GeoPoint((int) (39.90923 * 1E6), (int) (116.397428 * 1E6)); // 用给定的经纬度构造一个GeoPoint，单位是微度

            mMapView.getController().setCenter(point);
            mMapView.getMap().setZoom(16);
            mMapView.setOnTouchListener(this);
            TencentLocationRequest request = TencentLocationRequest.create();
            TencentLocationManager.getInstance(this).requestLocationUpdates(request, this);

        } else {
            LatLng latLng = new LatLng((mMsg.getLat()), (mMsg.getLng()));
            mMapView.getMap().setZoom(16);

            SOSOPoiItem sosoPoiItem = new SOSOPoiItem();
            sosoPoiItem.setName(mMsg.getPoi());
            sosoPoiItem.setLat((mMsg.getLat() * 1E6));
            sosoPoiItem.setLng((mMsg.getLng() * 1E6));

            mHandler.obtainMessage(RENDER_POI, sosoPoiItem).sendToTarget();
            findViewById(android.R.id.icon).setVisibility(View.GONE);

            mMapView.getMap().animateTo(latLng);
        }

    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == RENDER_POI) {

            SOSOPoiItem poiItem = (SOSOPoiItem) msg.obj;

            LatLng latLng = new LatLng(poiItem.getLat() / 1E6, poiItem.getLng() / 1E6);

            mMapView.getMap().addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.red_location))
                    .position(latLng)
                    .draggable(true));

        } else if (msg.what == SHWO_TIPS) {

            SOSOPoiItem poiItem = (SOSOPoiItem) msg.obj;

            mTitle.setText(poiItem.name);
            mTitle.setVisibility(View.VISIBLE);

            Uri uri = Uri
                    .parse("http://apis.map.qq.com/ws/staticmap/v2").buildUpon()
                    .appendQueryParameter("size", "240*240")
                    .appendQueryParameter("key", "7JYBZ-4Y3W4-JMUU7-DJHQU-NOYH7-SRBBU")
                    .appendQueryParameter("zoom", "16")
                    .appendQueryParameter("center", poiItem.getLat() / 1E6
                            + ","
                            + poiItem.getLng() / 1E6).build();

            mMsg = LocationMessage.obtain(poiItem.getLat() / 1E6,
                    poiItem.getLng() / 1E6, poiItem.name, uri);

        }
        return false;
    }


    @Override
    public void onLocationChanged(final TencentLocation tencentLocation,
                                  int code, String s) {
        if (TencentLocation.ERROR_OK == code) {
            Toast.makeText(this, "定位成功", Toast.LENGTH_SHORT).show();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GeoPoint point = new GeoPoint((int) (tencentLocation.getLatitude() * 1E6),
                            (int) (tencentLocation.getLongitude() * 1E6)); // 用给定的经纬度构造一个GeoPoint，单位是微度
                    mMapView.getController().setCenter(point);

                    SOSOPoiItem sosoPoiItem = new SOSOPoiItem();
                    sosoPoiItem.setName(tencentLocation.getAddress());
                    sosoPoiItem.setLat((int) (tencentLocation.getLatitude() * 1E6));
                    sosoPoiItem.setLng((int) (tencentLocation.getLongitude() * 1E6));
                    android.util.Log.e("tag", "-----onLocationChanged-----(tencentLocation.getLatitude() * 1E6)---4.sdf------" + (tencentLocation.getLatitude()));
                    android.util.Log.e("tag", "-----onLocationChanged-----(tencentLocation.getLatitude() * 1E6)---4.0032057E7------" + (int) (tencentLocation.getLatitude() * 1E6));


                    if (getIntent().hasExtra("location"))
                        mHandler.obtainMessage(RENDER_POI, sosoPoiItem).sendToTarget();
                    else
                        mHandler.obtainMessage(SHWO_TIPS, sosoPoiItem).sendToTarget();

                }
            });

            TencentLocationManager.getInstance(this).removeUpdates(this);


        } else {
            Toast.makeText(this, "定位失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusUpdate(String s, int i, String s2) {
    }

    @Override
    protected void onDestroy() {

        if (DemoContext.getInstance().getLastLocationCallback() != null)
            DemoContext.getInstance().getLastLocationCallback().onFailure("失败");

        DemoContext.getInstance().setLastLocationCallback(null);
        TencentLocationManager.getInstance(this).removeUpdates(this);

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        if (mMsg != null) {
            DemoContext.getInstance().getLastLocationCallback().onSuccess(mMsg);
            DemoContext.getInstance().setLastLocationCallback(null);
            finish();
        } else {
            DemoContext.getInstance().getLastLocationCallback()
                    .onFailure("定位失败");
        }

    }


    POISearchRunnable mLastSearchRunnable;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mLastSearchRunnable != null)
                    mWorkHandler.removeCallbacks(mLastSearchRunnable);

                mTitle.setVisibility(View.INVISIBLE);
                mHandler.removeMessages(RENDER_POI);

                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                mLastSearchRunnable = new POISearchRunnable();
                mWorkHandler.post(new POISearchRunnable());
                break;
            default:
                break;
        }

        return false;
    }


    private class POISearchRunnable implements Runnable {

        public void run() {

            try {
                float lat = (float) mMapView.getMap().getMapCenter().getLatitude();
                float lng = (float) mMapView.getMap().getMapCenter().getLongitude();

                Location location = new Location(lat, lng);
                if (location == null)
                    return;

                TencentSearch tencentSearch = new TencentSearch(SOSOLocationActivity.this);
                //还可以传入其他坐标系的坐标，不过需要用coord_type()指明所用类型
                //这里设置返回周边poi列表，可以在一定程度上满足用户获取指定坐标周边poi的需求
                Geo2AddressParam geo2AddressParam = new Geo2AddressParam().
                        location(location).get_poi(true);

                tencentSearch.geo2address(geo2AddressParam, new HttpResponseListener() {

                    @Override
                    public void onSuccess(int arg0, Header[] arg1, BaseObject arg2) {
                        if (arg2 == null)
                            return;

                        Geo2AddressResultObject obj = (Geo2AddressResultObject) arg2;

                        SOSOPoiItem sosoPoiItem = new SOSOPoiItem();
                        sosoPoiItem.setName(obj.result.address);

                        sosoPoiItem.setLat((int) (mMapView.getMap().getMapCenter().getLatitude() * 1E6));
                        sosoPoiItem.setLng((int) (mMapView.getMap().getMapCenter().getLongitude() * 1E6));

                        if (getIntent().hasExtra("location"))
                            mHandler.obtainMessage(RENDER_POI, sosoPoiItem).sendToTarget();
                        else
                            mHandler.obtainMessage(SHWO_TIPS, sosoPoiItem).sendToTarget();
                    }

                    @Override
                    public void onFailure(int arg0, Header[] arg1, String arg2, Throwable arg3) {

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private class SOSOPoiItem implements Parcelable {

        private String name;

        private double lat;

        private double lng;

        public SOSOPoiItem() {

        }

        public final Creator<SOSOPoiItem> CREATOR = new Creator<SOSOPoiItem>() {

            @Override
            public SOSOPoiItem createFromParcel(Parcel source) {

                return new SOSOPoiItem(source);
            }

            @Override
            public SOSOPoiItem[] newArray(int size) {

                return new SOSOPoiItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        private SOSOPoiItem(Parcel in) {
            name = ParcelUtils.readFromParcel(in);
            lat = Double.parseDouble(ParcelUtils.readFromParcel(in));
            lng = Double.parseDouble(ParcelUtils.readFromParcel(in));
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            ParcelUtils.writeToParcel(parcel, name);
            ParcelUtils.writeToParcel(parcel, lat);
            ParcelUtils.writeToParcel(parcel, lng);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }
}
