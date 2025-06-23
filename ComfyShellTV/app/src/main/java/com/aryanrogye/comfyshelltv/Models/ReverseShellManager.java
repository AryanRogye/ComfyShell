package com.aryanrogye.comfyshelltv.Models;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import android.os.Handler;
import android.os.Looper;
final public class ReverseShellManager {

    private String ip;
    private volatile Boolean didStart = false;
    int port = 4444;

    private Socket socket;
    private Process process;
    private Thread stdoutThread, stderrThread, stdinThread;

    public ReverseShellManager(String ip) {
        this.ip = ip;
    }

    public Boolean getDidStart() {
        return this.didStart;
    }

    public void setDidStart(Boolean d) {
        this.didStart = d;
    }

    public void start_reverse_shell(Context target) {
        new Thread(() -> {
            try {
                this.socket = new Socket(ip, port);
                this.process = Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c", "cd /data/data/" + yourPackageName() + " && exec /system/bin/sh"});

                this.didStart = true;

                InputStream pi = process.getInputStream();    // shell output
                OutputStream po = process.getOutputStream();  // shell input
                InputStream pe = process.getErrorStream();    // shell errors

                OutputStream so = socket.getOutputStream();   // to Mac
                InputStream si = socket.getInputStream();     // from Mac

                // Shell stdout & stderr → Mac
                stdoutThread = new Thread(() -> pipe(pi, so));
                stderrThread = new Thread(() -> pipe(pe, so));
                stdinThread  = new Thread(() -> pipe(si, po));

                stdoutThread.start();
                stderrThread.start();
                stdinThread.start();

                // wait for shell exit in separate thread
                new Thread(() -> {
                    try {
                        process.waitFor(); // shell exited (e.g. user typed `exit`)
                        System.out.println("Shell exited with code: " + process.exitValue());
                        stop();
                    } catch (InterruptedException ignored) {}
                }).start();

            } catch (IOException e) {
                this.didStart = false;
                System.out.println("There Was A Error Creating a Reverse Shell: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(target, "There Was A Error Creating a Reverse Shell" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private String yourPackageName() {
        return "com.aryanrogye.comfyshelltv"; // hardcoded is fine for now
    }

    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (process != null) process.destroy();

            if (stdoutThread != null) stdoutThread.interrupt();
            if (stderrThread != null) stderrThread.interrupt();
            if (stdinThread != null) stdinThread.interrupt();

            didStart = false;
        } catch (IOException e) {
            System.out.println("Error stopping reverse shell: " + e.getMessage());
        }
    }

    private void pipe(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while (!Thread.currentThread().isInterrupted() && (len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("There Was A Error With Piping: " + e.getMessage());
            new Thread(this::stop).start();
        }
    }

}
