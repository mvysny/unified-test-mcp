## Task

The CRM app runs at {{url}}; log in as `user`, password `password`. Clean up {{company}}: delete every contact there whose status is Closed (lost). Leave everyone else alone. Tell me how many you deleted.

## Done when

None left at that company with that status, every other contact intact, the answer equals the number.

## Watch for

- No confirm dialog; one row at a time. Every action answers with a snapshot (`D_action_returns_snapshot`), so the cost per action multiplies — the "Saving agent tokens" promise against that rule.
