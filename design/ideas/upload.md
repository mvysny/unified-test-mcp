# Upload

The one component no click can drive: it wants a file from the user's disk.

## Today

test-app's `upload` view: an Upload keeping files in memory; once all are in, the status names
them.

```
- Upload [ref=1]
  - Button "Upload Files..." [ref=2] actions: click
  - Span "Drop files here" [ref=3]
  - UploadIcon [ref=4]
  - UploadFileList [ref=5]
- Span "Nothing received" [ref=6]
```

`click` on "Upload Files..." would open the browser's file chooser; not tried. `UnifiedUpload`
has `upload(Path)` and `upload(fileName, mimeType, content)`.

## A shape to start from

An `upload` tool: `{ref, path}`, the Upload's ref and a file on the server's machine; the answer
the snapshot, its file list showing how each file went:

```
- Upload [ref=1] actions: upload
  - UploadFile "notes.txt" [ref=7, complete]
```

## Open questions

`Q_file_source`: over stdio the server runs on the agent's machine, so a path is the agent's file.
Over HTTP it may run elsewhere; then the content in the call instead, as text or base64? Both,
`path` or `content` + `name`?

`Q_paths_allowed`: a path lets the agent send any file the server can read to the app. A tester's
own machine, so fine; or only under a directory given at start, as `--upload-dir`?

`Q_file_list`: a file's line once uploaded: name, `complete`, `error="File is too big"`; its
progress? The app's rejections (`maxFileSize`, accepted types) show there.

`Q_several`: more than one file in one call, as one drop of several files?
