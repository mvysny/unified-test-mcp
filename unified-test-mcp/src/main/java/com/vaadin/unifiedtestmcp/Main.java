package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.StdioMCPServer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts the MCP server on stdio, for an MCP client that spawns this process.
 * <pre>
 * unified-test-mcp [--url http://localhost:8080] [--browser-executable /snap/bin/chromium] [--headed]
 * </pre>
 */
public final class Main {
    private static final String USAGE =
            "usage: unified-test-mcp [--url <app url>] [--browser-executable <path>] [--headed]";

    public static void main(String[] args) {
        Map<String, String> settings = new HashMap<>();
        settings.put("vaadin.testing.base-url", "http://localhost:8080");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--url" -> settings.put("vaadin.testing.base-url", value(args, ++i));
                case "--browser-executable" -> settings.put("vaadin.testing.playwright.executable", value(args, ++i));
                case "--headed" -> settings.put("vaadin.testing.playwright.headless", "false");
                default -> exit("unknown argument " + args[i]);
            }
        }
        String version = Main.class.getPackage().getImplementationVersion();
        try (UnifiedBrowser browser = new UnifiedBrowser(settings, Duration.ofSeconds(10))) {
            new StdioMCPServer(UnifiedTestMCP.createHandler(browser, version == null ? "dev" : version))
                    .runStdio(System.in, System.out);
        }
    }

    private static String value(String[] args, int i) {
        if (i >= args.length) {
            exit(args[i - 1] + " needs a value");
        }
        return args[i];
    }

    private static void exit(String message) {
        System.err.println(message);
        System.err.println(USAGE);
        System.exit(2);
    }
}
