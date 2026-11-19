package com.vaadin.unifiedtestmcp;

import com.vaadin.testing.unified.spi.AdapterContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AdapterContext} for opening a Unified API page without a Test Runner: one "run" is one MCP session.
 * What the adapter keeps for the run, its {@code Playwright} and {@code Browser}, lives until {@link #close()}:
 *
 * <pre>{@code
 * JUnitFreeContext context = new JUnitFreeContext(Map.of(), Duration.ofSeconds(10));
 * AdapterSession session = new PlaywrightAdapter().open(context);   // launches the browser into the context
 * session.close();                                                  // the tab
 * context.close();                                                  // the browser and Playwright
 * }</pre>
 *
 * No test class, test method or Spring context: the adapter then takes its base URL from the settings.
 */
final class JUnitFreeContext implements AdapterContext, AutoCloseable {
    private static final Logger LOG = Logger.getLogger(JUnitFreeContext.class.getName());

    private final Map<String, String> settings;
    private final Duration timeout;
    private final Map<Class<?>, AutoCloseable> runScoped = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * @param settings the Unified API's settings by key, e.g. {@code vaadin.testing.playwright.headless}
     */
    JUnitFreeContext(Map<String, String> settings, Duration timeout) {
        this.settings = Map.copyOf(settings);
        this.timeout = timeout;
    }

    @Override
    public Optional<String> configuration(String key) {
        return Optional.ofNullable(settings.get(key));
    }

    @Override
    public Duration timeout() {
        return timeout;
    }

    @Override
    public Optional<Class<?>> testClass() {
        return Optional.empty();
    }

    @Override
    public Optional<Method> testMethod() {
        return Optional.empty();
    }

    @Override
    public <T extends AutoCloseable> T runScoped(Class<T> key, Supplier<T> create) {
        synchronized (runScoped) {
            return key.cast(runScoped.computeIfAbsent(key, k -> create.get()));
        }
    }

    @Override
    public Optional<Object> springContext() {
        return Optional.empty();
    }

    /** Closes what {@link #runScoped} made, newest first, as a Test Runner does at the end of a run. */
    @Override
    public void close() {
        List<AutoCloseable> closing;
        synchronized (runScoped) {
            closing = new ArrayList<>(runScoped.values());
            runScoped.clear();
        }
        Collections.reverse(closing);
        for (AutoCloseable closeable : closing) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to close " + closeable, e);
            }
        }
    }
}
