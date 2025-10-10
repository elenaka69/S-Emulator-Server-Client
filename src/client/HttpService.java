package client;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class HttpService {
    private final HttpClient client = HttpClient.newHttpClient();

    public void postJsonAsync(String url, String json,
                              java.util.function.Consumer<String> onSuccess,
                              java.util.function.Consumer<Throwable> onError) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(onSuccess)
                .exceptionally(ex -> { onError.accept(ex); return null; });
    }
}
