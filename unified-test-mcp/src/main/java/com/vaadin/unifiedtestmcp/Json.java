package com.vaadin.unifiedtestmcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jspecify.annotations.Nullable;

/**
 * A field's value as JSON, the one notation of {@code get_value}'s answer and the snapshot's {@code value=}:
 *
 * <pre>{@code
 * Json.of("Tom & Jerry")                   // => "Tom & Jerry"
 * Json.of(null)                            // => null
 * Json.of(List.of("Finland", "Sweden"))    // => ["Finland","Sweden"]
 * }</pre>
 *
 * Not TinyMCPServer's {@code MCPProtocol.toJson}: its Gson escapes HTML, {@code "Tom & Jerry"}.
 */
final class Json {
    private Json() {}

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    static String of(@Nullable Object value) {
        return GSON.toJson(value);
    }
}
