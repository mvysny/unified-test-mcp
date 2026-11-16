# Releasing

How a maintainer cuts a release. The one artifact is `unified-test-mcp-<version>.jar`, every
dependency inside, published only on this repository's GitHub releases page — never on Maven
Central (`D_single_jar_release`). `master` always carries the next version with a `-SNAPSHOT`
suffix; the steps use `1.0` → `1.1-SNAPSHOT`.

1. **Start clean:** on `master`, `git status` empty, pulled, and CI green on that commit. No
   dependency in `gradle/libs.versions.toml` is a `-SNAPSHOT`.
2. **Set the release version:** `build.gradle.kts`: `version = "1.0"`.
3. **Build everything:** `./gradlew` on any JDK 21 or newer; `--release 21` makes the jar the
   same either way. It must be green; a red build stops the release.
4. **Smoke-test the jar itself** — the tests run the classes, not the merged jar. Start
   `./gradlew :test-app:run`, then in a scratch folder:
   ```bash
   claude mcp add vaadin-test -- java -jar /path/to/unified-test-mcp/build/libs/unified-test-mcp-1.0.jar
   ```
   Start `claude` there, ask it to click the button `clicker` twice and read its text:
   `Clicked 2 times`. Then `claude mcp remove vaadin-test` and stop the app.
5. **Commit and tag:** `git commit -am "Release 1.0"`, then `git tag 1.0`.
6. **Open the next version:** `build.gradle.kts`: `version = "1.1-SNAPSHOT"`, then
   `git commit -am "Prepare 1.1-SNAPSHOT"`.
7. **Push:** `git push origin master 1.0`.
8. **Publish** the jar built in step 3 — step 6 did not rebuild it. Write the notes from
   `git log --oneline <previous tag>..1.0`: what a user of the tools gets, and that it needs
   Java 21 or newer.
   ```bash
   gh release create 1.0 unified-test-mcp/build/libs/unified-test-mcp-1.0.jar \
       --title 1.0 --notes-file notes.md
   ```
