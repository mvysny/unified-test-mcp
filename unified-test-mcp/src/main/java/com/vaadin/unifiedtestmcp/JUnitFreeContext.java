package com.vaadin.unifiedtestmcp;

import com.vaadin.testing.unified.spi.AdapterContext;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AdapterContext} for opening a Unified API page without JUnit: the adapter SPI hands an adapter a JUnit
 * {@link ExtensionContext}, and the Playwright adapter keeps its {@code Playwright} and {@code Browser} in that
 * context's root store. Here the root store is a plain map, and {@link #close()} closes what the adapter put there,
 * as JUnit would at the end of a run.
 * <p>
 * Of {@link ExtensionContext} only {@code getRoot()} and {@code getStore(..)} answer; any other method throws, naming
 * itself, so an adapter upgrade that starts calling one fails loudly.
 */
final class JUnitFreeContext implements AdapterContext, AutoCloseable {
    private static final Logger LOG = Logger.getLogger(JUnitFreeContext.class.getName());

    private final Map<String, String> settings;
    private final Duration timeout;
    private final RootStore store = new RootStore();
    private final ExtensionContext extensionContext = fakeExtensionContext(store);

    /**
     * @param settings the Unified API's settings by key, e.g. {@code vaadin.testing.base-url}
     */
    JUnitFreeContext(Map<String, String> settings, Duration timeout) {
        this.settings = Map.copyOf(settings);
        this.timeout = timeout;
    }

    @Override
    public ExtensionContext extensionContext() {
        return extensionContext;
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
    public void close() {
        store.close();
    }

    private static ExtensionContext fakeExtensionContext(ExtensionContext.Store store) {
        ExtensionContext[] self = new ExtensionContext[1];
        self[0] = (ExtensionContext) Proxy.newProxyInstance(JUnitFreeContext.class.getClassLoader(),
                new Class<?>[] { ExtensionContext.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoot" -> self[0];
                    case "getStore" -> store;
                    case "toString" -> "JUnitFreeContext";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("ExtensionContext." + method.getName()
                            + " is not available outside JUnit; only getRoot() and getStore(..) are");
                });
        return self[0];
    }

    /** One map for every namespace: the adapter uses only the global one. */
    private static final class RootStore implements ExtensionContext.Store, AutoCloseable {
        private final Map<Object, Object> values = Collections.synchronizedMap(new LinkedHashMap<>());

        @Override
        public Object get(Object key) {
            return values.get(key);
        }

        @Override
        public <V> V get(Object key, Class<V> requiredType) {
            return requiredType.cast(values.get(key));
        }

        @Override
        @Deprecated
        public <K, V> Object getOrComputeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator) {
            return computeIfAbsent(key, defaultCreator);
        }

        @Override
        public <K, V> Object computeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator) {
            synchronized (values) {
                return values.computeIfAbsent(key, k -> defaultCreator.apply(key));
            }
        }

        @Override
        @Deprecated
        public <K, V> V getOrComputeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator,
                Class<V> requiredType) {
            return computeIfAbsent(key, defaultCreator, requiredType);
        }

        @Override
        public <K, V> V computeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator,
                Class<V> requiredType) {
            return requiredType.cast(computeIfAbsent(key, defaultCreator));
        }

        @Override
        public void put(Object key, Object value) {
            values.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return values.remove(key);
        }

        @Override
        public <V> V remove(Object key, Class<V> requiredType) {
            return requiredType.cast(values.remove(key));
        }

        /** Closes every {@link AutoCloseable} value, newest first, as JUnit closes a store. */
        @Override
        public void close() {
            List<Object> closing;
            synchronized (values) {
                closing = new ArrayList<>(values.values());
                values.clear();
            }
            Collections.reverse(closing);
            for (Object value : closing) {
                if (value instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Failed to close " + value, e);
                    }
                }
            }
        }
    }
}
