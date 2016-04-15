package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.persistency.User;
import ch.ti8m.iotmeetsbusiness.util.FirebaseHelper;
import ch.ti8m.iotmeetsbusiness.util.MyRequestQueue;

public class NewSensorActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NewSensorActivity";

    // GUI elements
    private EditText edit_description;
    private EditText edit_location;

    private Firebase firebase;
    private User user;


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

        hideKeyboard(view);

        String description = edit_description.getText().toString();
        String location = edit_location.getText().toString();

        AuthData authData = firebase.getAuth();
        String uid = authData.getUid();

        // Get ref to users channels on firebase
        Firebase channelRef = firebase.child("users/" + uid + "/channels");

        // Add channel-id to users channels
        //channelRef.child("1234").setValue(true);


        String url = "https://api.thingspeak.com/channels.json";

//        JsonObjectRequest jsObjRequest = new JsonObjectRequest
//                (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
//
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        //mTxtDisplay.setText("Response: " + response.toString());
//                        Log.d(LOG_TAG, response.toString());
//                    }
//                }, new Response.ErrorListener() {
//
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        // TODO Auto-generated method stub
//
//                    }
//                }
//
//                ) {
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("api_key", "4PULA5YY773OR5A9");
//                params.put("name", "Test Channel");
//                params.put("latitude", "0");
//                params.put("longitude", "0");
//                params.put("field1", "°C");
//                params.put("field2", "%");
//                params.put("tags", "IoTmeetsBusiness");
//
//                return params;
//            }
//        };
//
//        // Access the RequestQueue through your singleton class.
//        MyRequestQueue.getInstance(this).addToRequestQueue(jsObjRequest);

        Map<String, String> params = new HashMap<String, String>();
        params.put("api_key", "4PULA5YY773OR5A9");
        params.put("name", "Test Channel");
        params.put("latitude", "0");
        params.put("longitude", "0");
        params.put("field1", "°C");
        params.put("field2", "%");
        params.put("tags", "IoTmeetsBusiness");

        JsonObjectRequest req = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                            Log.d(LOG_TAG, response.toString());
                            Log.d(LOG_TAG, response.getString("id"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

        // add the request object to the queue to be executed
        MyRequestQueue.getInstance(this).addToRequestQueue(req);


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
