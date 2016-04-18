package ch.ti8m.iotmeetsbusiness.util;

import com.firebase.client.Firebase;

/**
 * Created by sa005 on 13.04.2016.
 */
public class FirebaseHelper {

    private static Firebase ref;

    public static Firebase getFirebaseRef() {

        if (ref == null) {

            ref = new Firebase("https://iot-meets-business.firebaseio.com/");
        }

        return ref;
    }
}
