package ch.ti8m.iotmeetsbusiness.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.util.ApplicationController;
import ch.ti8m.iotmeetsbusiness.util.Spinner;

public class ConnectSensorActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ConnectSensorActivity";

    // GUI elements
    private EditText edit_ssid;
    private EditText edit_wlan_password;

    private String writeKey;
    private boolean isReceiverRegistered = false;
    private WifiManager wifiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get channel write-key from intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            writeKey = (String) bundle.get("WRITE_KEY");
            Log.d(LOG_TAG, writeKey);
        }


        linkGuiElements();

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // ToDo: Remove
        edit_ssid.setText("MeBu-Mobil");
        edit_wlan_password.setText("0987qWerty");

    }


    protected void onPause() {
        super.onPause();
        if (isReceiverRegistered) {
            isReceiverRegistered = false;
            unregisterReceiver(receiver);
        }
    }


    /**
     * Click-handler for connect-button
     */
    public void connect(View view) {

        if (!isPhoneConnected()) {

            // show toast "phone not connected"
            String message = getResources().getString(R.string.msgPhoneNotConnected);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

            return;
        }

        // Register listener for Wifi-state changes
        if (!isReceiverRegistered) {
            isReceiverRegistered = true;
            registerReceiver(receiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        }

        hideKeyboard(view);
        Spinner.show(this);

        String URL = "http://192.168.4.1/ajax/connect";
        // Setup POST-parameter
        Map<String, String> params = new HashMap<>();
        params.put("network", edit_ssid.getText().toString());
        params.put("password", edit_wlan_password.getText().toString());
        params.put("channelKey", writeKey);

        JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            if (response.getBoolean("connected")) {

                                // success

                            } else {

                                // Show error-toast
                                String message = getResources().getString(R.string.msgCanNotConnect);
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {

                            // disconnect from sensor
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            int netId = wifiInfo.getNetworkId();
                            wifiManager.removeNetwork(netId);

                            // Go to home-activity
                            Intent intent = new Intent(ConnectSensorActivity.this, HomeActivity.class);
                            intent.putExtra("RECONNECT", true);
                            startActivity(intent);

                            Spinner.hide();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                // Show error-toast
                String message = getResources().getString(R.string.msgCanNotConnect);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                //Log.d(LOG_TAG, error.getMessage());
                Spinner.hide();
            }
        });

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);

    }


    /**
     * Checks if the phone is connected to wlan "iot-meets-business"
     */
    private boolean isPhoneConnected() {

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        String ssid = wifiInfo.getSSID();

        if (ssid.equals("\"iot-meets-business\"")) {
            return true;

        } else {
            return false;
        }

    }


    /**
     * Hiding the soft keyboard
     */
    private void hideKeyboard(View view) {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    /**
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        edit_ssid = (EditText) findViewById(R.id.edit_ssid);
        edit_wlan_password = (EditText) findViewById(R.id.edit_wlan_password);
    }


    /**
     *  Get wifi network-info
     */
    private NetworkInfo getNetworkInfo(Context context) {

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }


    /**
     * Broadcast-Receiver to handle changes of wifi-state
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent i) {

            NetworkInfo info = getNetworkInfo(context);

            if (info != null && info.isConnected()) {
                Log.d(LOG_TAG, "Connected");

                Intent intent = new Intent(ConnectSensorActivity.this, HomeActivity.class);
                startActivity(intent);

                Spinner.hide();

            } else {
                Log.d(LOG_TAG, "Disconnected");

                // reconnect to wifi
                wifiManager.reconnect();

                Intent intent = new Intent(ConnectSensorActivity.this, HomeActivity.class);
                startActivity(intent);

                Spinner.hide();

            }
        }
    };




}
