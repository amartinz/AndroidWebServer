/*
 * Copyright (C) 2013 - 2014 Alexander Martinz
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

package alexander.martinz.libs.webserver;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class for network information like getting the ip address of the device
 */
public class NetworkInfo {
    private static final String TAG = NetworkInfo.class.getSimpleName();

    public static String getAnyIpAddress(@NonNull Context context) {
        String ip = getWifiIp(context);
        if ("0.0.0.0".equals(ip)) {
            ip = getIpAddress(true);
        }
        return ip;
    }

    public static String getWifiIp(@NonNull Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        final String formattedIp = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        if (BuildConfig.DEBUG) {
            Log.v(TAG, String.format("formattedIp (wifi) -> %s", formattedIp));
        }
        return formattedIp;
    }

    public static String getIpAddress(final boolean useIPv4) {
        List<NetworkInterface> interfaces;
        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (Exception e) {
            interfaces = null;
        }
        if (interfaces == null) {
            return "0.0.0.0";
        }
        for (NetworkInterface intf : interfaces) {
            final List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (final InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress()) {
                    final String sAddr = addr.getHostAddress().toUpperCase();
                    boolean isIPv4 = isIPv4Address(sAddr);
                    if (useIPv4) {
                        if (isIPv4) {
                            return sAddr;
                        }
                    } else {
                        if (!isIPv4) {
                            final int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                            return ((delim < 0) ? sAddr : sAddr.substring(0, delim));
                        }
                    }
                }
            }
        }
        return "0.0.0.0";
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

}
