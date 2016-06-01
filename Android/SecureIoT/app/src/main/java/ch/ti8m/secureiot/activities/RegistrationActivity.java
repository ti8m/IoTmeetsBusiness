package ch.ti8m.secureiot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
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
import ch.ti8m.secureiot.util.MySpinner;

public class RegistrationActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RegistrationActivity";

    // GUI elements
    private TextView txt_deviceid;
    private TextView txt_ssid;

    private String deviceMac;
    private String ssid;

    private MqttAndroidClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get device-mac and ssid from intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            deviceMac = (String) bundle.get("DEVICE_MAC");
            ssid = (String) bundle.get("SSID");
            Log.d(LOG_TAG, deviceMac);
            Log.d(LOG_TAG, ssid);
        }

        linkGuiElements();
        initMqtt();

        txt_deviceid.setText(deviceMac);
        txt_ssid.setText(ssid);


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

            case  "AUTH_VALIDATE":{

                // disconnect mqtt-client
                try {
                    IMqttToken disconToken = mqttClient.disconnect();
                    disconToken.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            // we are now successfully disconnected
                            MySpinner.hide();

                            // Go to validation-activity
                            Intent intent = new Intent(RegistrationActivity.this, ValidationActivity.class);
                            intent.putExtra("DEVICE_MAC", deviceMac);
                            intent.putExtra("SSID", ssid);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken,
                                              Throwable exception) {
                            // something went wrong, but probably we are disconnected anyway
                            MySpinner.hide();
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }

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
    private void publishMessage(String m){

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
     * Click-handler for register-button
     */
    public void register(View view) {

        MySpinner.show(this);
        publishMessage("AUTH_REQUEST");
    }


    /**
     * Click-handler for cancel-button
     */
    public void cancel(View view) {

        // disconnect mqtt-client and go to main-activity
        try {
            IMqttToken disconToken = mqttClient.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    // Go to main-activity
                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
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
     * Linking all gui-elements
     */
    private void linkGuiElements() {

        txt_ssid = (TextView) findViewById(R.id.txt_ssid);
        txt_deviceid = (TextView) findViewById(R.id.txt_deviceId);
    }

}
