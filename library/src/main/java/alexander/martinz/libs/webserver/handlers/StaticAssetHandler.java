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

package alexander.martinz.libs.webserver.handlers;

import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import alexander.martinz.libs.webserver.Config;
import alexander.martinz.libs.webserver.WebServerCallbacks;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public class StaticAssetHandler extends RouterNanoHTTPD.StaticPageHandler {
    private static final String TAG = StaticAssetHandler.class.getSimpleName();

    private final AssetManager assetManager;
    private final String staticFileName;

    public StaticAssetHandler(@NonNull WebServerCallbacks webServerCallbacks) {
        this(webServerCallbacks, null);
    }

    public StaticAssetHandler(@NonNull WebServerCallbacks webServerCallbacks, @Nullable String staticFileName) {
        this.assetManager = webServerCallbacks.getContext().getAssets();
        this.staticFileName = staticFileName;
    }

    private InputStream openAsset(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("Filename is null or empty!");
        }

        // open it with buffer access, as asset files are mostly small
        return assetManager.open(fileName, AssetManager.ACCESS_BUFFER);
    }

    private NanoHTTPD.Response createChunkedResponse(InputStream inputStream, String fileName) {
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        return NanoHTTPD.newChunkedResponse(getStatus(), RouterNanoHTTPD.getMimeTypeForFile(fileName), bufferedInputStream);
    }

    @Override
    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource res, Map<String, String> params, NanoHTTPD.IHTTPSession session) {
        if (!TextUtils.isEmpty(staticFileName)) {
            if (Config.DEBUG) {
                Log.d(TAG, "serving static file: " + staticFileName);
            }
            try {
                return createChunkedResponse(openAsset(staticFileName), staticFileName);
            } catch (IOException ioe) {
                throw new RuntimeException("Check your setup!");
            }
        }

        final String sessionUri = session.getUri();
        final String baseUri = res.getUri();

        String assetUri = sessionUri;
        if (assetUri.startsWith("/")) {
            assetUri = assetUri.substring(1);
        }
        for (int index = 0; index < Math.min(baseUri.length(), assetUri.length()); index++) {
            if (baseUri.charAt(index) != assetUri.charAt(index)) {
                assetUri = assetUri.substring(index);
                break;
            }
        }

        if (Config.DEBUG) {
            Log.v(TAG, "trying to open asset: " + assetUri);
        }

        InputStream inputStream;
        try {
            inputStream = openAsset(assetUri);
            if (Config.DEBUG) {
                Log.d(TAG, "opened asset: " + assetUri);
            }
        } catch (IOException ioe) {
            // if directory listing is disabled, end here
            if (!Config.ENABLE_ASSETS_DIRECTORY_LISTING) {
                return notFoundResponse;
            }

            // else try to list files and send a very simple directory index
            final String htmlResponseText = tryListFiles(sessionUri, assetUri);
            if (TextUtils.isEmpty(htmlResponseText)) {
                return notFoundResponse;
            }
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", htmlResponseText);
        }

        // if we found the asset, wrap it around a buffered input stream
        return createChunkedResponse(inputStream, assetUri);
    }

    private String tryListFiles(String fullPath, String assetPath) {
        final String[] files;
        try {
            files = assetManager.list(assetPath);
        } catch (IOException ioeNew) {
            if (Config.DEBUG) {
                Log.e(TAG, "Could not open asset!", ioeNew);
            }
            return null;
        }

        final StringBuilder fileBuilder = new StringBuilder();
        fileBuilder.append("<html>");
        fileBuilder.append("<head><title>Index of ").append(fullPath).append("</title></head>");
        fileBuilder.append("<body>");
        fileBuilder.append("<h1>Index of ").append(fullPath).append("</h1>");
        fileBuilder.append("<hr>");
        for (final String file : files) {
            fileBuilder.append("<a href=\"").append(fullPath).append('/').append(file).append("\">").append(file).append("</a>");
            fileBuilder.append("<br/>");
        }
        fileBuilder.append("<hr>");
        fileBuilder.append("</body>");
        fileBuilder.append("</html>");
        return fileBuilder.toString();
    }

    private final NanoHTTPD.Response notFoundResponse =
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.REQUEST_TIMEOUT, "text/plain", null);
}
