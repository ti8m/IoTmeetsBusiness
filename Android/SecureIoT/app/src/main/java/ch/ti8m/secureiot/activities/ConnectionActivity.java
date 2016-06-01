package ch.ti8m.secureiot.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
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
import java.util.List;

import ch.ti8m.secureiot.EsptouchTask;
import ch.ti8m.secureiot.IEsptouchListener;
import ch.ti8m.secureiot.IEsptouchResult;
import ch.ti8m.secureiot.IEsptouchTask;
import ch.ti8m.secureiot.R;
import ch.ti8m.secureiot.task.__IEsptouchTask;
import ch.ti8m.secureiot.util.MySpinner;
import ch.ti8m.secureiot.util.WifiHelper;

public class ConnectionActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ConnectionActivity";
    private static final String STATE_MAC = "deviceMac";

    // GUI elements
    private TextView txt_ssid;
    private EditText edit_wlan_password;
    private Switch switch_ssidHidden;

    private WifiHelper wifiHelper;
    private Spinner mSpinnerTaskCount;

    private IEsptouchTask mEsptouchTask;

    private MqttAndroidClient mqttClient;

    private String deviceMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            // Restore mac-address of iot-device
            deviceMac = savedInstanceState.getString(STATE_MAC);
        }

        wifiHelper = new WifiHelper(this);

        linkGuiElements();

        txt_ssid.setFocusable(false);
        //btn_register.setEnabled(false);

        //initSpinner();

        initMqtt();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // display the connected ap's ssid
        String apSsid = wifiHelper.getWifiConnectedSsid();

        if (apSsid != null) {

            txt_ssid.setText(apSsid);

        } else {

            txt_ssid.setText(getResources().getString(R.string.notConnected));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save mac-address of iot-device
        savedInstanceState.putString(STATE_MAC, deviceMac);
        super.onSaveInstanceState(savedInstanceState);
    }



    /**
     * Click-handler for connect-button
     */
    public void connect(View view) {

        hideKeyboard(view);

        String apPassword = edit_wlan_password.getText().toString();

        if(apPassword.equals("")){

            String message = getResources().getString(R.string.msgSetPassword);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        if(!isPhoneConnected()){

            String message = getResources().getString(R.string.msgPhoneNotConnected);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        deviceMac = null;

        String apSsid = txt_ssid.getText().toString();
        String apBssid = wifiHelper.getWifiConnectedBssid();
        Boolean isSsidHidden = false; //switch_ssidHidden.isChecked();
        String isSsidHiddenStr = "NO";
        String taskResultCountStr = "1"; //Integer.toString(mSpinnerTaskCount.getSelectedItemPosition());
        if (isSsidHidden)
        {
            isSsidHiddenStr = "YES";
        }

        new EsptouchAsyncTask3().execute(apSsid, apBssid, apPassword, isSsidHiddenStr, taskResultCountStr);
    }

    /**
     * Click-handler for cancel-button
     */
    public void cancel(View view) {

        // Go to main-activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


    /**
     * Checks if the phone is connected to wlan
     */
    private boolean isPhoneConnected() {

        String ssid = wifiHelper.getWifiConnectedSsid();
        if (ssid == null || TextUtils.isEmpty(ssid)) {
            return false;
        } else {
            return true;
        }
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

        txt_ssid = (TextView) findViewById(R.id.txt_ssid);
        edit_wlan_password = (EditText) findViewById(R.id.edit_wlan_password);
        //switch_ssidHidden = (Switch) findViewById(R.id.switch_ssidHidden);
        //mSpinnerTaskCount = (Spinner) findViewById(R.id.spinnerTaskResultCount);

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

            case  "CONNECTED":{

                // Stop, if task is still running
                if (mEsptouchTask != null) {
                    mEsptouchTask.interrupt();
                }

                // disconnect mqtt-client
                try {
                    IMqttToken disconToken = mqttClient.disconnect();
                    disconToken.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {

                            MySpinner.hide();

                            // Go to registration-activity
                            Intent intent = new Intent(ConnectionActivity.this, RegistrationActivity.class);
                            intent.putExtra("DEVICE_MAC", deviceMac);
                            String ssid = txt_ssid.getText().toString();
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

//    private void initSpinner()
//    {
//
//        int[] spinnerItemsInt = getResources().getIntArray(R.array.taskResultCount);
//        int length = spinnerItemsInt.length;
//        Integer[] spinnerItemsInteger = new Integer[length];
//        for(int i=0;i<length;i++)
//        {
//            spinnerItemsInteger[i] = spinnerItemsInt[i];
//        }
//        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
//                android.R.layout.simple_list_item_1, spinnerItemsInteger);
//        mSpinnerTaskCount.setAdapter(adapter);
//        mSpinnerTaskCount.setSelection(1);
//    }



    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {

        deviceMac = result.getBssid();

        subscribeMqttTopic("/device/" + deviceMac);

//        runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                String text = deviceMac + " ist verbunden";
//                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
//            }
//
//        });

    }

    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };




    private class EsptouchAsyncTask3 extends AsyncTask<String, Void, List<IEsptouchResult>> {

        private ProgressDialog mProgressDialog;

        //private IEsptouchTask mEsptouchTask;
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(ConnectionActivity.this);
            mProgressDialog.setMessage("Verbindung wird hergestellt...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (__IEsptouchTask.DEBUG) {
                            Log.i(LOG_TAG, "progress dialog is canceled");
                        }
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });

//            String buttonMessage = getResources().getString(R.string.msgPleaseWait);
//            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, buttonMessage, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//
//                }
//            });

            mProgressDialog.show();
//            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                String apSsid = params[0];
                String apBssid = params[1];
                String apPassword = params[2];
                String isSsidHiddenStr = params[3];
                String taskResultCountStr = params[4];
                boolean isSsidHidden = false;
                if (isSsidHiddenStr.equals("YES")) {
                    isSsidHidden = true;
                }
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, isSsidHidden, ConnectionActivity.this);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {

//            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
//            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText("Ok");

            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("IoT-Gerät " + resultInList.getBssid() + " wurde erfolgreich verbunden");

                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }

                    if (count < result.size()) {
                        sb.append("\nthere's " + (result.size() - count)
                                + " more result(s) without showing\n");
                    }

                    //mProgressDialog.setMessage(sb.toString());

                } else {
                    ///mProgressDialog.setMessage(getResources().getString(R.string.msgCanNotConnect));
                    mProgressDialog.cancel();
                    showErrorDialog();
                }
            }
        }
    }

    /**
     * Show dialog with error-message
     */
    private void showErrorDialog(){

        AlertDialog dialog = new AlertDialog.Builder(ConnectionActivity.this).create();
        dialog.setMessage(getResources().getString(R.string.msgCanNotConnect));
        //dialog.setCancelable(false);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
    }


    public void test(View view){

        MySpinner.show(this);
        deviceMac = "18fe34a37794";
        subscribeMqttTopic("/device/" + deviceMac);

        String topic = "secureIoT/device/" + "18fe34a37794";
        String payload = "CONNECTED";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            mqttClient.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

}
