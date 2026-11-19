package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.TimeoutError;
import org.vaadin.addons.dramafinder.AutoWait;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Waits for a page load an action set going: a login form's POST, a link out of the app, {@code Page.setLocation}.
 * The Unified API's wait after an action asks whether Flow's client is busy, and misses these: the page leaving has
 * nothing pending, often not even its request yet, and the one loading has no UI yet to ask (R_flow_idle_wait).
 *
 * <pre>{@code
 * PageLoads.of(browser).await();   // before reading the page; returns at once unless a page is loading or just loaded
 * }</pre>
 *
 * A load under way shows two ways: the page's request for the next one, and the Navigation API's {@code navigate}
 * event, which fires as the page asks to leave, before the request goes out. The listener for the event goes into a
 * page at its first {@link #await}, a snapshot's; every action in a page comes after one, as it takes a ref from it.
 * A load may also end within the Unified API's own wait, which then misses Flow starting in the new page.
 *
 * <p>Browser-thread-confined.
 */
final class PageLoads {
    private static final Logger LOG = Logger.getLogger(PageLoads.class.getName());
    /** Each session's page's; the sessions' browser threads share it. */
    private static final Map<Page, PageLoads> ALL = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Whether the page asked to leave for another document since the last call, a {@code navigate} event that is
     * neither within the page nor a download; puts the listener in a page without one first.
     */
    private static final String LEAVING = """
            () => {
              if (!('__mcpLeaving' in window)) {
                window.__mcpLeaving = false;
                window.navigation?.addEventListener('navigate', e => {
                  if (!e.destination.sameDocument && e.downloadRequest === null) window.__mcpLeaving = true;
                });
              }
              const leaving = window.__mcpLeaving;
              window.__mcpLeaving = false;
              return leaving;
            }""";

    /**
     * Whether Flow, if the page runs it, has registered its UI's client beside the {@code TypeScript} one that stands
     * for its start (R_flow_idle_wait).
     */
    private static final String UI_STARTED = """
            () => { const clients = window.Vaadin?.Flow?.clients;
                    return !clients || Object.keys(clients).some(id => id !== 'TypeScript'); }""";

    private final Page page;
    /** A load is under way: no {@code load} yet since the page asked for another. */
    private boolean loading;
    /** A page has loaded since the last {@link #await}, maybe in the Unified API's own wait, Flow not yet started. */
    private boolean loaded;

    private PageLoads(Page page) {
        this.page = page;
        // Playwright for Java fires these on the thread of the call it is in: the browser thread
        page.onRequest(request -> {
            if (isPageLoad(request)) {
                loading = true;
            }
        });
        page.onRequestFailed(request -> {
            if (isPageLoad(request)) {
                loading = false;
            }
        });
        page.onDownload(download -> loading = false);
        page.onLoad(p -> {
            loading = false;
            loaded = true;
        });
    }

    static PageLoads of(Page page) {
        return ALL.computeIfAbsent(page, PageLoads::new);
    }

    /**
     * Waits until the page a navigation started has fired {@code load}, then, after any load since the last call,
     * until Flow has started its UI in the page, and is idle.
     *
     * @throws PlaywrightException if Flow stays busy past the timeout
     */
    void await() {
        if (leaving()) {
            loading = true; // the request may not be out yet; the load clears it
        }
        if (loading) {
            try {
                page.waitForCondition(() -> !loading);
            } catch (TimeoutError e) {
                // a navigation the page dropped, or an answer that loads no page (a 204): no event says so
                LOG.log(Level.FINE, "No load after a navigation; the page is at " + page.url(), e);
                loading = false;
            }
        }
        if (!loaded) {
            return;
        }
        loaded = false;
        try {
            page.waitForFunction(UI_STARTED);
        } catch (TimeoutError e) {
            LOG.log(Level.FINE, "Flow started no UI at " + page.url(), e);
        }
        AutoWait.waitForVaadinIdle(page);
    }

    private boolean leaving() {
        try {
            return Boolean.TRUE.equals(page.evaluate(LEAVING));
        } catch (PlaywrightException e) {
            // the page went away under the script: a load is what replaces it
            LOG.log(Level.FINE, "The page was replaced while asked whether it is leaving", e);
            return true;
        }
    }

    private static boolean isPageLoad(Request request) {
        return request.isNavigationRequest() && request.frame().parentFrame() == null;
    }
}
