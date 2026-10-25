# Team development without shared Dropbox credentials

Every member can build, register local demo users, develop screens and run unit tests without requesting the project account's email, password or token. Keep Java/XML, the package structure and the existing repository boundary.

## Start on your own computer

1. Clone the repository and use the reviewed branch or merged `main`.
2. Open it in Android Studio and configure your own Android SDK. Leave `DROPBOX_APP_KEY` unset for local demo work; preserve your own ignored `local.properties`.
3. Build/run the app, then create a local demo username and test password. No email is requested. Account explicitly shows when project storage is disconnected.
4. Run `gradlew.bat testDebugUnitTest`. `DropboxRepositoryTest` supplies in-memory HTTP responses to verify the SDK route, response parsing and error propagation without contacting Dropbox. These fixtures are test-only; the application never labels them as a real cloud connection.
5. Implement screen loading/success/empty/error behavior around repository results. Coordinate new repository method signatures before implementing file operations; never import another member's tokens into source or tests.

Verified on 22 October 2026: a separate source copy with no `DROPBOX_APP_KEY` in local configuration or environment passed `assembleDebug testDebugUnitTest` (7 tests, 0 failures/errors). Its generated App key is empty. This proves build/unit-test independence from Dropbox credentials, not live cloud access.

## Integrate the real cloud on the demo device

The presenter connects the dedicated project Dropbox account through Account > Set up project storage. The encrypted credential stays on that device until **Disconnect Dropbox only** or full logout. Full logout clears the demo session and local Dropbox credential; a later demo login needs fresh OAuth. This is shared academic storage while connected, without per-user cloud isolation.

Real Dropbox integration tests need a valid authorization. A member who needs an independent live test can authorize a separate Dropbox test account on their own device; they do not need the project account's login. An App key identifies the application and does not grant data access by itself. Development users must be allowed in the Developer App, and each account grants its own consent. Store any local App key in ignored configuration.

There is no automatic cloud connection on a freshly cloned computer. Encrypted device credentials cannot be copied through Git. Front-end and isolated unit-test work is independent of the project account; live upload/download acceptance must still be performed on an authorized device when those features are implemented.

## Repeat the live account acceptance check

After connecting the project account in the app, run this only against the intended demo emulator/device. Build the packages, install them as updates, and invoke the instrumentation runner directly so Gradle's connected-test cleanup cannot uninstall the authorized app:

```text
gradlew.bat assembleDebug assembleDebugAndroidTest
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -e class vn.edu.usth.dropboxclient.LiveDropboxAccountTest -e liveDropbox true vn.edu.usth.dropboxclient.test/androidx.test.runner.AndroidJUnitRunner
```

`LiveDropboxAccountTest` reads the encrypted device session and calls `DropboxRepository.getCurrentAccount()` against Dropbox. It asserts that the server returns an account ID and display name. It never prints these values or tokens, writes/deletes cloud files, changes demo users, or clears the project connection. Without the explicit `liveDropbox=true` argument it is skipped, so the normal test suite requires no Dropbox account.

Replace `emulator-5554` with the intended serial from `adb devices`. Use matching project configuration and the same debug signing key when updating an installed demo app. Run the ordinary `connectedDebugAndroidTest` suite on a disposable, unconnected emulator: the Gradle device runner was observed uninstalling the app after tests, so it must not be used to preserve a real project connection.

For presentation, refresh Account and explain the route `AccountFragment -> DropboxRepository -> DbxClientV2 -> Dropbox API`; API work runs on a worker thread and the UI updates on the main thread. Distinguish this read-account result from future file operations.
