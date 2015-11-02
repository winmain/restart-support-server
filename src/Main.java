import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import server.Config;
import server.ServerHandler;
import server.util.SdDaemon;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        Config config = new Config(args);

        InetSocketAddress address = new InetSocketAddress(config.listenHost, config.listenPort);
        HttpServer server = HttpServer.create(address, 0);
        final ServerHandler handler = new ServerHandler(config);
        server.createContext("/", handler);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handler.stop();
            }
        });

        SdDaemon.ready();
        System.out.println("Listening " + config.listenHost + ":" + config.listenPort + " for proxy to " + config.serverHost + ":" + config.serverPort);

        while (true) {
            Thread.sleep(1000);
        }
    }
}
