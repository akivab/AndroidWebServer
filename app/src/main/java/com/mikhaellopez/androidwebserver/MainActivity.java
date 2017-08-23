package com.mikhaellopez.androidwebserver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends AppCompatActivity {

    private static final int DEFAULT_PORT = 8080;
    private static final String TAG = "AndroidWebServer";

    // INSTANCE OF ANDROID WEB SERVER
    private AndroidWebServer androidWebServer;
    private BroadcastReceiver broadcastReceiverNetworkState;
    private static boolean isStarted = false;

    // VIEW
    private CoordinatorLayout coordinatorLayout;
    private EditText editTextPort;
    private FloatingActionButton floatingActionButtonOnOff;
    private View textViewMessage;
    private TextView textViewIpAccess;

    void tryStartingAndroidWebServer() {
        if (!isStarted && startAndroidWebServer()) {
            isStarted = true;
            textViewMessage.setVisibility(View.VISIBLE);
            floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorRed));
            editTextPort.setEnabled(false);
        } else if (stopAndroidWebServer()) {
            isStarted = false;
            textViewMessage.setVisibility(View.INVISIBLE);
            floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorGreen));
            editTextPort.setEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INIT VIEW
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        textViewMessage = findViewById(R.id.textViewMessage);
        textViewIpAccess = (TextView) findViewById(R.id.textViewIpAccess);
        setIpAccess();
        floatingActionButtonOnOff = (FloatingActionButton) findViewById(R.id.floatingActionButtonOnOff);
        floatingActionButtonOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnectedToInternet()) {
                    tryStartingAndroidWebServer();
                } else {
                    Snackbar.make(coordinatorLayout, getString(R.string.wifi_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });

        if (isConnectedToInternet()) {
            setIpAccess();
            tryStartingAndroidWebServer();
        }
    }

    //region Start And Stop AndroidWebServer
    private boolean startAndroidWebServer() {
        if (!isStarted) {
            int port = getPortFromEditText();
            try {
                if (port == 0) {
                    throw new Exception();
                }
                androidWebServer = new AndroidWebServer(port);
                androidWebServer.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(coordinatorLayout, "The PORT " + port + " doesn't work, please change it between 1000 and 9999.", Snackbar.LENGTH_LONG).show();
            }
        }
        return false;
    }

    private boolean stopAndroidWebServer() {
        if (isStarted && androidWebServer != null) {
            androidWebServer.stop();
            return true;
        }
        return false;
    }
    //endregion

    //region Private utils Method
    private void setIpAccess() {
        textViewIpAccess.setText(getLocalIpAddress());
    }


    private int getPortFromEditText() {
        String valueEditText = editTextPort.getText().toString();
        return (valueEditText.length() > 0) ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
    }

    public boolean isConnectedToInternet() {
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
                //&& wifiManager.isWifiEnabled() && networkInfo.getTypeName().equals("WIFI")) {
            return true;
        }
        return false;
    }
    //endregion
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ip = "(" + intf.getName() + ") " + inetAddress.getHostAddress();
                        Log.d(TAG, "Saw IP: " + ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isStarted) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.dialog_exit_message)
                        .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.cancel), null)
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAndroidWebServer();
        isStarted = false;
        if (broadcastReceiverNetworkState != null) {
            unregisterReceiver(broadcastReceiverNetworkState);
        }
    }

}
