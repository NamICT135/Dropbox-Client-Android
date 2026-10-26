# Team development without shared Dropbox credentials

This six-member Java/XML project has two separate sessions. A local username and password control entry to the app on one device. A Dropbox account is authorized once on that device for real API calls; local users there share the same App Folder. The public App key identifies the app but grants no file access by itself.

## On a teammate's computer

1. Clone `main` and open the repository root in Android Studio.
2. Let Android Studio create the ignored `local.properties` for that computer's SDK. Local sign-in, UI work and unit tests build without `DROPBOX_APP_KEY`.
3. Run `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`. The repository tests use in-memory responses; they do not require a Dropbox login.
4. For real API checks, add the public App key to your own ignored `local.properties`, then authorize an allowed Dropbox test account on your own device. Do not ask for the project's password or copy its encrypted credential.
5. Work on a feature branch, keep changes focused, push and open a PR into `main`. Review the diff and run the relevant checks before merging.

The project connection remains after local logout and is removed only through **Account > Disconnect project storage**. A newly cloned device has no connection until it completes OAuth. Local accounts do not provide private cloud storage or per-user isolation.

## Live checks

The existing opt-in device tests call the real Dropbox API only when explicitly enabled. Use an authorized test device and keep disposable files inside the app's Scoped App Folder. Never publish credentials, account IDs or personal files in logs, screenshots, code or chat.

The Android Gradle connected-test runner previously uninstalled the app after tests. To preserve an authorized device's session, install updated APKs with `adb install -r` and invoke only the intended instrumentation class directly. Use an unconnected emulator for the ordinary UI suite.
