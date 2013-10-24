package server;

import com.sun.net.httpserver.HttpExchange;
import server.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Request {
    HttpExchange httpExchange;
    boolean processed = false;

    final String method;
    final String path;
    final Set<Map.Entry<String, List<String>>> headers;
    final byte[] body;

    Request(HttpExchange http) {
        this.httpExchange = http;
        method = http.getRequestMethod();
        path = http.getRequestURI().getPath();
        headers = http.getRequestHeaders().entrySet();

        {
            InputStream reqBody = http.getRequestBody();
            byte[] bytes = new byte[0];
            try {
                if (reqBody.available() != 0) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(reqBody.available());
                    IOUtils.copy(reqBody, bos);
                    bytes = bos.toByteArray();
                }
                reqBody.close();
            } catch (IOException ignored) {
            }
            body = bytes;
        }
    }

    boolean hasBody() {
        return body.length > 0;
    }
}

