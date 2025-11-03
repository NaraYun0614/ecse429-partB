package org.example;

import okhttp3.*;

public class Api {
    private final OkHttpClient client = new OkHttpClient();
    private final String base = "http://localhost:4567";

    public Response checkService() {
        return getRequest("", "json");
    }

    public Response getRequest(String path, String accept) {
        try {
            Request req = new Request.Builder()
                    .url(base + (path.startsWith("/") ? path : "/" + path))
                    .header("Accept", mime(accept))
                    .build();
            return client.newCall(req).execute();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Response deleteRequest(String path) {
        try {
            Request req = new Request.Builder()
                    .url(base + (path.startsWith("/") ? path : "/" + path))
                    .delete()
                    .build();
            return client.newCall(req).execute();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Response postRequest(String path, String contentType, org.json.JSONObject body) {
        return postRaw(path, mime(contentType), body.toString());
    }

    public Response putRequest(String path, String contentType, org.json.JSONObject body) {
        return putRaw(path, mime(contentType), body.toString());
    }

    // Overloads for raw XML/JSON strings if you need them
    public Response postRaw(String path, String contentType, String raw) {
        try {
            RequestBody rb = RequestBody.create(raw, MediaType.parse(contentType));
            Request req = new Request.Builder()
                    .url(base + (path.startsWith("/") ? path : "/" + path))
                    .post(rb).build();
            return client.newCall(req).execute();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Response putRaw(String path, String contentType, String raw) {
        try {
            RequestBody rb = RequestBody.create(raw, MediaType.parse(contentType));
            Request req = new Request.Builder()
                    .url(base + (path.startsWith("/") ? path : "/" + path))
                    .put(rb).build();
            return client.newCall(req).execute();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String mime(String shortName) {
        if ("json".equalsIgnoreCase(shortName)) return "application/json";
        if ("xml".equalsIgnoreCase(shortName))  return "application/xml";
        return shortName; // pass-through
    }
}
