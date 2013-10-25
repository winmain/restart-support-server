package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerHandler implements HttpHandler, MainServerChecker.MainServerHandler {

    private final Config config;
    private final MainServerChecker mainServerChecker;
    private List<Request> requests = new ArrayList<Request>();
    private final Object requestsSync = new Object();

    public ServerHandler(Config config) {
        this.config = config;
        mainServerChecker = new MainServerChecker(config);
        mainServerChecker.setHandler(this);
    }

    public void handle(HttpExchange httpExchange) {
        addRequest(httpExchange);
        if (mainServerChecker.test()) {
            sendRequests();
        } else {
            mainServerChecker.startLazy();
        }
    }

    public synchronized void sendRequests() {
        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            if (!request.processed) {
                try {
                    System.out.print("Proxying request #" + i + " " + request.path + " ... ");
                    HttpExchange http = request.httpExchange;
                    URL url = new URL("http", config.serverHost, config.serverPort, request.path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(request.method);
                    conn.setDoInput(true);
                    conn.setConnectTimeout(config.proxyConnectionTimeoutMillis);
                    conn.setReadTimeout(config.proxyReadTimeoutMillis);
                    conn.setInstanceFollowRedirects(false);
                    conn.setDefaultUseCaches(false);
                    conn.setUseCaches(false);

                    // Copy request headers
                    for (Map.Entry<String, List<String>> entry : request.headers) {
                        for (String value : entry.getValue()) {
                            conn.addRequestProperty(entry.getKey(), value);
                        }
                    }

                    // Copy request body
                    if (request.hasBody()) {
                        conn.setDoOutput(true);
                        OutputStream connOutput = conn.getOutputStream();
                        connOutput.write(request.body);
                        connOutput.close();
                    }

                    // Copy response headers
                    Headers httpHeaders = http.getResponseHeaders();
                    long contentLength = 0;
                    for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                        String key = entry.getKey();
                        if (key != null) {
                            httpHeaders.put(key, entry.getValue());
                            if (key.equalsIgnoreCase("Content-Length")) {
                                contentLength = Long.parseLong(entry.getValue().get(0));
                            }
                        }
                    }
                    int responseCode = conn.getResponseCode();
                    http.sendResponseHeaders(responseCode, contentLength);

                    // Copy response body
                    InputStream connInput = (responseCode >= 400 && responseCode < 600) ? conn.getErrorStream() : conn.getInputStream();
                    OutputStream responseBody = http.getResponseBody();
                    try {
                        IOUtils.copy(connInput, responseBody);
                        responseBody.close();
                        System.out.println(responseCode + " content-length:" + contentLength);
                    } catch (IOException e) {
                        // Тут возникает ошибки Broken Pipe - значит клиент закрыл соединение
                        System.out.println("broken pipe, ignoring");
                    }
                    connInput.close();

                    conn.disconnect();
                    http.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                request.processed = true;
            }
        }
        cleanRequests();
    }

    @Override
    public void onMainServerUp() {
        sendRequests();
    }

    public void stop() {
        mainServerChecker.stop();
        if (!requests.isEmpty()) {
            for (int i = 0; i < config.maxRetriesBeforeStop; i++) {
                if (mainServerChecker.test()) {
                    sendRequests();
                }
                if (requests.isEmpty()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    // ------------------------------- Private & protected methods -------------------------------

    private void addRequest(HttpExchange httpExchange) {
        synchronized (requestsSync) {
            Request request = new Request(httpExchange);
            requests.add(request);
            System.out.println("New request #" + (requests.size() - 1) + " " + request.path);
        }
    }

    private void cleanRequests() {
        synchronized (requestsSync) {
            int ln = requests.size();
            int i;
            for (i = 0; i < ln; i++) {
                if (!requests.get(i).processed) {
                    break;
                }
            }
            if (i == ln) {
                // Очищаем список только когда все запросы уже обработаны.
                requests = new ArrayList<Request>();
                System.out.println("All requests cleared");
            }
        }
    }
}
