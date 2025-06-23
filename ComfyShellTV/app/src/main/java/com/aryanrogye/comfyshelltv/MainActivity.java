package com.aryanrogye.comfyshelltv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.aryanrogye.comfyshelltv.Models.ReverseShellManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "ComfyPrefs";
    private static final String KEY_TEXT = "user_input";

    private ReverseShellManager rsManager;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);


        // set layout as view
        setContentView(generateLayout());
    }

    private LinearLayout generateLayout() {
        // root layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 60, 40, 40);
        layout.setGravity(Gravity.CENTER);

        EditText url = new EditText(this);
        url.setHint("Enter Link");
        layout.addView(url, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button search = new Button(this);
        search.setText("Search");
        layout.addView(search);

        search.setOnClickListener(v -> {
            String link = url.getText().toString().trim();
            if (link.isEmpty()) {
                Toast.makeText(this, "Enter an APK URL", Toast.LENGTH_SHORT).show();
                return;
            }
            enqueueDownload(link);   // fire & forget
            Toast.makeText(this, "Download started…", Toast.LENGTH_SHORT).show();
        });

        // input field
        EditText inputField = new EditText(this);
        inputField.setHint("Enter Your Device IPs");
        layout.addView(inputField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // button
        Button saveButton = new Button(this);
        saveButton.setText("Save");
        layout.addView(saveButton);

        // saved text view
        TextView savedText = new TextView(this);
        layout.addView(savedText);

        // shared prefs
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(KEY_TEXT, "Nothing saved yet.");
        savedText.setText("Saved IP: " + saved);

        Button start_reverse_shell = new Button(this);
        start_reverse_shell.setText("Start Reverse Shell");
        start_reverse_shell.setVisibility(isValidIp(saved) ? View.VISIBLE : View.GONE);
        layout.addView(start_reverse_shell);

        Button stop_reverse_shell = new Button(this);
        stop_reverse_shell.setText("Stop Reverse Shell");
        stop_reverse_shell.setVisibility(isValidIp(saved) ? View.VISIBLE : View.GONE);
        layout.addView(stop_reverse_shell);

        // button logic
        saveButton.setOnClickListener(v -> {
            String input = inputField.getText().toString();
            prefs.edit().putString(KEY_TEXT, input).apply();
            savedText.setText("Saved value: " + input);

            if (isValidIp(input)) {
                start_reverse_shell.setVisibility(View.VISIBLE);
                stop_reverse_shell.setVisibility(View.VISIBLE);
            } else {
                start_reverse_shell.setVisibility(View.GONE);
                stop_reverse_shell.setVisibility(View.GONE);
            }
        });

        start_reverse_shell.setOnClickListener(v -> {
            String ip = prefs.getString(KEY_TEXT, "Nothing saved yet.");
            if (rsManager == null || !rsManager.getDidStart()) {
                rsManager = new ReverseShellManager(ip);
                rsManager.start_reverse_shell(this);
                Toast.makeText(this, "Reverse Shell Started", Toast.LENGTH_SHORT).show();
            }
        });

        stop_reverse_shell.setOnClickListener(v -> {
            if (rsManager != null) {
                rsManager.stop();
                Toast.makeText(this, "Reverse Shell Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f // weight pushes it to take up remaining space
        );
        layout.addView(spacer, spacerParams);

        return layout;
    }

    public static boolean isValidIp(String ip) {
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) return false;

        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean simpleWget(String fileURL, File destination) {
        try {
            URL url = new URL(fileURL);

            // openConnection can be reused in loop for redirects
            HttpURLConnection conn;
            int redirects = 0;
            while (true) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);                    // manual reveal
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Android 10; TV) AppleWebKit/537.36 Chrome/96");

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) break;             // 200 → done

                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_MOVED_TEMP) {          // 301 / 302
                    String loc = conn.getHeaderField("Location");
                    url  = new URL(loc);                                   // follow
                    redirects++;
                    if (redirects > 5) throw new IOException("Too many redirects");
                    continue;
                }
                System.out.println("Server returned HTTP " + code);
                return false;
            }

            try (InputStream in  = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(destination)) {

                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            return destination.length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // enqueueDownload() kicks off the download;
// when it finishes, we auto-call installApk().
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void enqueueDownload(String urlStr) {

        // 1) build request
        DownloadManager dm  = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Uri uri             = Uri.parse(urlStr.trim());
        File dest           = new File(getExternalFilesDir(null), "downloaded_app.apk");

        DownloadManager.Request req = new DownloadManager.Request(uri)
                .setTitle("Downloading APK…")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(dest));

        long downloadId = dm.enqueue(req);
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();

        // 2) receiver to know when it's done
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                long done = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (done == downloadId) {
                    unregisterReceiver(this);
                    installApk(dest);   // prompt system installer
                }
            }
        };

        // 3) register with correct signature
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(br, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(br, filter);
        }
    }

    public void installApk(File apkFile) {
        if (apkFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(
                    FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile),
                    "application/vnd.android.package-archive"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show();
        }
    }

}
