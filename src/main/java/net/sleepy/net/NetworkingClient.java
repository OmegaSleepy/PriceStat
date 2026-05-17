package net.sleepy.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public class NetworkingClient {

    private static final Logger logger = LoggerFactory.getLogger(NetworkingClient.class);

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static Optional<byte[]> get (String url) throws IOException, InterruptedException {
        logger.info("Getting url {}", url);
        URI uri = URI.create(url);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(5))
                .uri(uri)
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            logger.info("Bad response code {}", response.statusCode());
            return Optional.empty();
        }

        return Optional.of(response.body());
    }
}