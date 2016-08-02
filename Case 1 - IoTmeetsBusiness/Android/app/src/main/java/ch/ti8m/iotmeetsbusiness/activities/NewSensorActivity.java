package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Context;
import android.content.Intent;
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
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.persistency.DataChannel;
import ch.ti8m.iotmeetsbusiness.util.ApplicationController;
import ch.ti8m.iotmeetsbusiness.util.FirebaseHelper;
import ch.ti8m.iotmeetsbusiness.util.Spinner;
import ch.ti8m.iotmeetsbusiness.util.ThingSpeakHelper;

public class NewSensorActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NewSensorActivity";

    // GUI elements
    private EditText edit_description;
    private EditText edit_location;

    private Firebase firebase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get reference to firebase root
        firebase = FirebaseHelper.getFirebaseRef();

        linkGuiElements();
    }

    /**
     * Click-handler for save-button
     */
    public void save(View view) {

        Spinner.show(this);
        hideKeyboard(view);

        String description = edit_description.getText().toString();
        String location = edit_location.getText().toString();

        AuthData authData = firebase.getAuth();
        String uid = authData.getUid();

        // Get ref to users channels on firebase
        final Firebase channelRef = firebase.child("users/" + uid + "/channels");

        // URL to thngspeak api
        String url = ThingSpeakHelper.getUrl() + ".json";

        // Setup POST-parameter
        Map<String, String> params = getDefaultParams();
        params.put("name", description);
        params.put("description", location);
        // ToDo: Add more parameters

        // HTTP POST with volley-library
        JsonObjectRequest req = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // Create new channel-object
                        DataChannel channel = createChannel(response);

                        // Add channel to user on firebase
                        channelRef.push().setValue(channel);

                        // Go to connect-phone-activity
                        Intent intent = new Intent(NewSensorActivity.this, ConnectPhoneActivity.class);
                        String writeKey = channel.getWriteKey();
                        intent.putExtra("WRITE_KEY", writeKey);
                        startActivity(intent);

                        Spinner.hide();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                // show error message
                String message = getResources().getString(R.string.msgErrorOccurred);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                Log.d(LOG_TAG, error.getMessage());
                Spinner.hide();
            }
        });

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);


    }


    /**
     * Creates a new DataChannel object from the Json-response
     */
    private DataChannel createChannel(JSONObject response) {

        DataChannel channel = new DataChannel();

        try {

            channel.setId(response.getString("id"));
            channel.setDescription(response.getString("name"));
            channel.setLocation(response.getString("description"));
            channel.setWriteKey(response.getJSONArray("api_keys").getJSONObject(0).getString("api_key"));
            channel.setReadKey(response.getJSONArray("api_keys").getJSONObject(1).getString("api_key"));
            channel.setPosition(response.getString("latitude") + "/" + response.getString("longitude"));

            // Get tags
            JSONArray jsonTags = response.getJSONArray("tags");
            ArrayList<String> stringTags = new ArrayList<>();

            for (int i = 0; i < jsonTags.length(); ++i) {
                String tag = jsonTags.getJSONObject(i).getString("name");
                stringTags.add(tag);
            }

            // set tags
            channel.setTags(stringTags);


            Log.d(LOG_TAG, response.toString());
            Log.d(LOG_TAG, response.getJSONArray("tags").toString());
            Log.d(LOG_TAG, response.getJSONArray("api_keys").getJSONObject(0).getString("api_key"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return channel;

    }


    /**
     * Get default post-parameter for creating a new thingspeak-channel
     */
    private Map<String, String> getDefaultParams() {

        Map<String, String> params = new HashMap<>();
        params.put("api_key", ThingSpeakHelper.getApiKey());
        params.put("latitude", "0");
        params.put("longitude", "0");
        params.put("field1", "°C");
        params.put("field2", "%");
        //params.put("tags", "IoTmeetsBusiness");

        return params;

    }


    /**
     * Hiding the soft keyboard and removing focus from input fields
     */
    private void hideKeyboard(View view) {

        edit_description.clearFocus();
        edit_location.clearFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    /**
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        edit_description = (EditText) findViewById(R.id.edit_description);
        edit_location = (EditText) findViewById(R.id.edit_location);
    }


}
