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
    public void snapshotShowsALoginFormsHeadingAndError() throws IOException {
        String page = snapshotOf("login");
        assertEquals("""
                URL: APP/login
                - LoginForm "Log in" [ref=N]
                  - TextField "Username" [ref=N, id=vaadinLoginUsername, value="", required] actions: set_value
                  - PasswordField "Password" [ref=N, id=vaadinLoginPassword, value="", required] actions: set_value
                  - Button "Log in" [ref=N] actions: click
                  - Button "Forgot password" [ref=N] actions: click
                - Button "Sign in" [ref=N] actions: click
                - Button "Log out" [ref=N] actions: click
                - Span "Signed out" [ref=N]
                """, masked(page));
        int form = ref(page, "LoginForm");
        call(client, "set_value", Map.of("ref", ref(page, "TextField \"Username\""), "value", "alice"));
        call(client, "set_value", Map.of("ref", ref(page, "PasswordField"), "value", "wrong"));
        String failed = call(client, "click", ref(ref(page, "Button \"Log in\"")));
        assertTrue(failed.contains("\n- LoginForm \"Log in\" [ref=" + form + ", error \"Incorrect username or"
                + " password: Check that you have entered the correct username and password and try again.\"]\n"),
                failed);
        call(client, "set_value", Map.of("ref", ref(page, "PasswordField"), "value", "secret"));
        String signedIn = call(client, "click", ref(ref(page, "Button \"Log in\"")));
        assertTrue(signedIn.contains("\n- LoginForm \"Log in\" [ref=" + form + "]\n"), "the error is gone: " + signedIn);
        assertTrue(signedIn.contains("\n- Span \"Signed in as alice\" ["), signedIn);
    }

    @Test
    public void clickWaitsForThePageItLoads() throws IOException {
        String page = snapshotOf("login");
        call(client, "set_value", Map.of("ref", ref(page, "TextField \"Username\""), "value", "alice"));
        call(client, "set_value", Map.of("ref", ref(page, "PasswordField"), "value", "secret"));
        call(client, "click", ref(ref(page, "Button \"Log in\"")));
        String loggedOut = call(client, "click", ref(ref(page, "Button \"Log out\"")));
        assertTrue(loggedOut.contains("\n- Span \"Signed out\" ["), "the page loaded anew: " + loggedOut);
    }

    @Test
    public void snapshotNamesALoginOverlayByItsTitle() throws IOException {
        String overlay = call(client, "click", ref(ref(snapshotOf("login"), "Button \"Sign in\"")));
        assertEquals("""
                Clicked ref=N

                URL: APP/login
                Modal: LoginOverlay "Mail" over the page; only the dialog on top is shown: nothing under it takes \
                input
                - LoginOverlay "Mail" [ref=N, description="Your mail, anywhere"]
                  - TextField "Username" [ref=N, id=vaadinLoginUsername, value="", required] actions: set_value
                  - PasswordField "Password" [ref=N, id=vaadinLoginPassword, value="", required] actions: set_value
                  - Button "Log in" [ref=N] actions: click
                  - Button "Forgot password" [ref=N] actions: click
                """, masked(overlay));
        call(client, "set_value", Map.of("ref", ref(overlay, "TextField \"Username\""), "value", "alice"));
        call(client, "set_value", Map.of("ref", ref(overlay, "PasswordField"), "value", "wrong"));
        String failed = call(client, "click", ref(ref(overlay, "Button \"Log in\"")));
        assertTrue(failed.contains("\n- LoginOverlay \"Mail\" [ref=" + ref(overlay, "- LoginOverlay")
                + ", description=\"Your mail, anywhere\", error \"Incorrect username or password: Check that you have"
                + " entered the correct username and password and try again.\"]\n"), failed);
        call(client, "set_value", Map.of("ref", ref(overlay, "PasswordField"), "value", "secret"));
        String signedIn = call(client, "click", ref(ref(overlay, "Button \"Log in\"")));
        assertFalse(signedIn.contains("Modal:"), "the overlay closed: " + signedIn);
        assertTrue(signedIn.contains("\n- Span \"Signed in as alice\" ["), signedIn);
    }

    @Test
    public void snapshotLeavesOutWhatTheUserCantSee() throws IOException {
        String page = snapshotOf("disclosure");
        assertFalse(page.contains("TextField \"Street\""), "a closed Details' content: " + page);
        assertTrue(page.contains("- TextField \"Account\" ["), "an open one's: " + page);
        assertTrue(page.contains("- TextField \"Name\" ["), "an open AccordionPanel's: " + page);
        assertFalse(page.contains("TextField \"Card number\""), "a closed one's: " + page);
        String opened = call(client, "click", ref(ref(page, "\"Shipping\"")));
        assertTrue(opened.contains("- TextField \"Street\" ["), opened);
    }

    @Test
    public void clickClosingTheDrawerAnswersWithoutIt() throws IOException {
        String page = snapshotOf("nav");
        assertTrue(page.contains("SideNavItem \"Inbox\""), page);
        int toggle = ref(page, "DrawerToggle");
        String closed = call(client, "click", ref(toggle));
        assertFalse(closed.contains("SideNav"), "answered once the drawer's transition ended: " + closed);
        assertTrue(closed.contains("\n  - Drawer: closed; click the DrawerToggle, ref=" + toggle + ", to open it\n"),
                closed);
        String opened = call(client, "click", ref(toggle));
        assertTrue(opened.contains("\n  - Drawer:\n    - SideNav [ref="), opened);
    }

    @Test
    public void snapshotMarksTheCurrentAndTheCollapsedNavItems() throws IOException {
        String page = snapshotOf("nav");
        assertEquals("""
                URL: APP/nav
                - AppLayout [ref=N]
                  - Navbar:
                    - DrawerToggle [ref=N, aria-label="Toggle navigation panel"] actions: click
                    - H1 "Mail" [ref=N]
                  - Drawer:
                    - SideNav [ref=N]
                      - SideNavItem "Inbox" [ref=N, path="nav", current] actions: click
                      - SideNavItem "Archive" [ref=N, path="nav/archive", collapsed] actions: click, toggle_expand
                      - SideNavItem "Trash" [ref=N, collapsed, disabled] actions: click, toggle_expand
                  - Content:
                    - Breadcrumbs [ref=N]
                      - BreadcrumbsItem "Home" [ref=N, path=""] actions: click
                      - BreadcrumbsItem "Mail" [ref=N, path="nav"] actions: click
                      - BreadcrumbsItem "Inbox" [ref=N, current] actions: click
                    - H2 "nav" [ref=N]
                """, masked(page));
        String archive = call(client, "click", ref(ref(page, "SideNavItem \"Archive\"")));
        assertEquals("""
                Clicked ref=N

                URL: APP/nav/archive
                - AppLayout [ref=N]
                  - Navbar:
                    - DrawerToggle [ref=N, aria-label="Toggle navigation panel"] actions: click
                    - H1 "Mail" [ref=N]
                  - Drawer:
                    - SideNav [ref=N]
                      - SideNavItem "Inbox" [ref=N, path="nav"] actions: click
                      - SideNavItem "Archive" [ref=N, path="nav/archive", current] actions: click, toggle_expand
                        - SideNavItem "2025" [ref=N, path="nav/archive/2025"] actions: click
                        - SideNavItem "2026" [ref=N, path="nav/archive/2026"] actions: click
                      - SideNavItem "Trash" [ref=N, collapsed, disabled] actions: click, toggle_expand
                  - Content:
                    - Breadcrumbs [ref=N]
                      - BreadcrumbsItem "Home" [ref=N, path=""] actions: click
                      - BreadcrumbsItem "Mail" [ref=N, path="nav"] actions: click
                      - BreadcrumbsItem "Inbox" [ref=N, current] actions: click
                    - H2 "nav/archive" [ref=N]
                """, masked(archive), "a click opens a collapsed item's page, and shows its items");
    }

    @Test
    public void toggleExpandShowsAnItemsItemsWithoutOpeningItsPage() throws IOException {
        String page = snapshotOf("nav");
        int archive = ref(page, "SideNavItem \"Archive\"");
        String expanded = call(client, "toggle_expand", ref(archive));
        assertEquals("""
                Expanded ref=N

                URL: APP/nav
                - AppLayout [ref=N]
                  - Navbar:
                    - DrawerToggle [ref=N, aria-label="Toggle navigation panel"] actions: click
                    - H1 "Mail" [ref=N]
                  - Drawer:
                    - SideNav [ref=N]
                      - SideNavItem "Inbox" [ref=N, path="nav", current] actions: click
                      - SideNavItem "Archive" [ref=N, path="nav/archive"] actions: click, toggle_expand
                        - SideNavItem "2025" [ref=N, path="nav/archive/2025"] actions: click
                        - SideNavItem "2026" [ref=N, path="nav/archive/2026"] actions: click
                      - SideNavItem "Trash" [ref=N, collapsed, disabled] actions: click, toggle_expand
                  - Content:
                    - Breadcrumbs [ref=N]
                      - BreadcrumbsItem "Home" [ref=N, path=""] actions: click
                      - BreadcrumbsItem "Mail" [ref=N, path="nav"] actions: click
                      - BreadcrumbsItem "Inbox" [ref=N, current] actions: click
                    - H2 "nav" [ref=N]
                """, masked(expanded));
        assertEquals("Collapsed ref=" + archive + "\n\n" + page, call(client, "toggle_expand", ref(archive)),
                "back as it was, refs and all");
    }

    @Test
    public void toggleExpandRefusesWhatDoesntExpand() throws IOException {
        String page = snapshotOf("nav");
        assertEquals("ref " + ref(page, "SideNavItem \"Inbox\"") + " is a SideNavItem without items under it:"
                + " nothing to expand. Call click to open its page.",
                error(client, "toggle_expand", ref(ref(page, "SideNavItem \"Inbox\""))));
        assertEquals("ref " + ref(page, "SideNavItem \"Trash\"") + " is a disabled SideNavItem: a user can't expand"
                + " or collapse it.", error(client, "toggle_expand", ref(ref(page, "SideNavItem \"Trash\""))));
        assertEquals("ref " + ref(page, "DrawerToggle") + " is a DrawerToggle: it doesn't expand or collapse."
                + " toggle_expand takes a SideNavItem with items under it; the snapshot lists toggle_expand in its"
                + " actions.", error(client, "toggle_expand", ref(ref(page, "DrawerToggle"))));
    }

    @Test
    public void clickOnAnExpandedNavItemLandsOnItsOwnRow() throws IOException {
        String archive = call(client, "click", ref(ref(snapshotOf("nav"), "SideNavItem \"Archive\"")));
        String again = call(client, "click", ref(ref(archive, "SideNavItem \"Archive\"")));
        assertTrue(masked(again).contains("\nURL: APP/nav/archive\n"), "not on its first item, 2025: " + again);
        String year = call(client, "click", ref(ref(again, "SideNavItem \"2025\"")));
        assertTrue(year.contains("\n        - SideNavItem \"2025\" [ref=" + ref(again, "SideNavItem \"2025\"")
                + ", path=\"nav/archive/2025\", current] actions: click\n"), year);
    }

    @Test
    public void snapshotLeavesOutAClosedMenu() throws IOException {
        String open = call(client, "click", ref(ref(snapshotOf("menus"), "MenuBarItem \"File\"")));
        assertTrue(open.contains("MenuBarItem \"New\""), open);
        String chosen = call(client, "click", ref(ref(open, "MenuBarItem \"New\"")));
        assertFalse(chosen.contains("MenuBarItem \"New\""), chosen);
        assertTrue(chosen.contains("- Span \"New\" ["), chosen);
    }

    @Test
    public void refHiddenSinceTheSnapshotIsRefused() throws IOException, InterruptedException {
        String page = snapshotOf("disclosure");
        int account = ref(page, "TextField \"Account\"");
        String later = call(client, "click", ref(ref(page, "Button \"Close Billing later\"")));
        assertTrue(later.contains("- TextField \"Account\" [ref=" + account + ","), later);
        Thread.sleep(2500); // the app's poll closes Billing
        assertEquals("ref " + account + " (TextField) isn't shown any more: the app hid it, or closed what holds it."
                + " Call snapshot to see the components on the page now.",
                error(client, "set_value", Map.of("ref", account, "value", "x")));
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

    /** test-app's {@code grid} view as the snapshot shows it on arrival. */
    private static final String GRID = """
            URL: APP/grid
            - Grid [ref=N, rows=200, selection=single]
              Header: | Name ⇅ | Age ⇅ | City | Tags | Actions |
              Header: | TextField [ref=N, placeholder="Filter", value=""] | | | | |
              - row 0: Alice Novak | 20 | Turku | Turku \\| 20s | Button "Edit" [ref=N]
              - row 1: Bob Novak | 27 | Brno | Brno \\| 20s | Button "Edit" [ref=N]
              - row 2: Carol Novak | 34 | Oslo | Oslo \\| 30s | Button "Edit" [ref=N]
              - row 3: David Novak | 41 | Lyon | Lyon \\| 40s | Button "Edit" [ref=N]
              - row 4: Eve Novak | 48 | Porto | Porto \\| 40s | Button "Edit" [ref=N]
              - rows 5–199 not shown: get_rows ref=N
              Footer: | 200 people | | | | |
            - Grid [ref=N, rows=2, selection=multi]
              Header: | Checkbox [ref=N, id=selectAllCheckbox] | Department ⇅ | People |
              - row 0: Checkbox [ref=N] | Engineering | 12
              - row 1: Checkbox [ref=N] | Sales | 4
            - Span "Nothing selected" [ref=N]
            """;

    @Test
    public void snapshotShowsAGridAsRows() throws IOException {
        assertEquals(GRID, masked(snapshotOf("grid")),
                "the header group skipped, a Span's text in a cell with its | escaped, the rows in view");
    }

    @Test
    public void clickOnARowSelectsIt() throws IOException {
        int people = ref(snapshotOf("grid"), "rows=200");
        String clicked = call(client, "click", Map.of("ref", people, "row", 1));
        assertTrue(clicked.startsWith("Clicked row 1 of ref=" + people + "\n\n"), clicked);
        assertTrue(clicked.contains("\n  - row 1 [selected]: Bob Novak | 27 | Brno |"), clicked);
        assertTrue(clicked.contains("\n- Span \"Selected Bob Novak\" [ref="), clicked);
        String edited = call(client, "click", ref(ref(clicked, "- row 0: Alice Novak")));
        assertTrue(edited.contains("\n- Span \"Editing Alice Novak\" [ref="), "a row's Edit button: " + edited);
    }

    @Test
    public void getRowsReadsRowsBeyondTheView() throws IOException {
        int people = ref(snapshotOf("grid"), "rows=200");
        String rows = call(client, "get_rows", Map.of("ref", people, "offset", 150, "length", 3));
        assertTrue(masked(rows).startsWith("""
                Rows 150–152 of 200 in ref=N. The grid now shows row 150; the snapshot below has the refs of the rows\
                 in view:
                - row 150: Alice Popescu | 35 | Turku | Turku \\| 30s | Button "Edit"
                - row 151: Bob Popescu | 42 | Brno | Brno \\| 40s | Button "Edit"
                - row 152: Carol Popescu | 49 | Oslo | Oslo \\| 40s | Button "Edit"

                URL: APP/grid
                - Grid [ref=N, rows=200, selection=single]
                  Header: | Name ⇅ | Age ⇅ | City | Tags | Actions |
                  Header: | TextField [ref=N, placeholder="Filter", value=""] | | | | |
                  - row 150: Alice Popescu | 35 | Turku | Turku \\| 30s | Button "Edit" [ref=N]
                """), rows);
        assertTrue(rows.contains("\n  - rows 0–149, 155–199 not shown: get_rows ref=" + people + "\n"), rows);
        String edited = call(client, "click", ref(ref(rows, "  - row 151: Bob Popescu")));
        assertTrue(edited.contains("\n- Span \"Editing Bob Popescu\" [ref="), "row 151's Edit, now in view: " + edited);
    }

    @Test
    public void getRowsReadsEveryRowScrollingAPageAtATime() throws IOException {
        int people = ref(snapshotOf("grid"), "rows=200");
        String all = call(client, "get_rows", Map.of("ref", people, "offset", 0, "length", 200));
        List<String> rows = all.substring(0, all.indexOf("\n\nURL: ")).lines().skip(1).toList();
        assertEquals(200, rows.size(), all);
        for (int i = 0; i < 200; i++) {
            assertTrue(rows.get(i).startsWith("- row " + i + ": "), rows.get(i));
        }
        assertTrue(rows.get(0).startsWith("- row 0: Alice Novak | "), rows.get(0));
        assertTrue(rows.get(100).startsWith("- row 100: Alice Meyer | "), rows.get(100));
        assertTrue(rows.get(199).startsWith("- row 199: Judy Ortiz | "), rows.get(199));
        String tail = call(client, "get_rows", Map.of("ref", people, "offset", 195, "length", 50));
        assertTrue(tail.startsWith("Rows 195–199 of 200 in ref=" + people + ". The grid now shows row 195;"), tail);
        String one = call(client, "get_rows", Map.of("ref", people, "offset", 150, "length", 1));
        assertTrue(one.startsWith("Row 150 of 200 in ref=" + people + ". The grid now shows row 150;"), one);
    }

    @Test
    public void gridRowToolsRefuseWhatIsntThere() throws IOException {
        String page = snapshotOf("grid");
        int people = ref(page, "rows=200");
        int status = ref(page, "Span \"Nothing selected\"");
        assertEquals("ref " + status + " is a Span, not a Grid: get_rows reads a Grid's rows.",
                error(client, "get_rows", Map.of("ref", status, "offset", 0, "length", 5)));
        assertEquals("ref " + people + " has 200 rows, 0–199: no row 200.",
                error(client, "get_rows", Map.of("ref", people, "offset", 200, "length", 5)));
        assertEquals("ref " + status + " is a Span, not a Grid: row picks a row of a Grid. Leave row out to click the"
                + " component itself.", error(client, "click", Map.of("ref", status, "row", 0)));
        assertEquals("ref " + people + " has 200 rows, 0–199: no row 250.",
                error(client, "click", Map.of("ref", people, "row", 250)));
    }

    @Test
    public void snapshotWithARefShowsTheGridInFull() throws IOException {
        String page = snapshotOf("grid");
        int people = ref(page, "rows=200");
        String full = call(client, "snapshot", Map.of("ref", people));
        assertEquals("""
                - Grid [ref=N, rows=200, selection=single]
                  Header: | GridSorter "Name" [ref=N] ⇅ | GridSorter "Age" [ref=N] ⇅ | City | Tags | Actions |
                  Header: | TextField [ref=N, placeholder="Filter", value=""] | | | | |
                  - row 0: Alice Novak | 20 | Turku | Span "Turku \\| 20s" [ref=N] | Button "Edit" [ref=N]
                  - row 1: Bob Novak | 27 | Brno | Span "Brno \\| 20s" [ref=N] | Button "Edit" [ref=N]
                  - row 2: Carol Novak | 34 | Oslo | Span "Oslo \\| 30s" [ref=N] | Button "Edit" [ref=N]
                  - row 3: David Novak | 41 | Lyon | Span "Lyon \\| 40s" [ref=N] | Button "Edit" [ref=N]
                  - row 4: Eve Novak | 48 | Porto | Span "Porto \\| 40s" [ref=N] | Button "Edit" [ref=N]
                  - rows 5–199 not shown: get_rows ref=N
                  Footer: | 200 people | | | | |
                """, masked(full), "every component in a cell named, the page around it left out");
        assertEquals("Turku | 20s", call(client, "get_text", ref(ref(full, "row 0:"))),
                "a ref the full view showed works");
        assertEquals("Nothing selected", call(client, "get_text", ref(ref(page, "Span \"Nothing selected\""))),
                "so does one from the snapshot before it");
    }

    @Test
    public void sortSortsAndTheHeaderMarksTheSortedColumnsInTheirOrder() throws IOException {
        int people = ref(snapshotOf("grid"), "rows=200");
        String byName = call(client, "sort", Map.of("ref", people, "column", "Name", "direction", "ascending"));
        assertTrue(byName.startsWith("Sorted ref=" + people + " by Name, ascending\n\n"), byName);
        assertTrue(byName.contains("\n  Header: | Name ↑ | Age ⇅ | City | Tags | Actions |\n"), byName);
        assertTrue(byName.contains("\n  - row 0: Alice Berg | "), byName);
        String byAge = call(client, "sort", Map.of("ref", people, "column", "Age", "direction", "descending"));
        assertTrue(byAge.contains("\n  Header: | Name ↑2 | Age ↓1 | City | Tags | Actions |\n"),
                "two clicks on Age, and a column sorted anew comes first: " + byAge);
        assertTrue(byAge.contains("\n  - row 0: Carol Berg | 64 |"), byAge);
        String again = call(client, "sort", Map.of("ref", people, "column", "Age", "direction", "descending"));
        assertTrue(again.startsWith("ref=" + people + " was sorted by Age, descending, first already: nothing"
                + " clicked\n\n"), again);
        assertTrue(again.contains("\n  Header: | Name ↑2 | Age ↓1 | City | Tags | Actions |\n"), again);
        String nameFirst = call(client, "sort", Map.of("ref", people, "column", "Name", "direction", "ascending"));
        assertTrue(nameFirst.startsWith("Sorted ref=" + people + " by Name, ascending\n\n"), nameFirst);
        assertTrue(nameFirst.contains("\n  Header: | Name ↑1 | Age ↓2 | City | Tags | Actions |\n"),
                "ascending already but second, clicked round to come first: " + nameFirst);
        assertTrue(nameFirst.contains("\n  - row 0: Alice Berg | "), nameFirst);
        String unsorted = call(client, "sort", Map.of("ref", people, "column", "Age", "direction", "none"));
        assertTrue(unsorted.startsWith("Stopped sorting ref=" + people + " by Age\n\n"), unsorted);
        assertTrue(unsorted.contains("\n  Header: | Name ↑ | Age ⇅ | City | Tags | Actions |\n"), unsorted);
        String stillUnsorted = call(client, "sort", Map.of("ref", people, "column", "Age", "direction", "none"));
        assertTrue(stillUnsorted.startsWith("ref=" + people + " didn't sort by Age: nothing clicked\n\n"),
                stillUnsorted);
    }

    @Test
    public void sortRefusesAColumnThatDoesntSort() throws IOException {
        String page = snapshotOf("grid");
        int people = ref(page, "rows=200");
        assertEquals("ref " + people + " has no column \"City\" that sorts. Those that do: Name, Age; the snapshot"
                + " marks them ⇅, ↑ or ↓.", error(client, "sort", Map.of("ref", people, "column", "City",
                "direction", "ascending")));
        int status = ref(page, "Span \"Nothing selected\"");
        assertEquals("ref " + status + " is a Span, not a Grid: sort sorts a Grid's rows by one of its columns.",
                error(client, "sort", Map.of("ref", status, "column", "Name", "direction", "ascending")));
    }

    @Test
    public void multiSelectShowsItsCheckboxColumn() throws IOException {
        String page = snapshotOf("unloaded-rows");
        String all = call(client, "click", ref(ref(page, "Button \"Select all\"")));
        assertTrue(all.contains("\n- Grid [ref=" + ref(page, "id=everyone") + ", id=everyone, rows=200, selection=multi]\n"
                + "  Header: | Checkbox [ref="), all);
        assertTrue(masked(all).contains("""
                  Header: | Checkbox [ref=N, id=selectAllCheckbox, checked] | Name | Age |
                  - row 0 [selected]: Checkbox [ref=N, checked] | Alice Novak | 20
                """), all);
    }

    @Test
    public void snapshotShowsWhatATemplateRendersInItsShadowRoot() throws IOException {
        String page = snapshotOf("template");
        assertEquals("""
                URL: APP/template
                - <order-view> [ref=N]
                  - H2 "Orders" [ref=N]
                  - <search-bar> [ref=N, id=search]
                    - TextField [ref=N, placeholder="Search", value=""] actions: set_value
                    - Button "New order" [ref=N] actions: click
                  - Grid [ref=N, id=grid, rows=3, selection=single]
                    Header: | Order |
                    - row 0: Grace Hopper Pending 2 × Strawberry Bun
                    - row 1: Alan Turing Ready 1 × Vanilla Cracker
                    - row 2: Ada Lovelace Delivered 1 × Blueberry Cheesecake
                  - Span "No order saved" [ref=N, id=status]
                """, masked(page), "a template in a template, a card's text in its shadow root, a slot's content");
        assertEquals("""
                - <search-bar> [ref=N, id=search]
                  - TextField [ref=N, placeholder="Search", value=""] actions: set_value
                  - Button "New order" [ref=N] actions: click
                """, masked(call(client, "snapshot", ref(ref(page, "<search-bar>")))));
    }

    @Test
    public void setValueInATemplateReachesTheApp() throws IOException {
        String searched = call(client, "set_value",
                Map.of("ref", ref(snapshotOf("template"), "placeholder=\"Search\""), "value", "ada"));
        assertTrue(masked(searched).contains("""
                  - Grid [ref=N, id=grid, rows=1, selection=single]
                    Header: | Order |
                    - row 0: Ada Lovelace Delivered 1 × Blueberry Cheesecake
                """), searched);
    }

    @Test
    public void snapshotShowsAModalInAShadowRootAlone() throws IOException {
        String page = snapshotOf("template");
        String dialog = call(client, "click", ref(ref(page, "Button \"New order\"")));
        assertEquals("""
                Clicked ref=N

                URL: APP/template
                Modal: Dialog "New order" over the page; only the dialog on top is shown: nothing under it takes \
                input
                - Dialog "New order" [ref=N]
                  - <order-editor> [ref=N]
                    - TextField "Customer" [ref=N, id=customer, value=""] actions: set_value
                    - Div [ref=N, id=items]
                      - <order-item-editor> [ref=N]
                        - ComboBox [ref=N, id=products, placeholder="Product", value=null] actions: set_value
                        - IntegerField [ref=N, id=amount, value="1"] actions: set_value
                    - <buttons-bar> [ref=N]
                      - Button "Cancel" [ref=N] actions: click
                      - Button "Save" [ref=N] actions: click
                """, masked(dialog), "the buttons in the bar's slots");
        int search = ref(page, "placeholder=\"Search\"");
        assertTrue(error(client, "set_value", Map.of("ref", search, "value", "ada"))
                .startsWith("ref " + search + " isn't in the latest snapshot, of " + appUrl + "/template, which shows"
                        + " only the modal Dialog \"New order\";"));
    }

    @Test
    public void setValueSaysWhenTheAppReplacedTheField() throws IOException {
        String dialog = call(client, "click", ref(ref(snapshotOf("template"), "Button \"New order\"")));
        int first = ref(dialog, "id=products");
        String picked = call(client, "set_value", Map.of("ref", first, "value", "Strawberry Bun"));
        assertTrue(picked.startsWith("Set ref=" + first + " to \"Strawberry Bun\"; it isn't shown now\n"),
                "found by its id, which the re-created editors repeat: " + picked);
        assertTrue(masked(picked).contains("""
                      - <order-item-editor> [ref=N]
                        - ComboBox [ref=N, id=products, placeholder="Product", value="Strawberry Bun"] actions: set_value
                        - IntegerField [ref=N, id=amount, value="1"] actions: set_value
                      - <order-item-editor> [ref=N]
                        - ComboBox [ref=N, id=products, placeholder="Product", value=null] actions: set_value
                """), "the app re-created the item editors, a new empty one after them: " + picked);

        int second = refs(picked, "id=products").get(1);
        String again = call(client, "set_value", Map.of("ref", second, "value", "Vanilla Cracker"));
        assertTrue(again.startsWith("Set ref=" + second + " to \"Vanilla Cracker\"; it isn't shown now\n"),
                "found by its stamp, gone with the old editor though the Unified API checks the pick there: " + again);
        assertTrue(masked(again).contains("""
                        - ComboBox [ref=N, id=products, placeholder="Product", value="Vanilla Cracker"] actions: set_value
                """), again);
    }

    @Test
    public void refsInTemplatesFindTheirComponents() throws IOException {
        String dialog = call(client, "click", ref(ref(snapshotOf("template"), "Button \"New order\"")));
        call(client, "set_value", Map.of("ref", ref(dialog, "TextField \"Customer\""), "value", "Linus Torvalds"));
        String picked = call(client, "set_value", Map.of("ref", ref(dialog, "id=products"), "value", "Strawberry Bun"));
        List<Integer> products = refs(picked, "id=products");
        call(client, "set_value", Map.of("ref", products.get(0), "value", "Vanilla Cracker"));
        assertEquals("\"Vanilla Cracker\"", call(client, "get_value", ref(products.get(0))));
        assertEquals("null", call(client, "get_value", ref(products.get(1))),
                "the second of two components with one id");
        call(client, "set_value", Map.of("ref", refs(picked, "id=amount").get(0), "value", 3));
        String saved = call(client, "click", ref(ref(picked, "Button \"Save\"")));
        assertTrue(saved.contains("\n    - row 3: Linus Torvalds New 3 × Vanilla Cracker\n"), saved);
        assertEquals("Saved order for Linus Torvalds: 3 × Vanilla Cracker",
                call(client, "get_text", ref(ref(saved, "id=status"))),
                "the page's one id=status, though each card's shadow root has one too");
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

    /** The refs on the snapshot lines containing {@code part}, top to bottom. */
    private static List<Integer> refs(String snapshot, String part) {
        return snapshot.lines().filter(l -> l.contains(part)).map(l -> {
            Matcher m = Pattern.compile("ref=(\\d+)").matcher(l);
            assertTrue(m.find(), l);
            return Integer.parseInt(m.group(1));
        }).toList();
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
