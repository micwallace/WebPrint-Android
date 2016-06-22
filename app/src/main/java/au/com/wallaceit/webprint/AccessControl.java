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

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

public class AccessControl {

    private SharedPreferences preferences;
    private JSONObject aclmap;

    public AccessControl(SharedPreferences preferences) {
        this.preferences = preferences;
        this.loadAcl();
    }
    
    public ArrayList<String> getAcl(){
        ArrayList<String> list = new ArrayList<>();
        Iterator iterator = aclmap.keys();
        while (iterator.hasNext()){
            list.add((String) iterator.next());
        }
        return list;
    }

    private void loadAcl() {

        String aclString = preferences.getString("webprintAcl", "{}");
        try {
            aclmap = new JSONObject(aclString);
        } catch (JSONException e) {
            e.printStackTrace();
            aclmap = new JSONObject();
        }
    }

    private void saveAcl() {
        preferences.edit().putString("webprintAcl", aclmap.toString()).apply();
    }

    public void add(String origin, String cookie) {
        try {
            aclmap.put(origin, cookie);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.saveAcl();
    }

    public void remove(String origin) {
        aclmap.remove(origin);
        this.saveAcl();
    }

    public boolean isAllowed(String origin, String cookie) {
        if (aclmap.has(origin)) {
            try {
                if (aclmap.get(origin).equals(cookie)) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
