# Upload/download acceptance

The debug build provides **Account > Test upload / download**. It uses the existing local demo session and the separately authorized project Dropbox connection. Release builds do not expose this debug entry. The regular file-browser UI remains separate work.

## Permissions and setup

1. Enable `account_info.read`, `files.metadata.read`, `files.content.read`, and `files.content.write` in the Developer App and save the change. Do not add `files.metadata.write` for ordinary create/rename/delete operations; those endpoints use `files.content.write`.
2. Keep the existing Scoped App Folder access type. Paths such as `/test_upload_demo.txt` are relative to the app folder, not the entire Dropbox account. Configure the public App key in ignored `local.properties`; never commit secrets, passwords or tokens.
3. Run the debug app and log into a local demo user. Use **Account > Set up project storage > Connect project Dropbox**, or the transfer screen's **Connect / renew Dropbox permissions** button. A group member signs into the dedicated project account and grants the updated permissions. Old access/refresh tokens do not automatically gain new scopes; demo logout does not renew Dropbox permissions or disconnect project storage.
4. The app verifies the account before saving its encrypted credential. All local demo users on that device share this connection. Other team members do not need the project's email/password for front-end work and isolated tests.

## Manual transfer check

1. Open **Account > Test upload / download** and upload a small disposable local file using the Android document picker.
2. Check that the screen reports the server's filename, actual path and byte size. Verify the file exists in the project App Folder. Upload uses ADD with automatic renaming to avoid silently overwriting an existing cloud file.
3. The path input is automatically updated with the upload response, including any name changed by Dropbox. Choose **Download from Dropbox** and select a new document destination. Compare downloaded contents with the original.
4. Cancel each picker and verify that the screen is usable again. Try an invalid path, an unavailable file and an offline connection. Renew project permissions if Dropbox rejects a previously granted scope. Error messages do not print authorization responses or credentials.

The transfer screen uses Java/XML, resource strings, a demo-session guard and system-bar insets. Provider metadata queries, credential loading and network/stream work run on a worker executor. Buttons and the path field are disabled while selecting/transferring a file. The download path selected before the document picker is preserved through Activity recreation. Stale UI callbacks are discarded when the Activity is destroyed.

## Architecture and error handling

`TransferTestActivity -> DropboxTransferFactory -> DropboxTransferService -> DropboxRepository -> DbxClientV2 -> Dropbox API`

The factory reuses `SessionManager` and checks that the credential belongs to the locally configured app. The service owns Android URI streams. Repository methods own Dropbox API calls, preserving the existing constructor and `getCurrentAccount()` API. Download opens the server response before the local output stream: a server-side rejection cannot truncate an existing destination. A later network/write failure can still leave a partial destination; choose a new document when retrying.

## Automated checks

- `gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest`
- Unit tests cover stream copying and failures, SDK request/response behavior through Repository, local demo auth, and account API parsing/error propagation.
- Device tests cover demo registration/login/logout and transfer navigation, invalid paths/URIs, byte-preserving Android file streams with an in-memory HTTP transport, server rejection preserving a destination, and encrypted session behavior. These are isolated tests, not live Dropbox proof.
- Preserve device data by installing APK updates and invoking the runner directly. The Gradle connected-test runner was observed uninstalling the app during cleanup, so do not use it on a device whose real project connection must remain installed.

For the normal suite on a demo emulator:

```text
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r vn.edu.usth.dropboxclient.test/androidx.test.runner.AndroidJUnitRunner
```

For live acceptance, first authorize the project account with the four scopes in the app, then run only:

```text
adb -s emulator-5554 shell am instrument -w -r -e class vn.edu.usth.dropboxclient.LiveDropboxTransferTest -e liveDropboxTransfer true vn.edu.usth.dropboxclient.test/androidx.test.runner.AndroidJUnitRunner
```

Replace the emulator serial when needed. The opt-in live test checks account lookup and a one-item listing, uploads a uniquely named tiny `demo_transfer_*.txt` fixture, downloads it to app cache and compares exact bytes. It removes only its local cache files and leaves the synthetic cloud file for verification. It logs no account details or credentials. Both live tests are intentionally skipped in the normal suite.

## Current limits

This is a foreground academic transfer acceptance screen. Use small files. Single-request uploads are used; upload sessions for large files, durable background work, progress percentages, explicit in-flight cancellation and recovery after process death/rotation during an active transfer are not implemented. The app does not blindly retry an uncertain upload. General file browsing and create/rename/delete screens are not part of this transfer branch.

Validation on 22 October 2026: build and both APK packages passed; 14 unit tests passed; direct instrumentation on Pixel 6 API 34 reported 12 passed and 2 deliberately skipped live checks, with no failures. Lint had no errors. After renewing the four scopes on-device, the opt-in `LiveDropboxTransferTest` passed (1/1): real account lookup, folder listing, upload of a uniquely named small fixture, download and exact byte comparison. This proves that the configured App Folder and saved credential work for those API calls on the tested device; it does not verify the regular file-browser UI or larger transfers.

References: [Dropbox scope behavior](https://dropbox.tech/developers/customizing-scopes-in-oauth-flow), [Create folder](https://docs.dropboxapi.com/dropbox-api/api-reference/user-endpoints/files/create-folder-v-2), [Move/rename](https://docs.dropboxapi.com/dropbox-api/api-reference/user-endpoints/files/move-v-2), [Delete](https://docs.dropboxapi.com/dropbox-api/api-reference/user-endpoints/files/delete-v-2).
