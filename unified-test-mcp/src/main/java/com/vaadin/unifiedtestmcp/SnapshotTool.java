package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code snapshot()}: the Vaadin components on the page, one line each, in the format {@link Snapshot} shows.
 * Its refs become the ones every other tool takes.
 */
final class SnapshotTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("snapshot",
            "The page's URL and title, then the Vaadin components on it, one line each, indented by nesting: "
                    + "`- Button \"Save\" [ref=3, id=save] actions: click`. The quoted name is what a user reads: a "
                    + "Button's or Span's text, a field's or Checkbox's label. A component with neither has no quoted "
                    + "name, its bracket showing what tells it apart instead: `- Button [ref=5, aria-label=\"Close\", "
                    + "icon=\"vaadin:close\"]`, an icon-only Button; else placeholder=. The bracket also shows the id "
                    + "when the app set one (a Java test finds the component by it), the name of the icon in front "
                    + "of the name, a field's value (a Slider's with its "
                    + "min and max), the hints helper= (the text below a field) and tooltip=, each only when the "
                    + "component has one and cut at 120 characters (get_helper and get_tooltip read them uncut), and "
                    + "the states that are on: checked, "
                    + "disabled, readonly, required, invalid with its error message. `actions` lists "
                    + "the tools made for that type. A component the app has hidden isn't listed, nor anything inside "
                    + "it, nor a dialog that isn't open. While a modal dialog is open, it alone is listed, below a "
                    + "`Modal:` line naming what it's over: nothing under it takes input until it closes. Every other tool names a component by its `ref` from here; "
                    + "a ref stays valid as long as the latest snapshot shows it.",
            new InputSchemaBuilder().build());

    /** The {@code ref} parameter, as every tool that acts on a component declares it. */
    static final String REF_DESCRIPTION = "The component's ref, from the latest snapshot";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        return MCPProtocol.Content.text(ToolCall.get(Snapshot::take));
    }
}
