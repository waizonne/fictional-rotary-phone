package info.androidhive.loginandregistration.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import info.androidhive.loginandregistration.R;
import info.androidhive.loginandregistration.app.AppConfig;
import info.androidhive.loginandregistration.app.AppController;
import info.androidhive.loginandregistration.helper.SessionManager;

public class MainActivity extends Activity {

	private Button btnLogout;
	private static final String TAG = RegisterActivity.class.getSimpleName();

	public static final String DEVICE_ID = "id";
	public static final String DEVICE_NAME = "name";
	public static final String DEVICE_STATUS = "status";
	public static final String DEVICE_VALUE = "value";
	public static final String DEVICE_TYPE = "type";
	public static final String DEVICE_DEVICETOPIC = "devicetopic";

	MotionEvent e;


	ArrayList<HashMap<String, String>> Item_List;
	HashMap<String, String> deviceSwitch,deviceSlide;
	int p,percentageX,pos;

	String b,c,sb,sc;

	boolean igons = false;

	private SessionManager session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final ListView listView = (ListView) findViewById(R.id.listView);
		Item_List = new ArrayList<HashMap<String, String>>();
		btnLogout = (Button) findViewById(R.id.btnLogout);

		// session manager
		session = new SessionManager(getApplicationContext());

		if (!session.isLoggedIn()) {
			logoutUser();
		}

		new Thread(new Runnable(){
			@Override
			public void run() {
				String tag_string_req = "req_device";

				StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_LOGIN, new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						try {
							JSONObject jObj = new JSONObject(response);
							int success = jObj.getInt("success");
							if (success == 1){
								JSONArray ja = jObj.getJSONArray("device");
								for (int i = 0; i < ja.length(); i++) {
									JSONObject device = ja.getJSONObject(i);
									HashMap<String, String> item = new HashMap<String, String>();
									item.put(DEVICE_ID, device.getString(DEVICE_ID));
									item.put(DEVICE_NAME, device.getString(DEVICE_NAME));
									item.put(DEVICE_STATUS, device.getString(DEVICE_STATUS));
									item.put(DEVICE_VALUE, device.getString(DEVICE_VALUE));
									item.put(DEVICE_TYPE, device.getString(DEVICE_TYPE));
									item.put(DEVICE_DEVICETOPIC, device.getString(DEVICE_DEVICETOPIC));
									Item_List.add(item);
								}
								String[] from = new String[] {DEVICE_NAME};
								int[] to = new int[] { R.id.label};

								SimpleAdapter adapter = new SimpleAdapter(MainActivity.this,Item_List, R.layout.activity_listview, from, to);

								listView.setAdapter(adapter);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Toast.makeText(getApplicationContext(),
								error.getMessage(), Toast.LENGTH_LONG).show();
					}
				})
				{
					@Override
					protected Map<String, String> getParams() {
						Map<String, String> params = new HashMap<String, String>();
						params.put("id", session.getId());
						return params;
					}
				};
				AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
			}
		}).start();


		listView.setOnTouchListener(new View.OnTouchListener() {

			DisplayMetrics dm = getResources().getDisplayMetrics();
			int width = dm.widthPixels;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				float x = event.getX();

				percentageX = (int) (((x * 100) / width) * 2);

				e=event;

				new Thread(new Runnable() {
					@Override
					public void run() {
						pos = getTouchPosition(e, listView);
					}
				}).start();

				if (pos != -1) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							new Thread(new Runnable() {
								@Override
								public void run() {
									deviceSlide = Item_List.get(pos);
									sb = deviceSlide.get(DEVICE_DEVICETOPIC);
									c = deviceSlide.get(DEVICE_STATUS);

									if (c == "0") {
										c = "1";
									} else {
										c = "0";
									}

									if (deviceSlide.get(DEVICE_TYPE) == "1") {
										new Thread(new Runnable() {
											@Override
											public void run() {
												ConnectMqtt(sb, c);
											}
										}).start();
									}

									if (deviceSlide.get(DEVICE_TYPE) == "3") {
										if (percentageX < 100) {
											new Thread(new Runnable() {
												@Override
												public void run() {
													ConnectMqtt(sb, "1");
												}
											}).start();
										} else {
											new Thread(new Runnable() {
												@Override
												public void run() {
													ConnectMqtt(sb, "2");
												}
											}).start();
										}
									}
								}
							}).start();
							break;
						case MotionEvent.ACTION_MOVE:
							new Thread(new Runnable() {
								@Override
								public void run() {
									if (deviceSlide.get(DEVICE_TYPE) == "2") {
										ConnectMqtt(sb, String.valueOf(percentageX));
									}
								}
							}).start();
							break;
						case MotionEvent.ACTION_UP:
							new Thread(new Runnable() {
								@Override
								public void run() {
									if (deviceSlide.get(DEVICE_TYPE) == "3") {
										new Thread(new Runnable() {
											@Override
											public void run() {
												ConnectMqtt(sb, "0");
											}
										}).start();
									}

									if (deviceSlide.get(DEVICE_TYPE) == "2"){
										new Thread(new Runnable() {
											@Override
											public void run() {
												ConnectMqtt(b, c);
												String tag_string_req = "req_update";
												StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_LOGIN, new Response.Listener<String>() {

													@Override
													public void onResponse(String response) {

													}
												}, new Response.ErrorListener() {

													@Override
													public void onErrorResponse(VolleyError error) {
														Toast.makeText(getApplicationContext(),
																error.getMessage(), Toast.LENGTH_LONG).show();
													}
												}) {
													@Override
													protected Map<String, String> getParams() {
														Map<String, String> params = new HashMap<String, String>();
														params.put("deviceid", deviceSlide.get(DEVICE_ID));
														params.put("status", String.valueOf(percentageX));
														return params;
													}
												};
												AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
											}
										}).start();
									}

									if (deviceSlide.get(DEVICE_TYPE) == "1") {
										new Thread(new Runnable() {
											@Override
											public void run() {
												ConnectMqtt(b, c);
												String tag_string_req = "req_update";
												StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_LOGIN, new Response.Listener<String>() {

													@Override
													public void onResponse(String response) {
														HashMap<String, String> item = new HashMap<String, String>();
														item.put(DEVICE_ID, deviceSlide.get(DEVICE_ID));
														item.put(DEVICE_NAME, deviceSlide.get(DEVICE_NAME));
														item.put(DEVICE_STATUS, c);
														item.put(DEVICE_VALUE, deviceSlide.get(DEVICE_VALUE));
														item.put(DEVICE_TYPE, deviceSlide.get(DEVICE_TYPE));
														item.put(DEVICE_DEVICETOPIC, deviceSlide.get(DEVICE_DEVICETOPIC));
														Item_List.set(pos, item);
														String[] from = new String[]{DEVICE_NAME};
														int[] to = new int[]{R.id.label};
														SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, Item_List, R.layout.activity_listview, from, to);
														listView.setAdapter(adapter);
													}
												}, new Response.ErrorListener() {

													@Override
													public void onErrorResponse(VolleyError error) {
														Toast.makeText(getApplicationContext(),
																error.getMessage(), Toast.LENGTH_LONG).show();
													}
												}) {
													@Override
													protected Map<String, String> getParams() {
														Map<String, String> params = new HashMap<String, String>();
														params.put("deviceid", deviceSlide.get(DEVICE_ID));
														params.put("status", c);
														return params;
													}
												};
												AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
											}
										}).start();
									}
								}
							}).start();
							break;
					}
				}
				return false;
			}
		});

		// Logout button click event
		btnLogout.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				logoutUser();
			}
		});
	}

	public int getTouchPosition(MotionEvent motionEvent, ListView listView){
		// Transient properties
		int mDismissAnimationRefCount = 0;
		float mDownX;
		int mDownPosition=-1;
		View mDownView=null;

		//   Find the child view that was touched (perform a hit test)
		Rect rect = new Rect();
		int childCount = listView.getChildCount();
		int[] listViewCoords = new int[2];
		listView.getLocationOnScreen(listViewCoords);
		int x = (int) motionEvent.getRawX() - listViewCoords[0];
		int y = (int) motionEvent.getRawY() - listViewCoords[1];
		View child;
		for (int i = 0; i < childCount; i++) {
			child = listView.getChildAt(i);
			child.getHitRect(rect);
			if (rect.contains(x, y)) {
				mDownView = child;
				break;
			}
		}

		if (mDownView != null) {
			mDownX = motionEvent.getRawX();
			mDownPosition = listView.getPositionForView(mDownView);
		} else {
			mDownPosition = -1;
		}

		return mDownPosition;
	}

	private void ConnectMqtt(final String a, final String b){
		String clientId = MqttClient.generateClientId();
		final MqttAndroidClient client = new MqttAndroidClient(this, "tcp://10.109.80.61:1883", clientId);

		try {
			IMqttToken token = client.connect();
			token.setActionCallback(new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					String topic = a;
					String payload = b;
					byte[] encodedPayload = new byte[0];
					try {
						encodedPayload = payload.getBytes("UTF-8");
						MqttMessage message = new MqttMessage(encodedPayload);
						client.publish(topic, message);
					} catch (UnsupportedEncodingException | MqttException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					// Something went wrong e.g. connection timeout or firewall problems
					Toast.makeText(getApplicationContext(), "onFailure", Toast.LENGTH_SHORT).show();
				}
			});
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	private void logoutUser() {
		session.setLogin(false, null, null, null);
		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
}