package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.persistency.DataChannel;
import ch.ti8m.iotmeetsbusiness.util.ApplicationController;
import ch.ti8m.iotmeetsbusiness.util.FirebaseHelper;
import ch.ti8m.iotmeetsbusiness.util.MyListFragment;
import ch.ti8m.iotmeetsbusiness.util.ThingSpeakHelper;

public class HomeActivity extends AppCompatActivity {

    private static final String LOG_TAG = "HomeActivity";

    private Firebase firebase;
    private final ArrayList<DataChannel> channels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get reference to firebase root
        firebase = FirebaseHelper.getFirebaseRef();

        AuthData authData = firebase.getAuth();
        String uid = authData.getUid();

        // Get ref to user on firebase
        Firebase userRef = firebase.child("users/" + uid);

        // Get ref to users channels on firebase
        Firebase channelRef = userRef.child("channels");

        // Test Case: Try to get data from test user 2 >> have to fail when not test user 2 is logged in!
        //Firebase userRef = firebase.child("users/a131dd76-ba72-4380-89ca-3762a848cb9b");

        // Get first and last name from user
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String firstName = (String) snapshot.child("firstName").getValue();
                String lastName = (String) snapshot.child("lastName").getValue();

                Toast.makeText(getApplicationContext(), "Hallo " + lastName + " " + firstName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });

        // Get channels from firebase
        channelRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Add all channels to array-list
                for (DataSnapshot channelSnapshot : snapshot.getChildren()) {
                    DataChannel channel = channelSnapshot.getValue(DataChannel.class);
                    channels.add(channel);
                    addChannelData(channel);
                    Log.d(LOG_TAG, "add channel " + channel.getId());
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });

    }

    /**
     * Add channel data from thingspeak to a channel
     */
    private void addChannelData(final DataChannel channel) {

        Log.d(LOG_TAG, "add data to channel " + channel.getId());

        String URL = ThingSpeakHelper.getUrl() + "/" + channel.getId() + "/feeds/last.json?api_key=" + channel.getReadKey();
        //String URL = "https://api.thingspeak.com/channels/96640/feeds/last.json?api_key=WNWUPPX489LYWMV4";
        // pass second argument as "null" for GET requests
        JsonObjectRequest req = new JsonObjectRequest(URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            // Set value 1 to channel-object
                            channel.setValue1(response.getString("field1") + "°C");

                        } catch (JSONException e) {
                            e.printStackTrace();
                            channel.setValue1("-°C");

                        }
                        try {
                            // Set value 1 to channel-object
                            channel.setValue2(response.getString("field2") + "%");

                        } catch (JSONException e) {
                            e.printStackTrace();
                            channel.setValue2("-%");

                        } finally {
                            // Get access to the fragment
                            FragmentManager manager = getSupportFragmentManager();
                            MyListFragment listFragment = (MyListFragment) manager.findFragmentById(R.id.myListFragment);

                            // Set channel data to the list-view
                            listFragment.setChannelsOnList(channels);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                // ToDo: handle different error types

                // no data available
                channel.setValue1("-°C");
                channel.setValue2("-%");

                // Get access to the fragment
                FragmentManager manager = getSupportFragmentManager();
                MyListFragment listFragment = (MyListFragment) manager.findFragmentById(R.id.myListFragment);

                // Set channel data to the list-view
                listFragment.setChannelsOnList(channels);

                VolleyLog.d(LOG_TAG, "no Data for channel " + channel.getId());

            }
        });

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_addSensor) {

            // Go to new-sensor-activity
            Intent intent = new Intent(this, NewSensorActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
