package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code snapshot()}: the Vaadin components on the page, one line each, in the format {@link Snapshot} shows.
 * Its refs become the ones every other tool takes. {@code snapshot(ref)}: that component alone, in its full view
 * ({@link Snapshot#full}); its refs join the latest ones.
 */
final class SnapshotTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("snapshot",
            "The page's URL and title, then the Vaadin components on it, one line each, indented by nesting: "
                    + "`- Button \"Save\" [ref=3, id=save] actions: click`. The quoted name is what a user reads: a "
                    + "Button's or Span's text, a field's or Checkbox's label. A component with neither has no quoted "
                    + "name, its bracket showing what tells it apart instead: `- Button [ref=5, aria-label=\"Close\", "
                    + "icon=\"vaadin:close\"]`, an icon-only Button; else placeholder=. The bracket also shows the id "
                    + "when the app set one (a Java test finds the component by it), the name of the icon in front "
                    + "of the name, path= where a SideNavItem or BreadcrumbsItem links, a field's value (a Slider's "
                    + "with its "
                    + "min and max), the hints helper= (the text below a field) and tooltip=, each only when the "
                    + "component has one and cut at 120 characters (get_helper and get_tooltip read them uncut), and "
                    + "the states that are on: checked, "
                    + "disabled, readonly, required, invalid with its error message; a LoginForm's error with its "
                    + "message; current, the SideNavItem or BreadcrumbsItem of the page shown; collapsed, a "
                    + "SideNavItem that has items, hidden (toggle_expand shows them, click opens its page). A "
                    + "dialog's quoted name is its title. An AppLayout lists what it holds under `- Navbar:`, `- Drawer:` and `- Content:`, the "
                    + "app's chrome apart from the view; a closed drawer as `- Drawer: closed`, with the ref of the "
                    + "DrawerToggle that opens it. A Grid lists how many rows= it has and its selection= single or "
                    + "multi, then a `Header:` line per header row, `| Name ↑ | Age ⇅ | "
                    + "Actions |` (↑ or ↓ marks a sorted column, with its order when several are, ⇅ one that sorts: "
                    + "the sort tool sorts it), up to 10 of the "
                    + "rows in view, `- row 3 [selected]: Frank Rossi | 20 | Button \"Edit\" [ref=7]`, a line naming "
                    + "the rows not shown (get_rows reads them), and a `Footer:` line. Rows are numbered from 0 and "
                    + "take no ref: click and get_rows take the Grid's ref and the row's number. In a cell, a "
                    + "component a user acts on shows with its ref, anything else as its text, parts joined by `; `, "
                    + "a `|` inside a cell written `\\|`. `actions` lists "
                    + "the tools made for that type, and toggle_expand on a SideNavItem with items. A component the "
                    + "user can't see isn't listed: one the app hid, "
                    + "the content of a closed dialog, Details, menu or drawer. While a modal dialog is open, it alone "
                    + "is listed, below a `Modal:` line naming what it's over: nothing under it takes input until it "
                    + "closes. Every other tool names a component by its `ref` from here; a ref stays valid as long as "
                    + "the latest snapshot shows it and the user still sees it. With a ref: that component alone, "
                    + "in full: a Grid's every row in view, every component in its cells with its ref; the refs it "
                    + "shows join the latest snapshot's.",
            new InputSchemaBuilder().optionalInteger("ref",
                    "A component's ref, from the latest snapshot: shows it alone, in full. Leave out for the page")
                    .build());

    /** The {@code ref} parameter, as every tool that acts on a component declares it. */
    static final String REF_DESCRIPTION = "The component's ref, from the latest snapshot";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        Integer ref = request.arguments().getIntOrNull("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) ->
                ref == null ? Snapshot.take(page, refs) : Snapshot.full(page, refs, ref)));
    }
}
