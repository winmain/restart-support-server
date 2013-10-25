package server;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class MainServerChecker {

    private final Config config;
    private Thread thread = null;
    private boolean stopChecker = false;
    private MainServerHandler handler = null;
    private boolean lastServerStatus = true;

    public MainServerChecker(Config config) {
        this.config = config;
    }

    public void startLazy() {
        if (thread == null) {
            stopChecker = false;
            thread = new Thread(new Checker(), "Main server checker");
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            stopChecker = true;
        }
    }

    public MainServerHandler getHandler() {
        return handler;
    }

    public void setHandler(MainServerHandler handler) {
        this.handler = handler;
    }

    public synchronized boolean test() {
        try {
            Socket socket = new Socket(config.serverHost, config.serverPort);
            socket.close();
            if (!lastServerStatus) {
                System.out.println("+++ " + new Date() + " Main server is online");
                lastServerStatus = true;
            }
            return true;
        } catch (IOException e) {
            if (lastServerStatus) {
                System.out.println("--- " + new Date() + " Main server goes offline");
                lastServerStatus = false;
            }
            return false;
        }
    }

    // ------------------------------- Inner classes -------------------------------

    interface MainServerHandler {
        void onMainServerUp();
    }

    private class Checker implements Runnable {
        @Override
        public void run() {
            try {
                while (!stopChecker && !test()) {
                    Thread.sleep(config.mainServerCheckPeriodMillis);
                }
                if (!stopChecker && handler != null) {
                    handler.onMainServerUp();
                }
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
    }
}
