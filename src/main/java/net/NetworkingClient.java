package net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class NetworkingClient {

    private static final Logger logger = LoggerFactory.getLogger(NetworkingClient.class);

    public static byte[] get (String url) throws IOException, InterruptedException {
        logger.info("Getting url {}", url);
        URI uri = URI.create(url);
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .uri(uri).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();

    }
}
