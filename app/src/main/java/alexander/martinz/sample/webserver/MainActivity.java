/*
 * Copyright 2015 Alexander Martinz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package alexander.martinz.sample.webserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import alexander.martinz.libs.webserver.Config;
import alexander.martinz.libs.webserver.NetworkInfo;
import alexander.martinz.libs.webserver.WebServerCallbacks;
import alexander.martinz.libs.webserver.routers.DefaultRouter;
import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements WebServerCallbacks {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.tv_ip) TextView tvIpAddress;
    @Bind(R.id.et_port) EditText etPort;
    @Bind(R.id.button_toggle) FloatingActionButton fabToggle;

    private DefaultRouter webServer;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // enable debugging
        Config.DEBUG = true;

        etPort.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override public void afterTextChanged(Editable s) {
                boolean valid;

                final String portString = ((s != null) ? s.toString() : null);
                if (TextUtils.isEmpty(portString)) {
                    fabToggle.setEnabled(false);
                    return;
                }

                try {
                    final int port = Integer.parseInt(portString);
                    valid = ((port <= 65535) && (port >= 1));
                } catch (Exception exc) {
                    valid = false;
                }
                fabToggle.setEnabled(valid);
            }
        });

        fabToggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (webServer == null) {
                    final int port = Integer.parseInt(etPort.getText().toString());
                    webServer = new DefaultRouter(MainActivity.this, port);
                }

                if (webServer.isAlive()) {
                    webServer.stop();

                    fabToggle.setImageResource(R.drawable.ic_portable_wifi_off_black_24dp);
                    fabToggle.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.button_off));
                } else {
                    try {
                        webServer.start();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Could not start web server!", ioe);
                        return;
                    }

                    fabToggle.setImageResource(R.drawable.ic_wifi_tethering_black_24dp);
                    fabToggle.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.button_on));
                }
            }
        });
    }

    @Override protected void onDestroy() {
        if (webServer != null) {
            webServer.stop();
        }
        super.onDestroy();
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceivers();
    }

    private void registerReceivers() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unregisterReceivers() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) { }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (tvIpAddress != null) {
                final String ip = NetworkInfo.getAnyIpAddress(context);
                tvIpAddress.setText(String.format("http://%s:", ip));
            }
        }
    };

    @Override public Context getContext() {
        return getApplicationContext();
    }
}
