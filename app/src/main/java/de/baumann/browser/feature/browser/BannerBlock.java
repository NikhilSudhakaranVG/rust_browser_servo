package de.baumann.browser.feature.browser;

import de.baumann.browser.feature.common.NinjaToast;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class BannerBlock {
    private static final String FILE = "banners.txt";
    private static volatile String configString = "";
    private static final AtomicBoolean isUpdating = new AtomicBoolean(false);

    private static void loadHosts(final Context context) {
        try {
            File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);
            if (!file.exists()) {
                configString = "";
                return;
            }
            String jsonDataString = new String(Files.readAllBytes(Paths.get(file.getPath())));
            JSONObject jsonData = new JSONObject(jsonDataString);
            JSONArray data = jsonData.getJSONArray("data");
            configString = data.toString().replaceAll("\\\\\"", "\\\\\\\\\"");
        } catch (IOException e) {
            Log.i(TAG, "FOSS Browser: loadHosts IOException: " + e);
            configString = "";
        } catch (JSONException e) {
            Log.w(TAG, "FOSS Browser: Malformed cookie banner rules JSON: " + e);
            configString = "";
        } catch (Exception e) {
            Log.e(TAG, "FOSS Browser: Unexpected error loading cookie banner rules: " + e);
            configString = "";
        }
    }

    public static void downloadBanners(final Context context) {
        if (!isUpdating.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> {
            String hostURL = "https://raw.githubusercontent.com/mozilla/cookie-banner-rules-list/main/cookie-banner-rules-list.json";
            File tempFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/banners.tmp");
            File bannerFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);

            try {
                URL url = new URL(hostURL);
                Log.d("browser", "Download Mozilla cookie banner rules");
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        NinjaToast.show(context, "Downloading cookie-banner-rules.");
                    } catch (Exception ignored) {}
                });

                URLConnection ucon = url.openConnection();
                ucon.setReadTimeout(5000);
                ucon.setConnectTimeout(10000);

                try (InputStream is = ucon.getInputStream();
                     BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                     FileOutputStream outStream = new FileOutputStream(tempFile)) {
                    byte[] buff = new byte[5 * 1024];
                    int len;
                    while ((len = inStream.read(buff)) != -1) {
                        outStream.write(buff, 0, len);
                    }
                    outStream.flush();
                }

                // Atomically move temp file to destination
                Files.move(tempFile.toPath(), bannerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                loadHosts(context);
                Log.w("browser", "Mozilla cookie banner rules updated");
            } catch (IOException i) {
                Log.w("browser", "Error updating Mozilla cookie banner rules", i);
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                isUpdating.set(false);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public BannerBlock(Context context) {
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);
        if (!file.exists()) {
            downloadBanners(context);
        } else {
            Calendar time = Calendar.getInstance();
            time.add(Calendar.DAY_OF_YEAR, -3);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Date lastModified = new Date(file.lastModified());
            if (lastModified.before(time.getTime())) {
                if (sp.getBoolean("profileStandard_adBlock", true)) {
                    downloadBanners(context);
                }
            }
        }
        loadHosts(context);
    }

    public static String getBannerBlockScriptPageStarted() {
        String currentConfig = configString;
        if (currentConfig == null || currentConfig.isEmpty()) return null;
        return "var configString = '" + currentConfig + "';\n" +
                "        var config = JSON.parse(configString);\n" +
                "        var currentDomain = window.location.hostname;\n" +
                "        function isSubdomain(subdomain, domain) {\n" +
                "            return subdomain.endsWith(\".\" + domain) || subdomain === domain;\n" +
                "        }\n" +
                "        for (var i = 0; i < config.length; i++) {\n" +
                "            var item = config[i];\n" +
                "            // Check if the current domain is in the list of specified domains or a subdomain \n" +
                "            if (item.domains.some(domain => isSubdomain(currentDomain, domain))) {\n" +
                "                  if (item.cookies) {\n" +
                "                      // Set cookies first\n" +
                "                      var optOutCookies = item.cookies.optOut;\n" +
                "                      if (optOutCookies && Array.isArray(optOutCookies) && optOutCookies.length > 0) {\n" +
                "                          for (var k = 0; k < optOutCookies.length; k++) { \n" +
                "                              var cookie = optOutCookies[k];\n" +
                "                              document.cookie = cookie.name + \"=\" + cookie.value + \"; path=/; domain=\" + currentDomain;\n" +
                "                          }\n" +
                "                      }\n" +
                "                  }\n" +
                "            }\n" +
                "        }";
    }

    public static String getBannerBlockScriptPageFinished() {
        String currentConfig = configString;
        if (currentConfig == null || currentConfig.isEmpty()) return null;
        return "var configString = '" + currentConfig + "';\n" +
                "        var config = JSON.parse(configString);\n" +
                "        var currentDomain = window.location.hostname;\n" +
                "        function isSubdomain(subdomain, domain) {\n" +
                "            return subdomain.endsWith(\".\" + domain) || subdomain === domain;\n" +
                "        }\n" +
                "        for (var i = 0; i < config.length; i++) {\n" +
                "            var item = config[i];\n" +
                "            // Check if the current domain is in the list of specified domains or a subdomain \n" +
                "            if (item.domains.length === 0 || item.domains.some(domain => isSubdomain(currentDomain, domain))) {\n" +
                "               // Proceed with the presence check\n" +
                "               if (item.click) { \n" +
                "                   var presenceElements = document.querySelectorAll(item.click.presence);\n" +
                "                   if (presenceElements.length > 0) {\n" +
                "                       // Introduce a short delay before clicking the opt-out button\n" +
                "                       setTimeout(function () {\n" +
                "                           var optOutElements = document.querySelectorAll(item.click.optOut);\n" +
                "                           for (var j = 0; j < optOutElements.length; j++) {\n" +
                "                               optOutElements[j].click();\n" +
                "                           }\n" +
                "                       }, 300);\n" +
                "                      break; // Stop iterating if one banner is found\n" +
                "                   }\n" +
                "               }\n" +
                "            }\n" +
                "        }";
    }
}
