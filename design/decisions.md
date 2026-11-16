# Decisions

Why this project is the way it is and not otherwise — FAQ-shaped: each entry is a question and
its current answer. Rewrite the answer when it changes; delete the entry when nobody asks any
more. An entry is earned by what it would cost to reverse — half the code base — or by research
the next person would otherwise redo (cited as its `R_`). Not an entry: windows → panels
"because that's the trend", this red over that red, `get_foo` over `is_foo?`, the testing library,
the CI host, a version bump — a comment at the site of the choice, or nothing; nothing about
`design/` itself. Cite by slug, `D_<slug>`, never by position; `grep '^## D_' design/decisions.md`
is the index. An entry is as long as its question needs: every road not taken keeps all of its
losing reasons. When you have written an entry, re-read it against the one above for consistency,
and open the doc comments it touches and delete what they already say.

---

## D_unified_api_backend — Why drive the browser through the Unified API rather than Playwright or TestBench directly?

The tools speak Vaadin components (**Vaadin components, not DOM**), and the Unified API already
has that vocabulary: a typed wrapper per Flow component (`UnifiedButton`, `UnifiedTextField`,
`UnifiedGrid`, …), lookups by label, text, accessible name or id, and actions that wait until
the component is enabled and the server round-trip is done. On Playwright it runs through
Dramafinder, so it needs no license, and the Playwright session stays reachable through its
escape hatch for whatever the wrappers lack. Why not Playwright directly, or the Playwright MCP
the agent already has: it sees DOM and ARIA, so how each Vaadin component is built (shadow DOM,
overlays, a Grid's lazily loaded and recycled rows) would be ours to reverse-engineer, and to
follow on every Vaadin release; the Unified API and Dramafinder maintain that. Why not
TestBench: it drives Selenium only and needs a license; we revisit it once TestBench can drive
Playwright. The cost we carry: the Unified API is a 0.1.0 alpha whose only entry point is a
JUnit extension (`R_unified_junit_bound`), and it has no query that lists the components on a
page (`R_unified_no_enumeration`), so a snapshot cannot come from it alone.
