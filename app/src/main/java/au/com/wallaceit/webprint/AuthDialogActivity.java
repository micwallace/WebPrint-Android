package au.com.wallaceit.webprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Window;

public class AuthDialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        new AlertDialog.Builder(this)
            .setTitle(R.string.access_request_title)
            .setMessage(getString(R.string.access_request_msg, getIntent().getStringExtra("origin")))
            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnResultToService(false);
                }
            })
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnResultToService(true);
                }
            })
            .show();
    }

    private void returnResultToService(boolean accpeted){
        synchronized (RelayService.authLock) {
            RelayService.authResult = accpeted;
            RelayService.authLock.notify();
        }
        finish();
    }
}
