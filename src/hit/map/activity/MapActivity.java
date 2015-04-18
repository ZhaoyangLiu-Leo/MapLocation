package hit.map.activity;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

public class MapActivity extends Activity {

	private MapView mMapView; 					// 地图控件
	private BaiduMap mBaiduMap; 				// 百度地图
	private BitmapDescriptor descriptor; 		// 位图描述文件
	private LocationClient locationClient; 		// 定位客户端
	private final LatLng HIT_LOCATION = new LatLng(45.75201200, 126.63744200);
	private static final int UPDATE_TIME = 3000;
	private TableLayout latlngSettingLayout;
	private LinearLayout placeSettingLayout;
	private EditText longitudeEditText;
	private EditText latitudeEditText;
	private EditText placeEditText;
	private AlertDialog latlngSettingDialog;
	private AlertDialog placeDialog;
	private final String CITY = "哈尔滨";
	private final String TAG = "map";
	private GeoCoder geoCoder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.activity_map);
		
		// 获取地图控件
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		mBaiduMap.setMyLocationEnabled(true);
		
		// 将地理位置定位到HIT附近
		updateMapStatus(HIT_LOCATION);
		
		// 定义marker点击事件
		mBaiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {

			public boolean onMarkerClick(final Marker marker) {
				// TODO Auto-generated method stub
				// 从marker中读出信息，以Toast方式展示出来
				Bundle bundle = marker.getExtraInfo();
				String placeInfo = bundle.getString("placeInfo");
				Toast.makeText(MapActivity.this, placeInfo, Toast.LENGTH_LONG)
						.show();
				return true;
			}
		});

		
		initGeoCoderService();
		initLatlngSettingLayout();
		initPlaceSettingLayout();	
		initLocationService();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		int itemID = item.getItemId();
		switch (itemID) {
		case R.id.action_latlng_locate:
			latlngSettingDialog.show();
			break;
		case R.id.action_description_locate:
			placeDialog.show();
			break;
		case R.id.action_current_locate:
			locationClient.start();
			locationClient.requestLocation();
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * 初始化定位服务
	 */
	private void initLocationService() {
		locationClient = new LocationClient(this);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true); // 打开GPS
		option.setCoorType("bd09ll"); // 设置返回值的坐标类型。
		option.setIsNeedAddress(true);
		
		option.setScanSpan(UPDATE_TIME); // 设置定时定位的时间间隔。单位毫秒
		locationClient.setLocOption(option);
		Log.d(TAG, "in location init");

		// 注册位置监听器
		locationClient.registerLocationListener(new BDLocationListener() {
			@Override
			public void onReceiveLocation(BDLocation location) {
				Log.d(TAG, "in the Receive Location");
				
				if (location == null) {
					return;
				}
				mBaiduMap.clear();
				
				LatLng ll = new LatLng(location.getLatitude(), location
						.getLongitude());
				
				Log.d(TAG, ll.toString());
				
				addMarkerExtra(ll, location.getAddrStr());

				updateMapStatus(ll);
				// 在地图中显示自己的位置
				MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
				locationBuilder.latitude(ll.latitude);
				locationBuilder.longitude(ll.longitude);
				
				MyLocationData locationData = locationBuilder.build();
				mBaiduMap.setMyLocationData(locationData);
				Toast.makeText(MapActivity.this, location.getAddrStr(),
						Toast.LENGTH_LONG).show();
				locationClient.stop();
			}
		});
	}
	
	/**
	 * 更新地理位置
	 * @param ll 经纬度坐标
	 */
	public void updateMapStatus(LatLng ll) {	
		MapStatusUpdate u = MapStatusUpdateFactory.zoomTo(16f);
		mBaiduMap.animateMapStatus(u);
		u = MapStatusUpdateFactory.newLatLng(ll);
		mBaiduMap.animateMapStatus(u);
	}

	/**
	 * 向地图中添加标记点
	 * @param latlng
	 *            经纬度坐标
	 * @return marker
	 */
	private Marker addMarker(LatLng latlng) {
		descriptor = BitmapDescriptorFactory
				.fromResource(R.drawable.icon_marka);
		OverlayOptions markerOptions = new MarkerOptions().position(latlng)
				.icon(descriptor).zIndex(9);
		return (Marker) mBaiduMap.addOverlay(markerOptions);
	}

	/**
	 * 初始化经纬度定位服务
	 */
	private void initLatlngSettingLayout() {
		latlngSettingLayout = (TableLayout) getLayoutInflater().inflate(
				R.layout.latlng_setting, null);
		longitudeEditText = (EditText) latlngSettingLayout
				.findViewById(R.id.longitudeEditText);
		latitudeEditText = (EditText) latlngSettingLayout
				.findViewById(R.id.latitudeEditText);

		Builder builder = new AlertDialog.Builder(this).setTitle("经纬度定位")
				.setView(latlngSettingLayout);

		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				try {
					mBaiduMap.clear();
					double lat = Double.parseDouble(latitudeEditText.getText()
							.toString());
					double lng = Double.parseDouble(longitudeEditText.getText()
							.toString());

					LatLng ll = new LatLng(lat, lng);
					
					getAddressByLatlng(ll);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		latlngSettingDialog = builder.create();
	}

	private void initPlaceSettingLayout() {
		placeSettingLayout = (LinearLayout) getLayoutInflater().inflate(
				R.layout.place_setting, null);

		placeEditText = (EditText) placeSettingLayout
				.findViewById(R.id.placeEditText);

		Builder builder = new AlertDialog.Builder(this).setTitle("位置定位")
				.setView(placeSettingLayout);

		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				mBaiduMap.clear();
				String placeInfo = placeEditText.getText().toString();

				getLatLngByAddress(placeInfo);
			}
		});

		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		placeDialog = builder.create();
	}
	
	/**
	 * 根据经纬度获取位置信息
	 * @param ll
	 * @return
	 */
	private boolean getAddressByLatlng(LatLng ll) {
		if (ll == null) {
			return false;			
		}
		
		// 初始化方向地理编码信息
		ReverseGeoCodeOption rvsGeoOption = new 
				ReverseGeoCodeOption().location(ll);
		return geoCoder.reverseGeoCode(rvsGeoOption);
	}
	
	/**
	 * 根据位置信息获取经纬度信息
	 * @param ll
	 * @return
	 */
	private boolean getLatLngByAddress(String address) {	
		if (address == null || address.equals("")) 
			return false;
		
		GeoCodeOption geoOption = new GeoCodeOption().address(address).city(CITY);	
		return geoCoder.geocode(geoOption);
	}
	
	/**
	 * 初始化地理位置编码服务
	 */
	private void initGeoCoderService() {
		geoCoder = GeoCoder.newInstance();
		geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
			
			@Override
			public void onGetReverseGeoCodeResult(ReverseGeoCodeResult arg0) {
				// TODO Auto-generated method stub
				// 经纬度转换地理位置信息回调函数
				if (arg0 == null || arg0.error != SearchResult.ERRORNO.NO_ERROR) {
					Log.i(TAG, "No result!!"); 
					return; 
				}
				addMarkerExtra(arg0.getLocation(), arg0.getAddress());
			}
			
			@Override
			public void onGetGeoCodeResult(GeoCodeResult arg0) {
				// TODO Auto-generated method stub
				if (arg0 == null || arg0.error != SearchResult.ERRORNO.NO_ERROR) {
					Log.i(TAG, "No result!!"); 
					return; 
				}
				addMarkerExtra(arg0.getLocation(), arg0.getAddress());
			}
		});
	}
	
	private void addMarkerExtra(LatLng ll, String address) {
		updateMapStatus(ll);

		Marker marker = addMarker(ll);
		Bundle bundle = new Bundle();
		bundle.putSerializable("placeInfo", address);
		marker.setExtraInfo(bundle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mBaiduMap.setMyLocationEnabled(true);
		mMapView.onDestroy();	
		if (geoCoder != null) {
			geoCoder.destroy();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mMapView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume();
	}
}
