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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class CustomTabsHelper {
    private static final String TAG = CustomTabsHelper.class.getSimpleName();

    private CustomTabsClient mClient;
    private CustomTabsSession mSession;

    public CustomTabsHelper(@NonNull final Context context) {
        final String packageName = context.getPackageName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            CustomTabsClient.bindCustomTabsService(context, packageName, new CustomTabsServiceConnection() {
                @Override public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                    mClient = client;
                    mSession = mClient.newSession(new CustomTabsCallback());
                }

                @Override public void onServiceDisconnected(ComponentName name) {
                    mClient = null;
                }
            });
        }
    }

    public boolean warmup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        }
        if (mClient == null) {
            return false;
        }

        mClient.warmup(0);
        return true;
    }

    public boolean mayLaunchUrl(@NonNull final String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        }
        if (mSession == null) {
            return false;
        }

        mSession.mayLaunchUrl(Uri.parse(url), null, null);
        return true;
    }

    public void launchUrl(@NonNull final Activity activity, @NonNull final String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            CustomTabsHelper.viewInBrowser(activity, url);
            return;
        }
        try {
            createBuilder(activity).enableUrlBarHiding().build().launchUrl(activity, Uri.parse(url));
        } catch (ActivityNotFoundException anfe) {
            Log.e(TAG, "could not launch url!", anfe);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private CustomTabsIntent.Builder createBuilder(@NonNull final Activity activity) {
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(mSession);
        builder.setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        builder.setStartAnimations(activity, R.anim.slide_in_right, R.anim.slide_out_left);
        builder.setExitAnimations(activity, R.anim.slide_in_left, R.anim.slide_out_right);

        return builder;
    }

    public static void viewInBrowser(Context context, String url) {
        final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(i);
        } catch (Exception ignored) { }
    }
}
