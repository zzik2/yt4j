package zzik2.yt4j.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Optional;

public final class JsonPath {
    private static final Gson GSON = new Gson();

    private final JsonElement root;

    private JsonPath(JsonElement root) {
        this.root = root;
    }

    public static JsonPath parse(String json) {
        return new JsonPath(GSON.fromJson(json, JsonElement.class));
    }

    public static JsonPath of(JsonElement element) {
        return new JsonPath(element);
    }

    public JsonPath get(String key) {
        if (root == null || !root.isJsonObject()) return empty();
        JsonElement child = root.getAsJsonObject().get(key);
        return child == null ? empty() : new JsonPath(child);
    }

    public JsonPath get(String... keys) {
        JsonPath current = this;
        for (String key : keys) {
            current = current.get(key);
            if (current.isAbsent()) return empty();
        }
        return current;
    }

    public JsonPath index(int i) {
        if (root == null || !root.isJsonArray()) return empty();
        JsonArray arr = root.getAsJsonArray();
        if (i < 0 || i >= arr.size()) return empty();
        return new JsonPath(arr.get(i));
    }

    public Optional<String> asString() {
        if (root == null || !root.isJsonPrimitive()) return Optional.empty();
        return Optional.of(root.getAsString());
    }

    public Optional<Long> asLong() {
        if (root == null || !root.isJsonPrimitive())
            return Optional.empty();
        try {
            return Optional.of(root.getAsLong());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<Integer> asInt() {
        return asLong().map(Long::intValue);
    }

    public Optional<Boolean> asBoolean() {
        if (root == null || !root.isJsonPrimitive()) return Optional.empty();
        return Optional.of(root.getAsBoolean());
    }

    public int arraySize() {
        if (root == null || !root.isJsonArray()) return 0;
        return root.getAsJsonArray().size();
    }

    public JsonPath arrayItem(int i) {
        return index(i);
    }

    public boolean isAbsent() {
        return root == null;
    }

    public boolean isPresent() {
        return root != null;
    }

    public boolean isObject() {
        return root != null && root.isJsonObject();
    }

    public boolean isArray() {
        return root != null && root.isJsonArray();
    }

    public JsonElement element() {
        return root;
    }

    public <T> T as(Class<T> type) {
        return GSON.fromJson(root, type);
    }

    private static JsonPath empty() {
        return new JsonPath(null);
    }
}
