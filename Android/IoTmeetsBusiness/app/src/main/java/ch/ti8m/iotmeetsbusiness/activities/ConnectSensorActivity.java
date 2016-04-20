package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.util.ApplicationController;

public class ConnectSensorActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ConnectSensorActivity";

    // GUI elements
    private EditText edit_ssid;
    private EditText edit_wlan_password;

    private String writeKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_sensor);
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

        // ToDo: Remove
        edit_ssid.setText("MeBu-Mobil");
        edit_wlan_password.setText("0987qWerty");

    }

    public void connect(View view) {

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
                        // handle response
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // handle error
            }
        });

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);

    }

    /**
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        edit_ssid = (EditText) findViewById(R.id.edit_ssid);
        edit_wlan_password = (EditText) findViewById(R.id.edit_wlan_password);
    }

}
