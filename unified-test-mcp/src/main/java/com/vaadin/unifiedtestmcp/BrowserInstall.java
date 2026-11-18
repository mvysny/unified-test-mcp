package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.impl.driver.Driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Puts the one browser this server opens into Playwright's cache before any {@code Playwright} exists, where
 * Playwright left to itself would download every browser it supports, and onto stdout, which on stdio is the protocol
 * (D_browser_install).
 *
 * <pre>{@code
 * BrowserInstall.start(settings);   // at server start: the download runs in the background
 * BrowserInstall.await(settings);   // before a browser opens: blocks until it is in the cache
 * }</pre>
 *
 * From the {@code vaadin.testing.playwright.*} settings: Chromium's headless shell for a headless Chromium, Chromium
 * itself for a headed one, Firefox or WebKit when {@code browser} names it; nothing when {@code executable} is set. A
 * browser already in the cache isn't downloaded again. Playwright's output goes to the log.
 * <p>
 * Once per JVM, as Playwright's driver is: the first call's settings win. A failed install is tried again by the next
 * call.
 * <p>
 * Thread-safe.
 */
final class BrowserInstall {
    private static final Logger LOG = Logger.getLogger(BrowserInstall.class.getName());
    private static final String BROWSER = "vaadin.testing.playwright.browser";
    private static final String HEADLESS = "vaadin.testing.playwright.headless";
    private static final String EXECUTABLE = "vaadin.testing.playwright.executable";
    /** How many of Playwright's last lines a failure quotes. */
    private static final int TAIL_LINES = 10;

    private static CompletableFuture<Void> install; // guarded by BrowserInstall.class

    private BrowserInstall() {}

    /** Starts the install in the background, unless one is running or done. */
    static synchronized CompletableFuture<Void> start(Map<String, String> settings) {
        if (install == null || install.isCompletedExceptionally()) {
            install = CompletableFuture.runAsync(() -> install(settings), task -> {
                Thread thread = new Thread(task, "browser-install");
                thread.setDaemon(true);
                thread.start();
            });
            install.whenComplete((done, e) -> {
                if (e != null) {
                    LOG.log(Level.WARNING, "Couldn't install the browser", e.getCause());
                }
            });
        }
        return install;
    }

    /**
     * Starts the install unless it is running or done, and waits for it.
     *
     * @throws IllegalStateException the install failed; the message quotes Playwright's last lines
     */
    static void await(Map<String, String> settings) {
        try {
            start(settings).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    /** The driver's command line: {@code install --no-progress --only-shell chromium} for a headless Chromium. */
    private static List<String> command(Map<String, String> settings) {
        List<String> command = new ArrayList<>(List.of("install", "--no-progress"));
        switch (settings.getOrDefault(BROWSER, "chromium")) {
            case "firefox" -> command.add("firefox");
            case "webkit" -> command.add("webkit");
            // any other name is Chromium, as in the Unified API's adapter; headless, Playwright runs its headless shell
            default -> command.addAll(List.of(
                    "false".equalsIgnoreCase(settings.get(HEADLESS)) ? "--no-shell" : "--only-shell", "chromium"));
        }
        return command;
    }

    private static void install(Map<String, String> settings) {
        // One driver per JVM, and the first to ask decides whether it downloads every browser; asking here first,
        // every Playwright the Unified API creates after this one shares it, download skipped.
        Driver driver = Driver.ensureDriverInstalled(Map.of(), false);
        if (settings.containsKey(EXECUTABLE)) {
            return;
        }
        List<String> command = command(settings);
        ProcessBuilder builder = driver.createProcessBuilder();
        builder.command().addAll(command);
        builder.redirectErrorStream(true); // never inherited: on stdio, our stdout is the protocol
        Deque<String> tail = new ArrayDeque<>();
        try {
            Process process = builder.start();
            try (BufferedReader output = process.inputReader()) {
                output.lines().forEach(line -> {
                    LOG.info(line);
                    tail.addLast(line);
                    if (tail.size() > TAIL_LINES) {
                        tail.removeFirst();
                    }
                });
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("Couldn't download the browser: Playwright's `"
                        + String.join(" ", command) + "` exited with " + exit + ":\n" + String.join("\n", tail)
                        + "\nTo use an installed Chromium instead, start the server with --browser-executable <path>.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't run Playwright's `" + String.join(" ", command) + "`", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while installing the browser", e);
        }
    }
}
