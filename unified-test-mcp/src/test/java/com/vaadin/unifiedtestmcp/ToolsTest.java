package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.HttpMCPServer;
import com.github.mvysny.tinymcpserver.MCPHandler;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.github.mvysny.tinymcpserver.client.TinyMCPClient;
import com.github.mvysny.vaadinboot.VaadinBoot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tools end to end: test-app in this JVM, a real browser on it, the tools called over MCP's HTTP transport.
 * {@link #client} is one MCP session, with one browser, for the whole class.
 */
public class ToolsTest {
    private static VaadinBoot app;
    private static MCPHandler handler;
    private static HttpMCPServer server;
    private static TinyMCPClient client;
    /** test-app's URL, {@code http://localhost:<port>}. */
    private static String appUrl;

    @BeforeAll
    public static void start() throws Exception {
        int appPort = freePort();
        app = new VaadinBoot().withPort(appPort).localhostOnly().openBrowserInDevMode(false);
        app.start();
        appUrl = "http://localhost:" + appPort;
        handler = UnifiedTestMCP.createHandler(Map.of(), Duration.ofSeconds(10), "test");
        server = new HttpMCPServer(0, "/mcp", handler);
        server.start();
        client = connect();
    }

    @AfterAll
    public static void stop() throws IOException {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
        if (app != null) {
            app.stop("test done");
        }
    }

    @BeforeEach
    public void startOnMainView() throws IOException {
        navigate(client, "");
    }

    @Test
    public void snapshotShowsEachKindOfName() throws IOException {
        assertEquals("""
                Navigated to APP/form

                URL: APP/form
                Title: Form
                - TextField "Name" [ref=N, id=name, value="Alice", tooltip="Your full name, as on your ID card"] \
                actions: set_value
                - TextField "Email" [ref=N, value="", helper="We never share it", required] actions: set_value
                - Checkbox "Subscribe" [ref=N, helper="One email a month: what changed in the app, what is coming \
                next, and how to get the most out of it. Unsubscribe at any t…", checked] actions: set_value
                - Button "Save" [ref=N] actions: click
                - Button [ref=N, id=close, aria-label="Close", icon="vaadin:close", tooltip="Close without saving"] \
                actions: click
                - Button "Delete" [ref=N, disabled] actions: click
                - Span "Not saved" [ref=N]
                """, masked(navigate(client, "form")));
        assertEquals(masked(call(client, "snapshot", Map.of())), masked(navigate(client, "form")).substring(
                "Navigated to APP/form\n\n".length()));
    }

    @Test
    public void snapshotShowsTheIconInFrontOfTheName() throws IOException {
        assertEquals("""
                URL: APP/icons
                - Button "Search" [ref=N, icon="vaadin:search"] actions: click
                - Button "Next" [ref=N] actions: click
                - Button [ref=N, aria-label="Dismiss", icon="lumo:cross"] actions: click
                - TextField "Filter" [ref=N, icon="vaadin:search", value=""] actions: set_value
                - Button "Home" [ref=N, icon="home"] actions: click
                - Button "Profile" [ref=N, icon="fa-solid fa-user"] actions: click
                - Button "Star" [ref=N] actions: click
                - Button "Logo" [ref=N] actions: click
                """, masked(snapshotOf("icons")));
    }

    @Test
    public void snapshotShowsOnlyTheModalOnTop() throws IOException {
        String page = snapshotOf("dialog");
        assertEquals("""
                URL: APP/dialog
                - Button "Delete" [ref=N] actions: click
                - Button "Notes" [ref=N] actions: click
                - Button "Discard" [ref=N] actions: click
                - Span "Not deleted" [ref=N]
                """, masked(page), "a closed dialog isn't shown");
        String delete = call(client, "click", ref(ref(page, "Button \"Delete\"")));
        assertEquals("""
                Clicked ref=N

                URL: APP/dialog
                Modal: Dialog "Delete order?" over the page; only the dialog on top is shown: nothing under it takes \
                input
                - Dialog "Delete order?" [ref=N]
                  - TextField "Reason" [ref=N, value=""] actions: set_value
                  - Button "Why?" [ref=N] actions: click
                  - Button "Cancel" [ref=N] actions: click
                  - Button "Delete" [ref=N] actions: click
                """, masked(delete));
        String why = call(client, "click", ref(ref(delete, "Button \"Why?\"")));
        assertEquals("""
                Clicked ref=N

                URL: APP/dialog
                Modal: Dialog "Why?" over Dialog "Delete order?" over the page; only the dialog on top is shown: \
                nothing under it takes input
                - Dialog "Why?" [ref=N]
                  - Span "Deleting can't be undone." [ref=N]
                  - Button "OK" [ref=N] actions: click
                """, masked(why));
        String back = call(client, "click", ref(ref(why, "Button \"OK\"")));
        assertEquals(ref(delete, "Button \"Cancel\""), ref(back, "Button \"Cancel\""), "a component keeps its ref");
        String closed = call(client, "click", ref(ref(back, "Button \"Cancel\"")));
        assertEquals(page, closed.substring(closed.indexOf("URL: ")), "the page comes back with its refs");
    }

    @Test
    public void refBehindAModalIsRefused() throws IOException {
        String page = snapshotOf("dialog");
        int discard = ref(page, "Button \"Discard\"");
        String confirm = call(client, "click", ref(discard));
        assertEquals("""
                Clicked ref=N

                URL: APP/dialog
                Modal: ConfirmDialog "Discard changes?" over the page; only the dialog on top is shown: nothing under \
                it takes input
                - ConfirmDialog "Discard changes?" [ref=N]
                  - Div "Unsaved changes will be lost." [ref=N]
                  - Button "Cancel" [ref=N] actions: click
                  - Button "Discard" [ref=N] actions: click
                """, masked(confirm));
        assertEquals("ref " + discard + " isn't in the latest snapshot, of " + appUrl + "/dialog, which shows only"
                + " the modal ConfirmDialog \"Discard changes?\"; its refs are " + ref(confirm, "- ConfirmDialog")
                + "-" + ref(confirm, "Button \"Discard\"") + ". What's under the dialog takes no input until it"
                + " closes: close it with one of its buttons, and the page's components come back with their refs.",
                error(client, "click", ref(discard)));
        String discarded = call(client, "click", ref(ref(confirm, "Button \"Discard\"")));
        assertTrue(discarded.contains("\n- Span \"Discarded\" ["), discarded);
    }

    @Test
    public void snapshotShowsAModelessDialogWithThePage() throws IOException {
        String notes = call(client, "click", ref(ref(navigate(client, "dialog"), "Button \"Notes\"")));
        assertEquals("""
                Clicked ref=N

                URL: APP/dialog
                - Button "Delete" [ref=N] actions: click
                - Button "Notes" [ref=N] actions: click
                - Button "Discard" [ref=N] actions: click
                - Span "Not deleted" [ref=N]
                - Dialog "Notes" [ref=N]
                  - Span "Remember to call back." [ref=N]
                  - Button "Close" [ref=N] actions: click
                """, masked(notes));
    }

    @Test
    public void clickWithoutIdFindsTheStampedComponent() throws IOException {
        int save = ref(navigate(client, "form"), "Button \"Save\"");
        String after = call(client, "click", ref(save));
        assertTrue(after.startsWith("Clicked ref=" + save + "\n\nURL: " + appUrl + "/form\n"), after);
        assertTrue(after.contains("- Span \"Saved\" ["), after);
        assertEquals(save, ref(after, "Button \"Save\""), "a component keeps its ref");
    }

    @Test
    public void clickChangesText() throws IOException {
        int clicker = ref(call(client, "snapshot", Map.of()), "id=clicker");
        assertEquals("Click me", call(client, "get_text", ref(clicker)));
        String after = call(client, "click", ref(clicker));
        assertTrue(after.contains("- Button \"Clicked 1 times\" [ref=" + clicker + ", id=clicker]"), after);
        call(client, "click", ref(clicker));
        assertEquals("Clicked 2 times", call(client, "get_text", ref(clicker)));
    }

    @Test
    public void getValueReadsFieldsAndCheckbox() throws IOException {
        String form = navigate(client, "form");
        assertEquals("\"Alice\"", call(client, "get_value", ref(ref(form, "id=name"))));
        assertEquals("\"\"", call(client, "get_value", ref(ref(form, "TextField \"Email\""))));
        assertEquals("true", call(client, "get_value", ref(ref(form, "Checkbox \"Subscribe\""))));
    }

    @Test
    public void snapshotShowsSelectedItemsByTheirLabels() throws IOException {
        assertEquals("""
                Navigated to APP/choices

                URL: APP/choices
                - ComboBox "Country" [ref=N, id=country, value="Czechia"] actions: set_value
                - MultiSelectComboBox "Visited" [ref=N, value=["Finland","Sweden"]] actions: set_value
                - RadioButtonGroup "Home" [ref=N, value="Czechia"] actions: set_value
                - Select "Favourite" [ref=N, value="Sweden"] actions: set_value
                - ComboBox "Next" [ref=N, value=null] actions: set_value
                - ComboBox "Dream" [ref=N, value=null] actions: set_value
                """, masked(navigate(client, "choices")));
    }

    @Test
    public void getValueReadsSelectedItemsByTheirLabels() throws IOException {
        String choices = navigate(client, "choices");
        assertEquals("\"Czechia\"", call(client, "get_value", ref(ref(choices, "id=country"))));
        assertEquals("[\"Finland\",\"Sweden\"]",
                call(client, "get_value", ref(ref(choices, "MultiSelectComboBox \"Visited\""))));
        assertEquals("\"Czechia\"", call(client, "get_value", ref(ref(choices, "RadioButtonGroup \"Home\""))));
        assertEquals("\"Sweden\"", call(client, "get_value", ref(ref(choices, "Select \"Favourite\""))));
        assertEquals("null", call(client, "get_value", ref(ref(choices, "ComboBox \"Next\""))));
    }

    /** test-app's {@code fields} view as the snapshot shows it, enabled and editable; the URL left out. */
    private static final String FIELDS = """
            - VerticalLayout [ref=N, id=fields]
              - TextField "Text" [ref=N, value="Alice"] actions: set_value
              - TextArea "Notes" [ref=N, value="Line one\\nLine two"] actions: set_value
              - PasswordField "Password" [ref=N, value="secret"] actions: set_value
              - EmailField "Email" [ref=N, value="alice@example.com"] actions: set_value
              - IntegerField "Age" [ref=N, value="42"] actions: set_value
              - NumberField "Weight" [ref=N, value="72.5"] actions: set_value
              - BigDecimalField "Price" [ref=N, value="9,90"] actions: set_value
              - DatePicker "Born" [ref=N, value="31.1.2000"] actions: set_value
              - TimePicker "Alarm" [ref=N, value="7.30"] actions: set_value
              - DateTimePicker "Meeting" [ref=N, value=["17.11.2026","10.00"]] actions: set_value
              - Checkbox "Agree" [ref=N, checked] actions: set_value
              - Switch "Dark mode" [ref=N, checked] actions: set_value
              - CheckboxGroup "Toppings" [ref=N, value=["Cheese","Olives"]] actions: set_value
              - ListBox [ref=N, aria-label="Size", value="Medium"] actions: set_value
              - MultiSelectListBox [ref=N, aria-label="Days", value=["Mon","Wed"]] actions: set_value
              - Slider "Volume" [ref=N, value=30, min=0, max=100] actions: set_value
              - Slider "Ratio" [ref=N, value=0.5, min=0, max=1] actions: set_value
              - RangeSlider "Hours" [ref=N, value=[8,17], min=0, max=24] actions: set_value
              - CustomField "Phone" [ref=N]
                - TextField "Prefix" [ref=N, value="+420"] actions: set_value
                - TextField "Number" [ref=N, value="123456"] actions: set_value
            """;

    @Test
    public void snapshotShowsEveryField() throws IOException {
        assertEquals("URL: APP/fields\n" + FIELDS, masked(snapshotOf("fields")));
    }

    @Test
    public void snapshotShowsDisabledFields() throws IOException {
        assertEquals("URL: APP/fields/disabled\n" + flagged(FIELDS, "disabled", 1, 2),
                masked(snapshotOf("fields/disabled")));
        assertEquals("URL: APP/fields/disabled-parent\n" + flagged(FIELDS, "disabled", 0, 2),
                masked(snapshotOf("fields/disabled-parent")), "Flow disables what is inside a disabled layout");
    }

    @Test
    public void snapshotShowsReadOnlyFields() throws IOException {
        assertEquals("URL: APP/fields/readonly\n" + flagged(FIELDS, "readonly", 1, 2),
                masked(snapshotOf("fields/readonly")), "test-app's read-only CustomField makes its fields read-only");
    }

    @Test
    public void getValueReadsEveryField() throws IOException {
        String fields = snapshotOf("fields");
        assertEquals("\"Line one\\nLine two\"", call(client, "get_value", ref(ref(fields, "TextArea"))));
        assertEquals("\"42\"", call(client, "get_value", ref(ref(fields, "IntegerField"))));
        assertEquals("\"31.1.2000\"", call(client, "get_value", ref(ref(fields, "DatePicker"))),
                "what the user sees in a fi app, not ISO");
        assertEquals("[\"17.11.2026\",\"10.00\"]", call(client, "get_value", ref(ref(fields, "DateTimePicker"))));
        assertEquals("true", call(client, "get_value", ref(ref(fields, "Switch"))));
        assertEquals("[\"Cheese\",\"Olives\"]", call(client, "get_value", ref(ref(fields, "CheckboxGroup"))));
        assertEquals("\"Medium\"", call(client, "get_value", ref(ref(fields, "aria-label=\"Size\""))));
        assertEquals("[\"Mon\",\"Wed\"]", call(client, "get_value", ref(ref(fields, "MultiSelectListBox"))));
        assertEquals("30", call(client, "get_value", ref(ref(fields, "Slider \"Volume\""))));
        assertEquals("0.5", call(client, "get_value", ref(ref(fields, "Slider \"Ratio\""))));
        assertEquals("[8,17]", call(client, "get_value", ref(ref(fields, "RangeSlider"))));
        assertEquals("\"+420\"", call(client, "get_value", ref(ref(fields, "TextField \"Prefix\""))));
        int phone = ref(fields, "CustomField");
        assertEquals("ref " + phone + " is a CustomField: its value is in the fields inside it. Call get_value on"
                + " each; the snapshot shows them under it.", error(client, "get_value", ref(phone)));
        String disabled = snapshotOf("fields/disabled");
        assertEquals("\"Medium\"", call(client, "get_value", ref(ref(disabled, "aria-label=\"Size\""))),
                "a disabled ListBox's item loses its selected attribute; the index stays");
        assertEquals("[\"Mon\",\"Wed\"]", call(client, "get_value", ref(ref(disabled, "MultiSelectListBox"))));
    }

    @Test
    public void getHelperReadsItUncut() throws IOException {
        String form = navigate(client, "form");
        assertEquals("One email a month: what changed in the app, what is coming next, and how to get the most out"
                + " of it. Unsubscribe at any time, from the link at the bottom of every email.",
                call(client, "get_helper", ref(ref(form, "Checkbox \"Subscribe\""))));
        assertEquals("We never share it", call(client, "get_helper", ref(ref(form, "TextField \"Email\""))));
        assertEquals("(no helper text)", call(client, "get_helper", ref(ref(form, "id=name"))));
        int save = ref(form, "Button \"Save\"");
        assertEquals("ref " + save + " is a Button: only a field has a helper text. A Button's hint is its tooltip:"
                + " tooltip= in the snapshot, or get_tooltip.", error(client, "get_helper", ref(save)));
    }

    @Test
    public void getTooltipReadsItWithoutHovering() throws IOException {
        String form = navigate(client, "form");
        assertEquals("Close without saving", call(client, "get_tooltip", ref(ref(form, "id=close"))));
        assertEquals("Your full name, as on your ID card", call(client, "get_tooltip", ref(ref(form, "id=name"))));
        assertEquals("(no tooltip)", call(client, "get_tooltip", ref(ref(form, "Button \"Save\""))));
    }

    @Test
    public void getTextRefusesWhatHoldsAValue() throws IOException {
        String form = navigate(client, "form");
        int name = ref(form, "id=name");
        assertEquals("ref " + name + " is a TextField: it has a value, not a text. Call get_value; its label is its"
                + " name in the snapshot.", error(client, "get_text", ref(name)));
        int subscribe = ref(form, "Checkbox \"Subscribe\"");
        assertEquals("ref " + subscribe + " is a Checkbox: it has a value, not a text. Call get_value; its label is"
                + " its name in the snapshot.", error(client, "get_text", ref(subscribe)));
        assertEquals("(no text)", call(client, "get_text", ref(ref(form, "id=close"))), "an icon-only Button");
    }

    @Test
    public void getValueRefusesWhatHoldsAText() throws IOException {
        String form = navigate(client, "form");
        int save = ref(form, "Button \"Save\"");
        assertEquals("ref " + save + " is a Button: it has no value. Call get_text for its text.",
                error(client, "get_value", ref(save)));
        int status = ref(form, "Span \"Not saved\"");
        assertEquals("ref " + status + " is a Span: it has no value. Call get_text for its text.",
                error(client, "get_value", ref(status)));
    }

    @Test
    public void setValueTypesAndTheAppHearsIt() throws IOException {
        int name = ref(navigate(client, "edit"), "TextField \"Name\"");
        String after = call(client, "set_value", Map.of("ref", name, "value", "Bob"));
        assertTrue(after.startsWith("Set ref=" + name + " to \"Bob\"\n\nURL: " + appUrl + "/edit\n"), after);
        assertTrue(after.contains("- TextField \"Name\" [ref=" + name + ", value=\"Bob\"]"), after);
        assertTrue(after.contains("- Span \"Hello, Bob\" ["), "the app's listener ran: " + after);
    }

    @Test
    public void setValueSaysWhatTheAppPutBack() throws IOException {
        int username = ref(navigate(client, "edit"), "TextField \"Username\"");
        String after = call(client, "set_value", Map.of("ref", username, "value", "admin"));
        assertTrue(after.startsWith("Set ref=" + username + " to \"admin\"; it holds \"alice\" now, without showing invalid\n\n"), after);
    }

    @Test
    public void setValueSaysTheFieldShowsInvalid() throws IOException {
        int age = ref(navigate(client, "edit"), "IntegerField \"Age\"");
        String after = call(client, "set_value", Map.of("ref", age, "value", 130));
        assertTrue(after.startsWith("Set ref=" + age + " to \"130\"; it holds it but shows invalid \"At most 120\"\n\n"), after);
    }

    @Test
    public void setValueSetsEveryField() throws IOException {
        String fields = snapshotOf("fields");
        assertSets(fields, "TextField \"Text\"", "Bob", "\"Bob\"");
        assertSets(fields, "TextArea", "one\ntwo", "\"one\\ntwo\"");
        assertSets(fields, "PasswordField", "hunter2", "\"hunter2\"");
        assertSets(fields, "EmailField", "bob@example.com", "\"bob@example.com\"");
        assertSets(fields, "IntegerField", 7, "\"7\"");
        assertSets(fields, "NumberField", 1.5, "\"1.5\"");
        assertSets(fields, "BigDecimalField", "1,25", "\"1,25\"");
        assertSets(fields, "BigDecimalField", 2.5, "\"2,5\"");
        assertSets(fields, "DatePicker", "3.2.2001", "\"3.2.2001\"");
        assertSets(fields, "TimePicker \"Alarm\"", "8.15", "\"8.15\"");
        assertSets(fields, "DateTimePicker", List.of("2.1.2027", "3.04"), "[\"2.1.2027\",\"3.04\"]");
        assertSets(fields, "Checkbox \"Agree\"", false, "false");
        assertSets(fields, "Switch", false, "false");
        assertSets(fields, "CheckboxGroup", List.of("Ham"), "[\"Ham\"]");
        assertSets(fields, "aria-label=\"Size\"", "Large", "\"Large\"");
        assertSets(fields, "MultiSelectListBox", List.of("Tue"), "[\"Tue\"]");
        assertSets(fields, "Slider \"Volume\"", 55, "55");
        assertSets(fields, "Slider \"Ratio\"", 0.3, "0.3");
        assertSets(fields, "RangeSlider", List.of(18, 20), "[18,20]");
        assertSets(fields, "TextField \"Prefix\"", "+421", "\"+421\"");
        assertSets(fields, "TextField \"Text\"", "", "\"\"");
    }

    @Test
    public void setValueSaysTheFieldCantReadWhatWasTyped() throws IOException {
        String fields = snapshotOf("fields");
        int born = ref(fields, "DatePicker");
        String after = call(client, "set_value", Map.of("ref", born, "value", "dsdds"));
        assertTrue(after.startsWith("Set ref=" + born + " to \"dsdds\"; it can't read that as a date, so holds none,"
                + " and shows invalid"), after);
        assertTrue(after.contains("- DatePicker \"Born\" [ref=" + born + ", value=\"dsdds\", invalid"), after);
        int meeting = ref(fields, "DateTimePicker");
        String half = call(client, "set_value", Map.of("ref", meeting, "value", List.of("2.1.2027", "")));
        assertTrue(half.startsWith("Set ref=" + meeting + " to [\"2.1.2027\",\"\"]; it can't read that as a date and"
                + " time, so holds none"), half);
    }

    @Test
    public void setValueSaysHowTheFieldShowsWhatWasTyped() throws IOException {
        int born = ref(snapshotOf("fields"), "DatePicker");
        String after = call(client, "set_value", Map.of("ref", born, "value", "03.02.2001"));
        assertTrue(after.startsWith("Set ref=" + born + " to \"03.02.2001\"; it holds \"3.2.2001\" now, without"
                + " showing invalid\n\n"), after);
    }

    @Test
    public void setValueSetsSelections() throws IOException {
        String choices = navigate(client, "choices");
        assertSets(choices, "id=country", "Finland", "\"Finland\"");
        assertSets(choices, "MultiSelectComboBox", List.of("Sweden", "Czechia"), "[\"Sweden\",\"Czechia\"]");
        assertSets(choices, "MultiSelectComboBox", List.of(), "[]");
        assertSets(choices, "RadioButtonGroup", "Sweden", "\"Sweden\"");
        assertSets(choices, "Select \"Favourite\"", "Finland", "\"Finland\"");
        assertSets(choices, "ComboBox \"Next\"", "Czechia", "\"Czechia\"");
        assertSets(choices, "ComboBox \"Dream\"", "Norway", "\"Norway\"");
        int next = ref(choices, "ComboBox \"Next\"");
        String cleared = call(client, "set_value", ref(next));
        assertTrue(cleared.startsWith("Set ref=" + next + " to null\n\n"), cleared);
        assertEquals("null", call(client, "get_value", ref(next)));
        String snapshot = call(client, "snapshot", Map.of());
        assertFalse(snapshot.contains("- Div"), "Vaadin's screen-reader announcement: " + snapshot);
    }

    @Test
    public void setValueRefusesWhatAUserCantDo() throws IOException {
        String form = navigate(client, "form");
        int save = ref(form, "Button \"Save\"");
        assertEquals("ref " + save + " is a Button: it has no value to set. Call click to press it.",
                error(client, "set_value", Map.of("ref", save, "value", "x")));
        int subscribe = ref(form, "Checkbox \"Subscribe\"");
        assertEquals("ref " + subscribe + " is a Checkbox: it takes true or false; not \"yes\".",
                error(client, "set_value", Map.of("ref", subscribe, "value", "yes")));

        String fields = snapshotOf("fields");
        int phone = ref(fields, "CustomField");
        assertEquals("ref " + phone + " is a CustomField: its value is in the fields inside it. Call set_value on"
                + " each; the snapshot shows them under it.", error(client, "set_value", Map.of("ref", phone)));
        int age = ref(fields, "IntegerField");
        assertEquals("ref " + age + " is a IntegerField: it takes a number, 42; not \"abc\".",
                error(client, "set_value", Map.of("ref", age, "value", "abc")));
        int meeting = ref(fields, "DateTimePicker");
        assertEquals("ref " + meeting + " is a DateTimePicker: it takes its date and its time as two texts, as get_value"
                + " shows them, [\"17.11.2026\",\"10.00\"]; not \"2027-01-02T03:04\".",
                error(client, "set_value", Map.of("ref", meeting, "value", "2027-01-02T03:04")));
        int volume = ref(fields, "Slider \"Volume\"");
        assertEquals("ref " + volume + " is a Slider from 0 to 100: 150 is outside it.",
                error(client, "set_value", Map.of("ref", volume, "value", 150)));
        int toppings = ref(fields, "CheckboxGroup");
        assertEquals("ref " + toppings + " is a CheckboxGroup without the item \"Salami\". Its items: \"Cheese\","
                + " \"Ham\", \"Olives\".", error(client, "set_value",
                Map.of("ref", toppings, "value", List.of("Ham", "Salami"))));
        assertEquals("[\"Cheese\",\"Olives\"]", call(client, "get_value", ref(toppings)), "nothing touched");

        String choices = navigate(client, "choices");
        int favourite = ref(choices, "Select \"Favourite\"");
        assertEquals("ref " + favourite + " is a Select without the item \"Norway\". Its items: \"Finland\","
                + " \"Czechia\", \"Sweden\".", error(client, "set_value", Map.of("ref", favourite, "value", "Norway")));
        int country = ref(choices, "id=country");
        assertEquals("ref " + country + " is a ComboBox without the item \"null\". Its items: \"Finland\","
                + " \"Czechia\", \"Sweden\". To empty it, leave value out.",
                error(client, "set_value", Map.of("ref", country, "value", "null")));
        int home = ref(choices, "RadioButtonGroup");
        assertEquals("ref " + home + " is a RadioButtonGroup: no user can clear it, only pick another of its items:"
                + " \"Finland\", \"Czechia\", \"Sweden\".", error(client, "set_value", ref(home)));
    }

    @Test
    public void setValueRefusesADisabledOrReadOnlyField() throws IOException {
        int disabled = ref(snapshotOf("fields/disabled"), "TextField \"Text\"");
        assertEquals("ref " + disabled + " is a disabled TextField: a user can't change it.",
                error(client, "set_value", Map.of("ref", disabled, "value", "Bob")));
        int readOnly = ref(snapshotOf("fields/readonly"), "TextField \"Text\"");
        assertEquals("ref " + readOnly + " is a read-only TextField: a user can't change it.",
                error(client, "set_value", Map.of("ref", readOnly, "value", "Bob")));
    }

    @Test
    public void navigateOpensView() throws IOException {
        String second = navigate(client, "second");
        assertTrue(masked(second).startsWith("Navigated to APP/second\n\nURL: APP/second\n"), second);
        String main = call(client, "click", ref(ref(second, "id=back")));
        assertTrue(main.contains("\nURL: " + appUrl + "/\n- Button \"Click me\" [ref="), main);
    }

    @Test
    public void navigateToMissingRouteSaysSo() throws IOException {
        String orders = navigate(client, "orders");
        assertTrue(masked(orders).startsWith("No view at APP/orders: the app shows Vaadin's \"Could not navigate\""
                + " page\n\nURL: APP/orders\n"), orders);
        assertTrue(masked(navigate(client, "form")).startsWith("Navigated to APP/form\n"),
                "a view's text doesn't trip it");
    }

    @Test
    public void navigateStartsViewAfresh() throws IOException {
        int before = ref(call(client, "snapshot", Map.of()), "id=clicker");
        call(client, "click", ref(before));
        String reloaded = navigate(client, "");
        assertTrue(reloaded.contains("- Button \"Click me\" ["), reloaded);
        assertNotEquals(before, ref(reloaded, "id=clicker"), "a new page's components get new numbers");
    }

    @Test
    public void refFromBeforeNavigationIsRejected() throws IOException {
        int clicker = ref(call(client, "snapshot", Map.of()), "id=clicker");
        int back = ref(navigate(client, "second"), "id=back");
        assertEquals("ref " + clicker + " isn't in the latest snapshot, of " + appUrl + "/second; its refs are "
                + back + ". A ref from another page needs that page again: navigate to it. Else call snapshot to see"
                + " the components on this page now.", error(client, "get_text", ref(clicker)));
    }

    @Test
    public void refBeforeAnySnapshotIsRejected() throws IOException {
        try (TinyMCPClient other = connect()) {
            assertEquals("No refs yet: call snapshot if a page is open, or navigate to the app's URL; then use a ref"
                    + " from the snapshot either returns.",
                    error(other, "click", ref(1)));
        }
    }

    @Test
    public void browserStartsOnNoPage() throws IOException {
        try (TinyMCPClient other = connect()) {
            assertEquals("No page open yet: call navigate with the app's URL.\n", call(other, "snapshot", Map.of()));
        }
    }

    @Test
    public void navigateWithoutSchemeIsRejected() throws IOException {
        assertEquals("navigate takes a full URL, with its http:// or https://; the browser is at " + appUrl + "/.",
                error(client, "navigate", Map.of("url", "form")));
        try (TinyMCPClient other = connect()) {
            assertEquals("navigate takes a full URL, with its http:// or https://, as http://localhost:8080/orders.",
                    error(other, "navigate", Map.of("url", "form")));
        }
    }

    @Test
    public void navigateToNoServerSaysSo() throws IOException {
        String url = "http://localhost:" + freePort() + "/";
        String message = error(client, "navigate", Map.of("url", url));
        assertEquals("Can't open " + url + ": is the app running there? net::ERR_CONNECTION_REFUSED at " + url,
                message);
        assertTrue(masked(navigate(client, "")).startsWith("Navigated to APP/\n"), "the next navigate works");
    }

    @Test
    public void eachSessionHasItsOwnBrowser() throws IOException {
        int clicker = ref(call(client, "snapshot", Map.of()), "id=clicker");
        call(client, "click", ref(clicker));
        try (TinyMCPClient other = connect()) {
            int otherClicker = ref(navigate(other, ""), "id=clicker");
            call(other, "click", ref(otherClicker));
            String twice = call(other, "click", ref(otherClicker));
            assertTrue(twice.contains("\"Clicked 2 times\""), twice);
        }
        assertEquals("Clicked 1 times", call(client, "get_text", ref(clicker)));
        assertEquals(1, handler.getSessionCount());
    }

    private static TinyMCPClient connect() throws IOException {
        TinyMCPClient connected = new TinyMCPClient(URI.create(server.getUrl()));
        connected.initialize();
        return connected;
    }

    private static String call(TinyMCPClient session, String tool, Map<String, Object> arguments) throws IOException {
        MCPProtocol.CallToolResult result = session.callTool(request(tool, arguments));
        String text = result.getContent().get(0).getText();
        assertTrue(result.getIsError() == null || !result.getIsError(), text);
        return text;
    }

    /** Navigates to {@code path} in test-app: {@code "form"}, or {@code ""} for the start view. */
    private static String navigate(TinyMCPClient session, String path) throws IOException {
        return call(session, "navigate", Map.of("url", appUrl + "/" + path));
    }

    /** Navigates to {@code path} and answers the snapshot alone, without {@code navigate}'s echo line. */
    private static String snapshotOf(String path) throws IOException {
        String navigated = navigate(client, path);
        return navigated.substring(navigated.indexOf("URL: "));
    }

    /** {@code flag} added to the bracket of each line nested {@code fromDepth} to {@code toDepth} deep. */
    private static String flagged(String snapshot, String flag, int fromDepth, int toDepth) {
        return snapshot.lines().map(line -> {
            int depth = line.indexOf("- ") / 2;
            return depth >= fromDepth && depth <= toDepth
                    ? line.replaceFirst("](?=( actions: .*)?$)", ", " + flag + "]") : line;
        }).collect(Collectors.joining("\n", "", "\n"));
    }

    private static String error(TinyMCPClient session, String tool, Map<String, Object> arguments)
            throws IOException {
        MCPProtocol.CallToolResult result = session.callTool(request(tool, arguments));
        assertEquals(Boolean.TRUE, result.getIsError(), result.getContent().get(0).getText());
        return result.getContent().get(0).getText();
    }

    /**
     * {@code set_value} on the one snapshot line containing {@code part} answers that it set {@code value}, nothing
     * more, and {@code get_value} then reads {@code json}.
     */
    private static void assertSets(String snapshot, String part, Object value, String json) throws IOException {
        int ref = ref(snapshot, part);
        String after = call(client, "set_value", Map.of("ref", ref, "value", value));
        assertTrue(after.startsWith("Set ref=" + ref + " to " + json + "\n\n"), part + ": " + after);
        assertEquals(json, call(client, "get_value", ref(ref)), part);
    }

    private static Map<String, Object> ref(int ref) {
        return Map.of("ref", ref);
    }

    /** The ref on the one snapshot line containing {@code part}. */
    private static int ref(String snapshot, String part) {
        List<String> lines = snapshot.lines().filter(l -> l.contains(part)).toList();
        assertEquals(1, lines.size(), "one line with " + part + " in\n" + snapshot);
        Matcher m = Pattern.compile("ref=(\\d+)").matcher(lines.get(0));
        assertTrue(m.find(), lines.get(0));
        return Integer.parseInt(m.group(1));
    }

    /** Refs as {@code ref=N}, their numbers depending on what ran before; test-app's URL as {@code APP}. */
    private static String masked(String snapshot) {
        return snapshot.replaceAll("ref=\\d+", "ref=N").replace(appUrl, "APP");
    }

    private static ToolRequest request(String tool, Map<String, Object> arguments) {
        return new ToolRequest(tool, arguments, Map.of(), null);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
