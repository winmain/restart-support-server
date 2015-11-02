package server.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Java systemd-notify interface.
 */
public class SdDaemon {
    public static void status(String status) {
        notify("--status=" + status);
    }

    /**
     * Tells the service manager that service startup is finished.
     */
    public static void ready() {
        notify("--ready");
    }

    public static boolean booted() {
        return Files.isDirectory(Paths.get("/run/systemd/system"), LinkOption.NOFOLLOW_LINKS);
    }

    private static void notify(String arg) {
        if (!booted()) {
            System.out.println("INFO: Cannot notify - not booted");
            return;
        }
        if (System.getenv("NOTIFY_SOCKET") == null) {
            System.out.println("ERROR: Cannot notify - NOTIFY_SOCKET not set");
            return;
        }
        try {
            Process p = new ProcessBuilder("systemd-notify", arg)
                 .redirectErrorStream(true)
                 .start();
            if (ignoreInterruptedException(p::waitFor) == 0)
                return;

            System.out.println("ERROR: Failed to notify systemd of/that " + arg + "; systemd-notify exited with status " + p.exitValue());
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                r.lines().forEach(l -> System.out.println("ERROR: systemd-notify: " + l));
            }
        } catch (Exception e) {
            System.out.println("WARN: Failed to notify systemd of service readiness");
            e.printStackTrace();
        }
    }

    private interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    private static <T> T ignoreInterruptedException(ThrowingSupplier<T, InterruptedException> r) {
        Supplier<Object> s;
        for (; ; ) {
            try {
                return r.get();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
