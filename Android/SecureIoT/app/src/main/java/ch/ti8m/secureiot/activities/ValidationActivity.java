package ch.ti8m.secureiot.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import ch.ti8m.secureiot.R;
import ch.ti8m.secureiot.util.Dialogs;
import ch.ti8m.secureiot.util.WifiHelper;

public class ValidationActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ValidationActivity";

    // GUI elements
    private TextView txt_message;
    private TextView txt_deviceid;
    private TextView txt_countdown;
    private Button btn_confirm;
    private Button btn_reject;

    private String deviceMac;
    private WifiHelper wifiHelper;
    private MqttAndroidClient mqttClient;
    private ProgressDialog progressDialog;
    private CountDownTimer countDownTimer;
    private CountDownTimer timeoutTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get device-mac from intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            deviceMac = (String) bundle.get("DEVICE_MAC");
            Log.d(LOG_TAG, deviceMac);
        }

        linkGuiElements();
        initMqtt();

        // set deviceId
        txt_deviceid.setText(deviceMac);

        wifiHelper = new WifiHelper(this);
        progressDialog = Dialogs.getProgressDialog(getString(R.string.msgRegisterDevice), this);

    }

    @Override
    protected void onPause() {
        super.onPause();

        clearCountdown();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // start validation startCountdown
        startCountdown(20, txt_countdown);
    }


    @Override
    public void onBackPressed(){

        Dialogs.showCancelDialog(deviceMac, this, mqttClient);
    }


    /**
     *  Create and connect mqtt-client
     */
    private void initMqtt(){

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("secureIoT");
        options.setPassword("admin".toCharArray());

        String clientId = MqttClient.generateClientId();
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://m21.cloudmqtt.com:18188", clientId);

        try {
            IMqttToken token = mqttClient.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(LOG_TAG, "onSuccess");
                    // Subscribe for messages from server & device
                    subscribeMqttTopic("/server/" + deviceMac);
                    subscribeMqttTopic("/device/" + deviceMac);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(LOG_TAG, "onFailure");
                    exception.printStackTrace();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     *  Subscribe for messages from a specific topic
     */
    private void subscribeMqttTopic(String t){

        String topic = "secureIoT" + t;

        int qos = 1;
        try {
            IMqttToken subToken = mqttClient.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // subscription successful

                    mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            Log.d(LOG_TAG, "Connection Lost");
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {

                            Log.d(LOG_TAG, message.toString());
                            handleMessage(message);
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            Log.d(LOG_TAG, "Delivery Complete");
                        }
                    });

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }


            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    /**
     *  Handle incoming mqtt-messages
     */
    private void handleMessage(MqttMessage m){

        String message = m.toString();
        Log.d(LOG_TAG, "mqtt-message received: " + message );

        switch (message){

            case  "AUTH_SUCCESS":{

                progressDialog.cancel();
                timeoutTimer.cancel();
                showSuccessDialog();

                break;
            }
            case  "AUTH_FAILED":{

                progressDialog.cancel();
                timeoutTimer.cancel();
                showErrorDialog(getString(R.string.msgRegistrationFailed));

                break;
            }
            case "LOST_CONNECTION":{
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                break;
            }
        }
    }


    /**
     *  Publish mqtt-message
     */
    private void publishCommand(String m){

        String topic = "secureIoT/mobile/" + deviceMac;
        String payload = m;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            mqttClient.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }

    }


    /**
     * Click-handler for confirm-button
     */
    public void authConfirm(View view){

        clearCountdown();
        startTimeOut(20);
        publishCommand("AUTH_CONFIRM");

        btn_confirm.setEnabled(false);
        btn_reject.setEnabled(false);

        progressDialog.show();
        progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }


    /**
     * Click-handler for reject-button
     */
    public void authReject(View view){
        authReject();
    }


    /**
     * Reject registration request
     */
    public void authReject(){

        clearCountdown();
        publishCommand("AUTH_REJECT");

        // disconnect mqtt-client and go to registration-activity
        try {
            IMqttToken disconToken = mqttClient.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    // Go to registration-activity
                    Intent intent = new Intent(ValidationActivity.this, RegistrationActivity.class);
                    intent.putExtra("DEVICE_MAC", deviceMac);
                    String ssid = wifiHelper.getWifiConnectedSsid();
                    intent.putExtra("SSID", ssid);
                    startActivity(intent);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * Show dialog with success-message
     */
    private void showSuccessDialog(){

        String successMsg = getString(R.string.msgDeviceRegistered);
        AlertDialog dialog = new AlertDialog.Builder(ValidationActivity.this).create();
        dialog.setMessage(successMsg);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Fertig", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                publishCommand("AUTH_END");

                // disconnect mqtt-client and go to main-activity
                try {
                    IMqttToken disconToken = mqttClient.disconnect();
                    disconToken.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {

                            // Go to main-activity
                            Intent intent = new Intent(ValidationActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear activity back-stack
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken,
                                              Throwable exception) {
                            // something went wrong, but probably we are disconnected anyway
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }

            }
        });

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
    }


    /**
     * Start the validation countdown
     */
    private void startCountdown(int Seconds, final TextView tv){

        countDownTimer = new CountDownTimer(Seconds* 1000 + 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                tv.setText(String.format("%01d", seconds));
            }

            public void onFinish() {
                tv.setText("0");
                Toast.makeText(getApplicationContext(), R.string.msgTimeExpired, Toast.LENGTH_LONG).show();
                authReject(tv);
            }
        }.start();
    }


    /**
     * Cancel the validation countdown
     */
    private void clearCountdown(){
        countDownTimer.cancel();
        txt_countdown.setText("");
    }


    /**
     * Start the timeout countdown
     */
    private void startTimeOut(int seconds){

        timeoutTimer = new CountDownTimer(seconds* 1000 + 1000, 1000) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                progressDialog.cancel();
                showErrorDialog(getString(R.string.msgNoConnection));
            }
        }.start();
    }

    public void showErrorDialog(String message) {

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setMessage(message);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                authReject();
            }
        });

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
    }


    /**
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        txt_message = (TextView) findViewById(R.id.txt_instruction1);
        txt_deviceid = (TextView) findViewById(R.id.txt_deviceId);
        txt_countdown = (TextView) findViewById(R.id.txt_countdown);
        btn_confirm = (Button) findViewById(R.id.btn_confirm);
        btn_reject = (Button) findViewById(R.id.btn_reject);
    }

}
