package com.vaadin.unifiedtestmcp;

import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.internal.TestRun;
import com.vaadin.testing.unified.internal.UnifiedPageImpl;
import com.vaadin.testing.unified.playwright.PlaywrightAdapter;
import com.vaadin.testing.unified.spi.AdapterSession;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One browser tab, driven through the Unified API's Playwright adapter:
 *
 * <pre>{@code
 * try (UnifiedBrowser browser = new UnifiedBrowser(Map.of(), Duration.ofSeconds(10))) {
 *     browser.call(page -> { page.navigate("http://localhost:8080"); return null; });
 *     String text = browser.call(page -> page.find(UnifiedButton.class).withId("clicker").getText());
 * }
 * }</pre>
 *
 * The browser starts on the first {@link #call}, on an empty page: it goes nowhere until a call navigates. That call
 * first waits for {@link BrowserInstall} to put the browser in Playwright's cache.
 *
 * <p>Thread-safe.
 *
 * <h2>Implementation detail</h2>
 * Every call runs on one thread of its own, whichever thread asks: Playwright for Java is not thread-safe.
 */
public final class UnifiedBrowser implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(UnifiedBrowser.class.getName());

    private final Map<String, String> settings;
    private final JUnitFreeContext context;
    private final ExecutorService browserThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "unified-browser");
        t.setDaemon(true);
        return t;
    });

    // touched on browserThread only
    private AdapterSession session;
    private UnifiedPage page;

    /**
     * @param settings the Unified API's settings by key: the {@code vaadin.testing.playwright.*} keys pick and
     *                 launch the browser
     * @param timeout  how long a lookup or an action waits before it fails
     */
    public UnifiedBrowser(Map<String, String> settings, Duration timeout) {
        this.settings = Map.copyOf(settings);
        context = new JUnitFreeContext(settings, timeout);
    }

    /**
     * Runs {@code action} on the page, one call at a time, and returns its result; what it throws is rethrown as is.
     */
    public <T> T call(Function<UnifiedPage, T> action) {
        Future<T> result = browserThread.submit(() -> action.apply(page()));
        try {
            return result.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException r) {
                throw r;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new IllegalStateException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the browser", e);
        }
    }

    private UnifiedPage page() {
        if (page == null) {
            BrowserInstall.await(settings);
            session = new PlaywrightAdapter().open(context);
            page = new UnifiedPageImpl(new TestRun(TestingTool.PLAYWRIGHT, session, context.timeout()));
        }
        return page;
    }

    /** Closes the tab, then the browser and Playwright. */
    @Override
    public void close() {
        Future<?> closing = browserThread.submit(() -> {
            try {
                if (session != null) {
                    session.close();
                }
            } finally {
                context.close();
            }
        });
        try {
            closing.get();
        } catch (ExecutionException e) {
            LOG.log(Level.WARNING, "Failed to close the browser", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Interrupted while closing the browser", e);
        } finally {
            browserThread.shutdown();
        }
    }
}
