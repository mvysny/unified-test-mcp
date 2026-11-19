# sort

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open the page at
`grid`. Its first table lists people. Put them oldest first: who is the oldest, and does anyone
share that age? Then order the table by age, oldest first, and within one age by name from A to
Z, and tell me the first four people. Can I order the table by City? Last, stop ordering it by
age, and tell me who is first then.

## Done when

- Oldest: 64, four people share it: Carol Berg, Heidi Jansen, Carol Costa, Heidi Moreau.
- By age, then name: `Age ↓1`, `Name ↑2` in the header; Carol Berg, Carol Costa, Heidi Jansen,
  Heidi Moreau.
- City doesn't sort: no `⇅` on it.
- Age no longer sorted, `Name ↑` left: Alice Berg first.

## Watch for

- Does it read `⇅` as "sorts", and find `sort` from it?
- Age, then name: a column sorted anew comes first, so name has to be sorted before age. Does it
  read that from the description, or sort name second and read `Name ↑1 | Age ↓2` as wrong, and
  fix it? How many calls?
- City: answered from the header line, or by a failed `sort`? Does the error, naming the columns
  that sort, send it on in one step?
- Stopping: does it find `none`, or sort ascending and call that stopped?
