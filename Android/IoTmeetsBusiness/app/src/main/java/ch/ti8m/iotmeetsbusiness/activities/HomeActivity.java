package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.util.FirebaseHelper;

public class HomeActivity extends AppCompatActivity {

    private Firebase firebase;

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

        // Test Case: Try to get data from test user 2 >> have to fail when not test user 2 is logged in!
        //Firebase userRef = firebase.child("users/a131dd76-ba72-4380-89ca-3762a848cb9b");

        // Get first and last name from user
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String firstName = (String) snapshot.child("firstName").getValue();
                String lastName = (String) snapshot.child("lastName").getValue();

                Toast.makeText(getApplicationContext(), "Hallo " + lastName + " " + firstName, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });


    }

    public void addSensor(View view) {


        // Go to new-sensor-activity
        Intent intent = new Intent(this, NewSensorActivity.class);
        startActivity(intent);

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
