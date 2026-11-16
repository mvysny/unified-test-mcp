package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPHandler;
import com.github.mvysny.tinymcpserver.StdioMCPServer;

/**
 * Starts the MCP server on stdio, for an MCP client that spawns this process. No tools yet.
 */
public final class Main {
    public static void main(String[] args) {
        new StdioMCPServer(new MCPHandler()).runStdio(System.in, System.out);
    }
}
