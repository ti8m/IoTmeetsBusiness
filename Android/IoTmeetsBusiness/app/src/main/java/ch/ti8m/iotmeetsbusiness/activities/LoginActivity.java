package ch.ti8m.iotmeetsbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.util.FirebaseHelper;
import ch.ti8m.iotmeetsbusiness.util.Spinner;

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG = "LoginActivity";

    // GUI elements
    private EditText edit_email;
    private EditText edit_password;

    // Firebase
    private Firebase firebase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize firebase in startup activity
        Firebase.setAndroidContext(this);

        // Get reference to firebase app
        firebase = FirebaseHelper.getFirebaseRef();

        linkGuiElements();

        // set test-user login ToDo: Remove
        edit_email.setText("test@test.ch");
        edit_password.setText("test");

        firebase.unauth();
    }


    /**
     * Click-handler for login-button
     */
    public void login(View view) {

        hideKeyboard(view);
        Spinner.show(this);

        String email = edit_email.getText().toString();
        String password = edit_password.getText().toString();

        firebase.authWithPassword(email, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {

                Log.d(LOG_TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());

                // Listen for changes in the authentication state
                firebase.addAuthStateListener(new Firebase.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(AuthData authData) {
                        if (authData != null) {
                            // user is logged in
                        } else {

                            sessionExpired();

                        }
                    }
                });

                // Go to home-activity
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);

                Spinner.hide();
            }

            @Override
            public void onAuthenticationError(FirebaseError error) {

                String errorMessage;

                switch (error.getCode()) {
                    case FirebaseError.INVALID_CREDENTIALS:
                        errorMessage = getResources().getString(R.string.msgInvalidCredentials);
                        break;
                    case FirebaseError.INVALID_EMAIL:
                        errorMessage = getResources().getString(R.string.msgInvalidCredentials);
                        break;
                    case FirebaseError.INVALID_PASSWORD:
                        errorMessage = getResources().getString(R.string.msgInvalidCredentials);
                        break;
                    default:
                        errorMessage = getResources().getString(R.string.msgLoginFailed);
                        break;
                }

                Log.d(LOG_TAG, error.getMessage());
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                Spinner.hide();
            }
        });


    }

    /**
     * Click-handler for registration-button
     */
    public void register(View view) {

        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
    }


    /**
     * Show toast and go to login-activity when session has expired
     */
    private void sessionExpired() {

        String message = getResources().getString(R.string.msgSessionExpired);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        // prevent returning with back-button
        //intent.setFlags(intent.FLAG_ACTIVITY_NEW_TASK | intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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

        edit_email = (EditText) findViewById(R.id.edit_email);
        edit_password = (EditText) findViewById(R.id.edit_password);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
