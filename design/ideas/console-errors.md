# Console errors in the snapshot

A tester wants to know when the page broke in a way the screen doesn't show: a JavaScript error, a
failed request, Flow's client logging an exception. The snapshot's header (URL, title) could carry
one more line:

```
URL: http://localhost:8080/form
Title: Form
Console: 2 new errors since the last snapshot
```

Possible: Playwright for Java has `Page.onConsoleMessage` (each `console.*` call, with its
`type()`: `error`, `warning`, …) and `Page.onPageError` (an uncaught exception). Both fire on the
browser thread while a Playwright call runs, so `UnifiedBrowser` registers them when it opens
the page and counts. A snapshot reports the count since the previous one and resets it.
The line is left out when there are none, as the title's is.

## Open questions

`Q_what_counts`: `console.error` and uncaught exceptions surely; warnings, failed network
requests (`onRequestFailed`, 4xx/5xx responses)? Flow's own dev-mode noise could drown the signal.

`Q_reading_them`: a count says "something happened"; the agent then wants the messages. A
`console_messages` tool (what the Playwright MCP has), or the snapshot prints the new errors'
first lines when there are few?

`Q_server_side`: an exception in a click listener shows on the server's log, not the browser
console; in dev mode Flow shows it in a notification, which the snapshot may already list. Worth
checking what a failing listener looks like through the tools today.
