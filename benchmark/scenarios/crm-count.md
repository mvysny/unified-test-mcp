## Task

The CRM app runs at {{url}}; log in as `user`, password `password`. How many contacts at {{company}} have the status {{status}}? Answer with the number only.

## Done when

The answer equals the count in the database.

## Watch for

- The filter matches names only, and the Status and Company columns don't sort, so every row has to be read; 200 contacts, more than the grid renders at once. DOM tools have to scroll a virtual grid. The largest gap expected.
