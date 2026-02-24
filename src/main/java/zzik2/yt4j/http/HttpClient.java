package zzik2.yt4j.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpClient {
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;

    private final String userAgent;

    public HttpClient(String userAgent) {
        this.userAgent = userAgent;
    }

    public String get(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = openConnection(url);
        applyHeaders(conn, headers);
        conn.connect();
        return readResponse(conn);
    }

    public String post(String url, String jsonBody, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = openConnection(url);
        applyHeaders(conn, headers);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(jsonBody);
        }

        conn.connect();
        return readResponse(conn);
    }

    private HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept-Charset", "utf-8");
        return conn;
    }

    private void applyHeaders(HttpURLConnection conn, Map<String, String> headers) {
        if (headers == null) return;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + code + " from " + conn.getURL());
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } finally {
            conn.disconnect();
        }
    }
}
