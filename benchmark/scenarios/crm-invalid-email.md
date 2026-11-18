## Task

The CRM app runs at {{url}}; log in as `user`, password `password`. Try to add a contact Jane Doe, email `jane.doe@`, at {{company}}, status Imported lead. If the app won't accept it, don't fix it: tell me which field it objects to and how it shows that, and leave nothing saved.

## Done when

The answer names the Email field; no Jane Doe in the database.

## Watch for

- The app shows no text: the EmailField's own check runs before the bean validator's "must be a well-formed email address" and has an empty error message, so the field only turns invalid. A toolset has to show `invalid` without a message. A required field left empty does say "must not be empty".
- Save is disabled while the form is invalid. An agent that clicks it anyway: our `click` waits 10 s for a disabled button (`Q_timeout` in `design/ideas/beyond-buttons.md`).
