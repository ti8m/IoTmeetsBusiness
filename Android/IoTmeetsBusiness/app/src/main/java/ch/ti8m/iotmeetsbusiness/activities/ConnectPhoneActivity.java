package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import ch.ti8m.iotmeetsbusiness.R;

public class ConnectPhoneActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ConnectPhoneActivity";

    // GUI elements
    private TextView txt_connection_status;

    private String writeKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_phone);
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

        //isPhoneConnected();

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        isPhoneConnected();
    }


    /**
     * Click-handler for network-settings-button
     */
    public void networkSettings(View view) {

        // Go to the network-settings activity
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    /**
     * Click-handler for next-button
     */
    public void next(View view) {

        if (isPhoneConnected()) {

            // Go to connect-sensor-activity
            Intent intent = new Intent(ConnectPhoneActivity.this, ConnectSensorActivity.class);
            intent.putExtra("WRITE_KEY", writeKey);
            startActivity(intent);

        } else {

            // show toast "phone not connected"
            String message = getResources().getString(R.string.msgPhoneNotConnected);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Checks if the phone is connected to wlan "iot-meets-business"
     * and set connection info message
     */
    private boolean isPhoneConnected() {

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        String ssid = wifiInfo.getSSID();

        if (ssid.equals("\"iot-meets-business\"")) {

            // set green message "connected"
            txt_connection_status.setText(R.string.connected);
            txt_connection_status.setTextColor(getResources().getColor(R.color.green));

            return true;

        } else {

            // set red message "not connected"
            txt_connection_status.setText(R.string.notConnected);
            txt_connection_status.setTextColor(getResources().getColor(R.color.red));

            return false;
        }

    }

    /**
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        txt_connection_status = (TextView) findViewById(R.id.txt_connection_status);
    }
}