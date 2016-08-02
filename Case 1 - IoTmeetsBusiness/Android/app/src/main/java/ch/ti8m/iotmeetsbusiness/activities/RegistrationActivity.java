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

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.persistency.User;
import ch.ti8m.iotmeetsbusiness.util.FirebaseHelper;
import ch.ti8m.iotmeetsbusiness.util.Spinner;

public class RegistrationActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RegistrationActivity";

    // GUI elements
    private EditText edit_firstName;
    private EditText edit_lastName;
    private EditText edit_email;
    private EditText edit_password;
    private EditText edit_repeatPassword;

    private Firebase firebase;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get reference to firebase root
        firebase = FirebaseHelper.getFirebaseRef();

        linkGuiElements();
    }

    /**
     * Click-handler for registration-button
     */
    public void register(View view) {

        hideKeyboard(view);

        String firstName = edit_firstName.getText().toString();
        String lastName = edit_lastName.getText().toString();
        String email = edit_email.getText().toString();
        String password = edit_password.getText().toString();
        String repeatPassword = edit_repeatPassword.getText().toString();

        // Check password
        if (!password.equals(repeatPassword)) {

            // Clear password fields
            edit_repeatPassword.setText("");
            edit_password.setText("");
            edit_password.requestFocus();

            String message = getResources().getString(R.string.msgUnequalPasswords);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, message);

            return;
        }

        // Create new user-object
        user = new User(firstName, lastName, email);

        Spinner.show(this);

        // Create user-account on firebase
        firebase.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {

                // Save user-object on firebase
                String uid = (String) result.get("uid");
                firebase.child("users").child(uid).setValue(user);

                // Show success message & log
                String message = getResources().getString(R.string.msgRegistrationSuccessful);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "Successfully created user account with uid: " + uid);

                // Go to login-activity
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);

                Spinner.hide();
            }

            @Override
            public void onError(FirebaseError error) {

                String errorMessage;

                switch (error.getCode()) {
                    case FirebaseError.EMAIL_TAKEN:
                        errorMessage = getResources().getString(R.string.msgEmailTaken);
                        edit_email.requestFocus();
                        break;
                    case FirebaseError.INVALID_EMAIL:
                        errorMessage = getResources().getString(R.string.msgInvalidEmail);
                        edit_email.requestFocus();
                        break;
                    case FirebaseError.INVALID_PASSWORD:
                        errorMessage = getResources().getString(R.string.msgInvalidPassword);
                        edit_repeatPassword.setText("");
                        edit_password.setText("");
                        edit_password.requestFocus();
                        break;
                    default:
                        errorMessage = getResources().getString(R.string.msgRegistrationFailed);
                        break;
                }


                Log.d(LOG_TAG, error.getMessage());
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();

                Spinner.hide();
            }
        });

    }

    /**
     * Hiding the soft keyboard and removing focus from input fields
     */
    private void hideKeyboard(View view) {

        edit_firstName.clearFocus();
        edit_lastName.clearFocus();
        edit_email.clearFocus();
        edit_password.clearFocus();
        edit_repeatPassword.clearFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    /**
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        edit_firstName = (EditText) findViewById(R.id.edit_firstName);
        edit_lastName = (EditText) findViewById(R.id.edit_lastName);
        edit_email = (EditText) findViewById(R.id.edit_email);
        edit_password = (EditText) findViewById(R.id.edit_password);
        edit_repeatPassword = (EditText) findViewById(R.id.edit_RepeatPassword);

    }

}
