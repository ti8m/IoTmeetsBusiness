package ch.ti8m.secureiot.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import ch.ti8m.secureiot.R;
import ch.ti8m.secureiot.activities.MainActivity;

/**
 * Created by sa005 on 13.04.2016.
 */
public class Dialogs {


    public static void showMessageDialog(String message, Context context) {

        AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setMessage(message);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
    }



    public static void showCancelDialog(final String deviceMac, final Context context, final MqttAndroidClient mqttClient) {

        AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setMessage("Registrierung abbrechen?");
        dialog.setCancelable(false);

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Ja", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                if(mqttClient != null){

                    publishCommand("AUTH_CANCEL", deviceMac, mqttClient);



                    // disconnect mqtt-client and go to main-activity
                    try {
                        IMqttToken disconToken = mqttClient.disconnect();
                        disconToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {

                                Intent intent = new Intent(context, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |  Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear activity back-stack
                                context.startActivity(intent);
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

                } else {

                    // go to home-activity
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |  Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear activity back-stack
                    context.startActivity(intent);

                }

            }
        });

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Nein", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
    }




    public static ProgressDialog getProgressDialog(String message, Context context) {

        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(message);
        //dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        return dialog;
    }

    /**
     *  Publish mqtt-message
     */
    private static void publishCommand(String m, String deviceMac, final MqttAndroidClient mqttClient ){

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


}

