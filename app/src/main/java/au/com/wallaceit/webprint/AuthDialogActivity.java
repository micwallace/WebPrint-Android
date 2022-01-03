/**
 * This file is part of WebPrint
 *
 * @author Michael Wallace
 *
 * Copyright (C) 2016 Michael Wallace, WallaceIT
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 */
package au.com.wallaceit.webprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;

public class AuthDialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
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
        }
    }

    private void returnResultToService(boolean accpeted){
        synchronized (RelayService.authLock) {
            RelayService.authResult = accpeted;
            RelayService.authLock.notify();
        }
        finish();
    }
}
