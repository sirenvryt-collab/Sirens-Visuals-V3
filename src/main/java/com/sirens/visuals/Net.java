package com.sirens.visuals;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class Net {
    private Net() { }
    static final HttpClient HC = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public static byte[] bytes(String url) {
        try {
            HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "SirensVisuals/2.0").GET().build();
            HttpResponse<byte[]> r = HC.send(rq, HttpResponse.BodyHandlers.ofByteArray());
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Exception e) { return null; }
    }

    public static String text(String url) {
        byte[] b = bytes(url);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    public static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
