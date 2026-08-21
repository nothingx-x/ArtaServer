package xyz.nothing.artaserver;


import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

// A simple http server for testing plugin
public class SimpleHttpServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        server.createContext("/webhook", httpExchange -> {
            String method = httpExchange.getRequestMethod();
            if (!method.equalsIgnoreCase("post")) {
                String response = "Not found";
                httpExchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }

            System.out.println("received a request at path /webhook");

            String response = "Success";
            httpExchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.setExecutor(null); // Use default executor
        server.start();
    }
}
