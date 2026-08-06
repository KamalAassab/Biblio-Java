# Packaging — standalone Windows installer

**Nothing here is needed to run the app.** To use BiblioTech, double-click
`START-DESKTOP-APP.bat` in the project root.

These files build a self-contained Windows distribution — the app plus a bundled
Java runtime — so it can be installed on a machine with no JDK.

| File | Role |
| --- | --- |
| `Launcher.cs` | Small C# shim: reads `.env`, then starts the app with `javaw.exe` so no console window appears. Compiled to `Biblio-Java.exe`. Not part of the Java build. |
| `Biblio-Java.exe` | The compiled shim. |
| `install-user.bat` | Copies a `jpackage` build into `%LOCALAPPDATA%\Programs\Biblio-Java` and prompts once for a `DATABASE_URL`. |

## Status — currently stale

All three expect a `dist/` tree produced by [`jpackage`](https://docs.oracle.com/en/java/javase/17/jpackage/):

```
dist/jpackage/Biblio-Java-Windows-x64/
```

That tree is build output, so it is **not** in the repository and is excluded by
`.gitignore`. There is no script that regenerates it yet — so `Biblio-Java.exe` and
`install-user.bat` will not work from a fresh clone until someone adds a `jpackage`
step to the build.

They are kept here rather than deleted because the C# shim and the installer logic are
still the right starting point for that work.
