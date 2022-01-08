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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

public class RelayService extends Service {
    private static int notifyId = 1;
    private NotificationManager mNotificationManager;
    private Server htserver;
    private WebPrint app;
    private int sourceport;
    private UsbManager usbManager;
    private HashMap<String, UsbDevice> usbPrinters;
    public static final Object authLock = new Object();
    public static boolean authResult = false;

    public RelayService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        app = (WebPrint) getApplicationContext();
        Bundle bundle = intent.getExtras();
        if (bundle!=null) {
            sourceport = Integer.parseInt(bundle.getString("sourceport"));
        }
        if (startRelay()) {
            createNotification(null);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        stopRelay();
        removeNotification();
    }

    private boolean started = false;
    private boolean even = true;
    private void createNotification(String tickertxt){
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(getString(R.string.server_running))
                        .setContentText(getString(R.string.print_server_running));
        PendingIntent pe = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pe);
        if (tickertxt!=null){
            mBuilder.setTicker(tickertxt+(even?"":" ")); // a hack to make ticker show each request even though text has not changed
            even = !even;
        }
        mBuilder.setOngoing(true);
        if (started) {
            mNotificationManager.notify(notifyId, mBuilder.build());
        } else {
            startForeground(notifyId, mBuilder.build());
            started = true;
        }
    }

    private void removeNotification(){
        mNotificationManager.cancel(notifyId);
    }

    private boolean startRelay(){
        htserver = new Server(RelayService.this);
        try {
            htserver.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void stopRelay(){
        htserver.stop();
    }

    public void refreshUsbDevices(){
        usbPrinters = new HashMap<>();
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (String s : deviceList.keySet()) {
            UsbDevice device = deviceList.get(s);
            boolean deviceIsPrinter = false;
            if (device.getDeviceClass()==UsbConstants.USB_CLASS_PER_INTERFACE) {
                for (int i=0; i<device.getInterfaceCount(); i++) {
                    if (device.getInterface(i).getInterfaceClass()==UsbConstants.USB_CLASS_PRINTER) {
                        deviceIsPrinter = true;
                        break;
                    }
                }
            } else if (device.getDeviceClass()==UsbConstants.USB_CLASS_PRINTER){
                deviceIsPrinter = true;
            }
            System.out.println("Printer added to list: "+device.getDeviceName());
            if (deviceIsPrinter)
                usbPrinters.put(s, device);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class Server extends NanoHTTPD {

        private Service parent;

        public Server(Service service){
            super("127.0.0.1", sourceport);
            parent = service;
            System.out.println("Relay Started on port "+sourceport+";");
        }

        @Override
        public void stop(){
            super.stop();
            parent.stopSelf();
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> files = new HashMap<>();
            Method method = session.getMethod();
            String responseBody = "1";


            if (Method.GET.equals(method)) {
                if (session.getUri().equals("/printwindow")) {
                    responseBody = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><style>h1, h2 { color:#0078ae; font-family:helvetica; font-size:110%; }</style>"
                            + "<script>document.addEventListener('freeze', (event) => { window.opener.postMessage({a:'freeze'}, '*'); }); window.addEventListener('message',sendData); window.blur(); function sendData(event){ var data = JSON.parse(event.data); data.origin = event.origin; data = JSON.stringify(data); var xmlhttp=new XMLHttpRequest(); try { xmlhttp.open('POST','/',false); xmlhttp.send(data); if (xmlhttp.status!=200){ window.opener.postMessage({a:'error'}, '*'); return; } var response = xmlhttp.responseText; if (response!=1){ event.source.postMessage({a:'response', json:response}, '*'); } }catch(err){ window.opener.postMessage({a:'error'}, '*'); }  } window.opener.postMessage({a:'init'}, '*');</script></head>";
                    responseBody += "<body style='text-align:center;'><h1 style='margin-top:50px;'>Connected to the Print Service</h1><h2>You can minimize this window, but leave it open for faster printing</h2><img style=\"margin-top:20px; width:50px;\" id=\"wscan-loader\" src=\"data:image/gif;base64,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\"/></body></html>";
                } else if (session.getUri().equals("/stopserver")){
                    this.stop();
                }
            } else if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
                // parse the request
                String postBody = session.getQueryParameterString();
                JSONObject responseJson = new JSONObject();
                try {
                    JSONObject request = new JSONObject(postBody);
                    String action = request.getString("a");

                    if (!request.has("origin")) {
                        responseJson.put("error", "Invalid authentication credentials provided.");
                    } else {
                        String origin = request.getString("origin");
                        String cookie = "";
                        if (request.has("cookie")) {
                            cookie = request.getString("cookie");
                        }
                        if (action.equals("init")) {
                            if (!app.accessControl.isAllowed(origin, cookie)) {
                                System.out.println("Authentication needed for "+origin);
                                // Not authenticated, show dialog
                                authResult = false;
                                Intent intent = new Intent(RelayService.this, AuthDialogActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                intent.putExtra("origin", origin);
                                startActivity(intent);
                                synchronized (authLock) {
                                    try {
                                        authLock.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (authResult) {
                                    // add to acl and return cookie
                                    String ncookie = (UUID.randomUUID()).toString();
                                    app.accessControl.add(origin, ncookie);
                                    responseJson.put("cookie", ncookie);
                                    responseJson.put("ready", true);
                                    System.out.println("Access granted for "+origin);
                                } else {
                                    responseJson.put("error", "Webprint access has been denied for this site.");
                                    System.out.println("Access denied for "+origin);
                                }
                            } else {
                                responseJson.put("ready", true);
                            }
                        } else {
                            if (app.accessControl.isAllowed(origin, cookie)) {
                                if (action.equals("listprinters")) {
                                    refreshUsbDevices();
                                    JSONArray deviceArr = new JSONArray();
                                    for (String s : usbPrinters.keySet()){
                                        deviceArr.put(s);
                                    }
                                    responseJson.put("printers", deviceArr);
                                }
                                if (action.equals("printraw")) {
                                    byte[] data = Base64.decode(request.getString("data"), Base64.DEFAULT);
                                    if (request.has("printer")) {
                                        String printerName = request.getString("printer");
                                        if (usbPrinters.containsKey(printerName)) {
                                            UsbDevice printer = usbPrinters.get(printerName);
                                            authResult = usbManager.hasPermission(printer);
                                            if (!authResult) {
                                                usbManager.requestPermission(printer, PendingIntent.getBroadcast(RelayService.this, 0, new Intent(UsbReceiver.ACTION_USB_PERMISSION), 0));
                                                synchronized (authLock) {
                                                    try {
                                                        authLock.wait();
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                            if (authResult) {
                                                if (sendUsb(printer, data)){
                                                    createNotification("Print Job Submitted");
                                                } else {
                                                    createNotification("Print Job Error!");
                                                }
                                            } else {
                                                responseJson.put("error", "Webprint access was denied for this printer.");
                                                System.out.println("USB Access denied for "+origin);
                                            }
                                        } else {
                                            responseJson.put("error", "The selected printer is not powered on or disconnected.");
                                        }
                                    } else if (request.has("socket")) {
                                        // send to socket
                                        String[] parts = request.getString("socket").split(":");
                                        if (sendSocket(parts[0], parts[1], data)){
                                            createNotification("Print Job Submitted");
                                        } else {
                                            createNotification("Print Job Error!");
                                        }
                                    } else {
                                        responseJson.put("error", "No printer specified in the request.");
                                    }
                                }
                                System.out.println(action);
                            } else {
                                responseJson.put("error", origin+" has not been allowed access to web print yet.\nTry refreshing the page.");
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    try {
                        responseJson.put("error", e.getMessage());
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
                responseBody = responseJson.toString();
            }
            //return super.serve(session);
            Response response = new Response(responseBody);
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "POST, PUT, GET");
            response.addHeader("Access-Control-Max-Age", "3600");
            response.addHeader("Access-Control-Allow-Headers", "x-requested-with");
            return response;
        }

        private boolean sendUsb(UsbDevice printer, byte[] data){

            UsbInterface intf = null;
            if (printer.getInterfaceCount()>1 && printer.getDeviceClass()==UsbConstants.USB_CLASS_PER_INTERFACE){
                for (int i=0; i<printer.getInterfaceCount(); i++){
                    if (printer.getInterface(i).getInterfaceClass()==UsbConstants.USB_CLASS_PRINTER) {
                        intf = printer.getInterface(i);
                        break;
                    }
                }
                if (intf == null)
                    return false;
            } else {
                intf = printer.getInterface(0);
            }
            for (int i = 0; i < intf.getEndpointCount(); i++) {
                UsbEndpoint endpoint = intf.getEndpoint(i);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        UsbDeviceConnection connection = usbManager.openDevice(printer);
                        connection.claimInterface(intf, true);
                        int result = connection.bulkTransfer(endpoint, data, data.length, 0); //do in another thread
                        connection.releaseInterface(intf);
                        connection.close();
                        return result!=0;
                    }
                }
            }
            System.out.println("No valid USB endpoint found");
            return false;
        }

        private boolean sendSocket(String host, String port, byte[] data){
            Socket sock;
            try {
                sock = new Socket(host, Integer.parseInt(port));
                DataOutputStream dataOutputStream = new DataOutputStream(sock.getOutputStream());
                dataOutputStream.write(data);
                dataOutputStream.flush();
                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } catch (IOException e) {
                System.out.println("TCP IO failed");
                e.printStackTrace();
                return false;
            }
        }
    }

}
