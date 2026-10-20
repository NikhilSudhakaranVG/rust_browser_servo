package de.baumann.browser.feature.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class AdBlock {
    private static final String FILE = "hosts.txt";
    private static volatile Set<String> hosts = Collections.emptySet();
    private static final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private static final AtomicBoolean isLoading = new AtomicBoolean(false);
    @SuppressLint("ConstantLocale")
    private static final Locale locale = Locale.getDefault();

    public AdBlock(Context context) {
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);
        if (!file.exists()) {
            // copy hosts.txt from assets if not available
            Log.d("Hosts file", "does not exist");
            try {
                AssetManager manager = context.getAssets();
                try (InputStream in = manager.open(FILE);
                     OutputStream out = Files.newOutputStream(file.toPath())) {
                    copyFile(in, out);
                }
                downloadHosts(context);  // try to update hosts.txt from internet
            } catch (IOException e) {
                Log.e("browser", "Failed to copy asset file", e);
            }
        }

        Calendar time = Calendar.getInstance();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("sp_savedata", false)) {
            time.add(Calendar.DAY_OF_YEAR, -7);
        } else {
            time.add(Calendar.DAY_OF_YEAR, -1);
        }

        Date lastModified = new Date(file.lastModified());
        if (lastModified.before(time.getTime()) || getHostsDate(context).isEmpty()) {
            // also download again if something is wrong with the file
            downloadHosts(context);
        }

        if (hosts.isEmpty()) {
            loadHosts(context);
        }
    }

    public static String getHostsDate(Context context) {
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);
        if (!file.exists()) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Date:")) {
                    return "hosts.txt " + line.substring(2);
                }
            }
        } catch (IOException i) {
            Log.w("browser", "Error getting hosts date", i);
        }
        return "";
    }

    private static void loadHosts(final Context context) {
        if (!isLoading.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);
                if (!file.exists()) {
                    return;
                }
                Set<String> newHosts = new HashSet<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("#")) continue;
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            newHosts.add(trimmed.toLowerCase(locale));
                        }
                    }
                }
                hosts = Collections.unmodifiableSet(newHosts);
            } catch (IOException i) {
                Log.w("browser", "Error loading adBlockHosts", i);
            } finally {
                isLoading.set(false);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void downloadHosts(final Context context) {
        if (!isUpdating.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String hostURL = sp.getString("ab_hosts", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts");

            File tempFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/temp_hosts.txt");
            File outFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + FILE);

            try {
                URL url = new URL(hostURL);
                Log.d("browser", "Download AdBlock hosts");
                URLConnection connection = url.openConnection();
                connection.setReadTimeout(5000);
                connection.setConnectTimeout(10000);

                try (InputStream is = connection.getInputStream();
                     BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                     FileOutputStream outStream = new FileOutputStream(tempFile)) {
                    byte[] buff = new byte[5 * 1024];
                    int len;
                    while ((len = inStream.read(buff)) != -1) {
                        outStream.write(buff, 0, len);
                    }
                    outStream.flush();
                }

                // Remove leading 0.0.0.0 and write to final file
                try (BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                     FileWriter out = new FileWriter(outFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("0.0.0.0 ")) {
                            line = line.substring(8);
                        }
                        out.write(line + "\n");
                    }
                }

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                loadHosts(context); // reload hosts after update
                Log.w("browser", "AdBlock hosts updated");

            } catch (IOException i) {
                Log.w("browser", "Error updating AdBlock hosts", i);
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

    private static String getDomain(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String cleanUrl = url.toLowerCase(locale).trim();

        int index = cleanUrl.indexOf('/', 8); // -> http://(7) and https://(8)
        if (index != -1) {
            cleanUrl = cleanUrl.substring(0, index);
        }

        try {
            URI uri = new URI(cleanUrl);
            String domain = uri.getHost();
            if (domain == null) {
                return cleanUrl;
            }
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return cleanUrl;
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public boolean isAd(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String domain = getDomain(url);
        if (domain == null) {
            return false;
        }
        return hosts.contains(domain.toLowerCase(locale));
    }
}
