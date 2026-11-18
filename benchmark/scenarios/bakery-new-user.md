## Task

The bakery app runs at {{url}}; log in as admin@vaadin.com, password admin. Create an account for the new baker Linus Torvalds, linus@bakery.example, with the password `penguin`. If the app rejects that password, choose one it accepts and tell me which.

## Done when

The user exists with role baker and those names, and the password in the answer matches the stored hash.

## Watch for

- The rule, "need 6 or more chars, mixing digits, lowercase and uppercase letters", shows only as the field's error after an attempt, so no arm can guess it ahead. Role is a ComboBox of lowercase values.
