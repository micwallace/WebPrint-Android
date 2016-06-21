package au.com.wallaceit.webprint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbReceiver extends BroadcastReceiver {
    public static final String ACTION_USB_PERMISSION = "au.com.wallaceit.webprint.USB_PERMISSION";

    public UsbReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
            synchronized (RelayService.authLock) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                Log.d("Webprint Permission", "permission result " + granted);
                RelayService.authResult = granted;
                RelayService.authLock.notify();
            }
        }
    }
}
