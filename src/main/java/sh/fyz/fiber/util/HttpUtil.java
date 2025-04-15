package sh.fyz.fiber.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpUtil {

    private static final int CONNECT_TIMEOUT = 5000; // ms
    private static final int READ_TIMEOUT = 5000;    // ms

    // === GET ===
    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        return sendRequest(url, "GET", null, headers);
    }

    // === POST ===
    public static String post(String url, String body) throws IOException {
        return post(url, body, null);
    }

    public static String post(String url, String body, Map<String, String> headers) throws IOException {
        return sendRequest(url, "POST", body, headers);
    }

    // === PUT ===
    public static String put(String url, String body) throws IOException {
        return put(url, body, null);
    }

    public static String put(String url, String body, Map<String, String> headers) throws IOException {
        return sendRequest(url, "PUT", body, headers);
    }

    // === DELETE ===
    public static String delete(String url) throws IOException {
        return delete(url, null);
    }

    public static String delete(String url, Map<String, String> headers) throws IOException {
        return sendRequest(url, "DELETE", null, headers);
    }

    // === Core Request ===
    private static String sendRequest(String urlString, String method, String body, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoInput(true);

            // Apply headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // Handle request body
            if (body != null && !body.isEmpty() && ("POST".equals(method) || "PUT".equals(method))) {
                connection.setDoOutput(true);
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.writeBytes(body);
                    out.flush();
                }
            }

            // Read response
            int status = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()
            ));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }

            reader.close();
            return response.toString().trim();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
