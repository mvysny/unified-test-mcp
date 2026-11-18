# Login: LoginForm and LoginOverlay

Mostly works already: its fields and buttons are ordinary ones. What's missing is the error and
the overlay's title.

## Today

test-app's `login` view: a LoginForm and a Sign in button opening a LoginOverlay "Mail"; either
takes any username with the password `secret`.

```
- LoginForm [ref=1]
  - <input> [ref=2, id=csrf]
  - TextField "Username" [ref=3, id=vaadinLoginUsername, value="", required] actions: set_value
  - PasswordField "Password" [ref=4, id=vaadinLoginPassword, value="", required] actions: set_value
  - Button "Log in" [ref=5] actions: click
  - Button "Forgot password" [ref=6] actions: click
- Button "Sign in" [ref=7] actions: click
- Span "Signed out" [ref=8]
```

Works already: `set_value` on both fields, `click` on Log in. The LoginOverlay shows as a modal,
its `aria-modal` (`D_modal_shows_alone`):

```
Modal: LoginOverlay over the page; only the dialog on top is shown: nothing under it takes input
- LoginOverlay [ref=9]
  - <input> [ref=10, id=csrf]
  - TextField "Username" [ref=11, …]
  …
  - Div "Mail" [ref=15, id=title-vaadin-login-overlay-34]
```

Doesn't: with a wrong password, the form's error doesn't show, the snapshot answering the click
unchanged: the message is in shadow DOM (`R_vaadin_login_dom`); the hidden `<input id=csrf>` is
noise, left out with every hidden input by `snapshot-cross-cutting.md`'s `Q_visibility`; the
overlay isn't named by its title, which shows as a trailing Div, and its description is missing;
the form's own heading, "Log in", is missing too.

## Open questions

`Q_error`: the form's `error` property says there is one; its text from the form's `i18n`, or
read out of the shadow DOM as the user sees it? Lean: `i18n.errorMessage`, the very text the
shadow DOM renders (`R_vaadin_login_dom`), read from the component as a field's `errorMessage`
is; the snapshot walks the light DOM only. Its shape a field's `invalid "…"`, the state in Flow's
word (`setError`), then its message: `[error "Incorrect username or password: Check that you have
entered the correct username and password and try again."]`. Title and message both, as the user
sees them: no tool reads the message otherwise, and an app may put the substance there, "Account
locked for 15 minutes"; it costs tokens only while the error shows.

`Q_overlay_title`: name the LoginOverlay by its title, as a Dialog by its header (`NameFrom.TITLE`),
the title's Div dropped; the description as `helper=`? Lean: yes, but the name read from the
dropped title element's text where there's no `aria-label`: the overlay has `aria-labelledby`
instead (`R_vaadin_login_dom`), so `NameFrom.TITLE` reading `aria-label`, as today, would drop the
Div and name nothing, on its line and on the `Modal:` line. A Dialog keeps its name: its
`aria-label` is its title's text. The LoginForm the same, `LoginForm "Log in"`, from the
`i18n.form.title` it shows as its heading. The description as `description="Your mail, anywhere"`,
Flow's word, from the property, uncut as a name is; not `helper=`, the text below a field, whose
`…` points at `get_helper`, which refuses anything but a field.
