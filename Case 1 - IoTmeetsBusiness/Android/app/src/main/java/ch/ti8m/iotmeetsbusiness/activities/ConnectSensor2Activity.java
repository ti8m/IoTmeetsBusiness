package ch.ti8m.iotmeetsbusiness.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
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
import java.util.List;
import java.util.Map;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.util.ApplicationController;
import ch.ti8m.iotmeetsbusiness.util.Spinner;

public class ConnectSensor2Activity extends AppCompatActivity {

    private static final String LOG_TAG = "ConnectSensor2Activity";

    // GUI elements
    private EditText edit_ssid;
    private EditText edit_wlan_password;

    private String writeKey;
    private boolean stateReceiverRegistered = false;
    private boolean scanReceiverRegistered = false;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_sensor2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get channel write-key form intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            writeKey = (String) bundle.get("WRITE_KEY");
            Log.d(LOG_TAG, writeKey);
        }

        linkGuiElements();

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // ToDo: Remove
        edit_ssid.setText("ABC");
        edit_wlan_password.setText("crazygirl08");

    }


    protected void onPause() {
        super.onPause();
        if (stateReceiverRegistered) {
            stateReceiverRegistered = false;
            unregisterReceiver(wifiStateReceiver);
        }

        if (scanReceiverRegistered) {
            scanReceiverRegistered = false;
            unregisterReceiver(scanResultReceiver);
        }
    }


    /**
     * Click-handler for connect-button
     */
    public void connect(View view) {

        // Register listener for Wifi-state changes
        if (!stateReceiverRegistered) {

            stateReceiverRegistered = true;
            IntentFilter stateIntent = new IntentFilter();
            stateIntent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            registerReceiver(wifiStateReceiver, stateIntent);

        }

        if (!scanReceiverRegistered) {

            scanReceiverRegistered = true;
            IntentFilter scanResultIntent = new IntentFilter();
            scanResultIntent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(scanResultReceiver, scanResultIntent);

        }

        hideKeyboard(view);
        Spinner.show(this);

        // connect phone to sensor wlan
        //connectToSensor();
        wifiManager.startScan();
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
     * Connect phone to sensor wlan
     */
    private void connectToSensor() {

        String ssid = "iot-meets-business";

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";

        // for open network
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        // for WPA
        //conf.preSharedKey = "\"password\"";

        wifiManager.addNetwork(conf);

        // Search the network "iot-meets-business" and connect
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }

        }

//        List<ScanResult> list = wifiManager.getScanResults();
//        for( ScanResult i : list ) {
//            if(i.SSID != null && i.SSID.equals(ssid)) {
//
//                wifiManager.disconnect();
//                wifiManager.enableNetwork(conf.networkId, true);
//                wifiManager.reconnect();
//
//                found = true;
//                break;
//            }
//
//        }
//
//        if(!found){
//
//            // network network "iot-meets-business" not found
//            String message = getResources().getString(R.string.msgSensorNotFound);
//            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
//
//            Spinner.hide();
//        }

    }

    private void connectSensorToWlan() {

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
                                wifiManager.disconnect();
                                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                                int netId = wifiInfo.getNetworkId();
                                wifiManager.removeNetwork(netId);

                            } else {

                                // Show error-toast
                                String message = getResources().getString(R.string.msgCanNotConnect);
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {

                            // disconnect from sensor
//                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                            int netId = wifiInfo.getNetworkId();
//                            wifiManager.removeNetwork(netId);

                            // Go to home-activity
                            Intent intent = new Intent(ConnectSensor2Activity.this, HomeActivity.class);
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
     * Get wifi network-info
     */
    private NetworkInfo getNetworkInfo(Context context) {

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }


    /**
     * Broadcast-Receiver to handle changes of wifi-state
     */
    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = networkInfo.getState();

                if (state == NetworkInfo.State.CONNECTED) {
                    String ssid = wifiManager.getConnectionInfo().getSSID();

                    if (ssid.equals("\"iot-meets-business\"") && networkInfo.isConnected()) {

                        connectSensorToWlan();
                        stateReceiverRegistered = false;
                        unregisterReceiver(wifiStateReceiver);
                    }

                    Log.d(LOG_TAG, "Connected to: " + ssid);

                }

                if (state == NetworkInfo.State.DISCONNECTED) {
                    Log.d(LOG_TAG, "Disconnected");
                }
            }
        }
    };

    /**
     * Broadcast-Receiver to handle wifi-scan results
     */
    private BroadcastReceiver scanResultReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            String ssid = "iot-meets-business";
            boolean found;

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                found = false;

                List<ScanResult> list = wifiManager.getScanResults();
                for (ScanResult i : list) {
                    if (i.SSID != null && i.SSID.equals(ssid)) {

                        connectToSensor();
                        scanReceiverRegistered = false;
                        unregisterReceiver(scanResultReceiver);
                        found = true;
                        break;
                    }

                }

                if (!found) {

                    // network network "iot-meets-business" not found
                    String message = getResources().getString(R.string.msgSensorNotFound);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    Spinner.hide();
                }
            }

        }
    };


}
