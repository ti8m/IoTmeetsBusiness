package ch.ti8m.secureiot.util;

import android.app.ProgressDialog;
import android.content.Context;

import ch.ti8m.secureiot.R;

/**
 * Created by sa005 on 13.04.2016.
 */
public class Spinner {

    private static ProgressDialog spinnerDialog;

    public static void show(Context context) {

        spinnerDialog = new ProgressDialog(context, R.style.SpinnerDialogTheme);
        spinnerDialog.setCancelable(false);
        spinnerDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        spinnerDialog.show();
    }

    public static void hide() {

        if (spinnerDialog != null) {
            spinnerDialog.dismiss();
        }
    }
}
