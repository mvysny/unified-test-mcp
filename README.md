# Unified Test MCP

An MCP (Model Context Protocol) server that lets an AI agent test a running Vaadin Flow app in
a real browser, in terms of Vaadin components rather than DOM nodes: `Button "Save"`, a text
field by its label, a Grid's rows. The component knowledge comes from the Unified API for
Vaadin UI tests running on Playwright; this project owns the MCP tools on top of it.

## Status

Design only, no code yet. The first prototype (click a Button by its id, read its text) is
sketched in [design/ideas/first-tools.md](design/ideas/first-tools.md).
