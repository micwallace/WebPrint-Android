package au.com.wallaceit.webprint;/*
 * Copyright 2013 Michael Boyde Wallace (http://wallaceit.com.au)
 * This file is part of WebPrint.
 *
 * WebPrint is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebPrint is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebPrint (COPYING). If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by michael on 22/06/16.
 */

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WebPrint extends Application {
    public AccessControl accessControl;
    public SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        accessControl = new AccessControl(preferences);
    }
}
