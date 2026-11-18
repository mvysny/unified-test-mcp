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
unchanged: the message is in shadow DOM (`R_vaadin_login_dom`); the hidden `<input id=csrf>` is noise; the overlay
isn't named by its title, which shows as a trailing Div, and its description is missing.

## Open questions

`Q_error`: the form's `error` property says there is one; its text from the form's `i18n`, or
read out of the shadow DOM as the user sees it? Its shape on the form's line,
`[error="Incorrect username or password"]`, the text as the app set it.

`Q_overlay_title`: name the LoginOverlay by its title, as a Dialog by its header (`NameFrom.TITLE`),
the title's Div dropped; the description as `helper=`?

`Q_hidden_inputs`: `<input type=hidden>` left out everywhere, not only the login's.
