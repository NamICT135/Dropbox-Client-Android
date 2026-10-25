# Dropbox Client Android

USTH Mobile Application Development group project. The app is written in **Java with XML layouts** (`vn.edu.usth.dropboxclient`). It uses local demo usernames for the academic UI and a separate Dropbox connection for real API calls. Dropbox access is limited to the project's **App Folder**; Box and offline synchronization are outside the project scope.

## What the app does now

- Register and sign in with a local demo username and password. Registration has optional first name, surname and email fields; these are local profile details, not a new Dropbox account. Existing demo accounts still work without an email. **Log out** removes both the demo login and the locally saved Dropbox credential before returning to login.
- Connect a Dropbox account through **Account > Set up project storage**. The app verifies the account before saving the credential and displays the name, email and verification state, plan and account ID returned by Dropbox. Network errors can be retried; invalid access requires connecting again. **Disconnect Dropbox only** keeps the demo login.
- **Files** reads the connected project's App Folder through `DropboxRepository`. It lists files and folders, opens folders, moves to the parent, refreshes, loads additional pages and shows loading, empty and error states with retry.
- In debug builds, open **Account > Test upload / download** to select a small local file and a download destination with Android's document pickers. The transfer service uses `DropboxRepository` for Dropbox API calls.

The Dropbox connection is local to the device and is cleared on full logout. Logging in to the demo again requires a new Dropbox OAuth authorization. A team member can build the app and develop the UI without the project's Dropbox email, password or token. A new device also needs its own Dropbox authorization before it can call the real API.

## Run the project

1. Clone the repository and open its root in Android Studio. Use Android Studio's bundled JBR 21 and install Android SDK 36.1. The app supports Android 8.0 (API 26) and later.
2. Let Android Studio create the ignored `local.properties` file for your SDK path. Local demo login and unit tests work without a Dropbox App key.
3. For real Dropbox access, add `DROPBOX_APP_KEY=your_app_key` to the root `local.properties` or set the environment variable, then rebuild. The App key is a public application identifier; never commit passwords, tokens or the App secret.
4. Run the app on an emulator or device. Register a demo username (3-24 letters, digits or underscores) and a test password (8-128 characters). From Account, connect project storage and approve the Dropbox request.
5. Open **Files** to browse the connected project's App Folder. Use **Up** for the parent folder, **Refresh** to reload and **Load more** when Dropbox returns another page. For transfers, use **Account > Test upload / download** in a debug build with a small disposable file. [Transfer acceptance and limitations](docs/transfer-review.md) explains the test commands.

On Windows, run the build and local tests with:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

The Dropbox Developer App needs `account_info.read`, `files.metadata.read`, `files.content.read` and `files.content.write`. After changing scopes, authorize again in the app. The project uses OAuth 2.0 with PKCE and Dropbox SDK 8.0.2; `app/build.gradle.kts` configures the callback scheme from the App key. See the [Dropbox OAuth guide](https://docs.dropboxapi.com/dropbox-api/docs/oauth) and [SDK repository](https://github.com/dropbox/dropbox-sdk-java).

## How it is organized

- `DemoAuthManager` handles local usernames and password verification. Passwords are salted and hashed with PBKDF2-HMAC-SHA256; demo login is for coursework, not a production identity service.
- `DropboxAuthManager` starts OAuth. `SessionManager` encrypts the Dropbox credential with AES-GCM and Android Keystore; backup rules exclude the demo and Dropbox preferences.
- OAuth cancellation or failed account verification preserves the current connection. Session revisions prevent callbacks and account lookups started before logout or disconnect from restoring or displaying the old connection. Local removal does not revoke a token on Dropbox's servers.
- `DropboxClientFactory` creates `DbxClientV2`. `DropboxRepository` owns account, folder listing and transfer API calls. `FileBrowserFragment` presents paged Dropbox results; `DropboxTransferService` handles Android URI streams, and `TransferTestActivity` provides the debug UI.
- Login and Dropbox verification use ViewModels so rotation retains pending work. Passwords are not saved in Activity state. Account requests, provider queries and transfers run on worker executors. UI updates return to the main thread.

## Verified on 22 October 2026

- [x] Build and both debug APK packages completed; lint reported **0 errors, 8 warnings**.
- [x] **14/14 unit tests** passed.
- [x] On Pixel 6 API 34, **12 isolated device tests** passed. Two live tests were intentionally skipped in that normal run.
- [x] Local demo registration/login/logout and Files / Account navigation were exercised on the emulator.
- [x] Dropbox OAuth returned to the app and Account showed the real connected account.
- [x] The opt-in live test passed **1/1**: account lookup, App Folder listing, upload of a small synthetic file, download and exact byte comparison.
- [x] An isolated device test confirmed that a rejected download does not truncate an existing local destination.

On 25 October 2026, the File Browser branch passed `assembleDebug`, `testDebugUnitTest` (15/15) and `lintDebug` (0 errors, 9 warnings). The new folder-listing test uses an in-memory Dropbox response. On Pixel 6 API 34, the installed app retained the project connection, listed a real file in the App Folder root and loaded it again after **Refresh**.

On 25 October 2026, the login/account update passed `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` (**23/23**) and `lintDebug` (**0 errors, 11 warnings**). Direct instrumentation on a fresh Pixel 6 API 34 emulator passed **14 isolated tests**, with **2 live Dropbox tests intentionally skipped**. Coverage includes registration correction, duplicate users, sign-in/logout, optional profiles, form recreation, cancelled logout, encrypted session round-trips and stale OAuth rejection. No App key or live Dropbox account was available for this run; live OAuth cancellation, revoked access and refresh still need the device checks below.

The isolated tests use local fixtures or an in-memory HTTP transport; the opt-in test above is the evidence for real Dropbox API access. See [transfer test details](docs/transfer-review.md). Do not run Gradle's `connectedDebugAndroidTest` on the authorized demo device: its cleanup was observed uninstalling the app. The linked guide shows how to run instrumentation directly while preserving the connection.

## Still to complete

- [ ] A second team member performs a clean clone, Gradle sync, build and run.
- [ ] OAuth cancellation, revoked/expired authorization, token refresh and disconnect/reconnect are checked end to end on a device.
- [ ] On a device with a configured App key and test Dropbox account, check OAuth → Account displays the Dropbox name → full logout → demo login again shows disconnected → OAuth is required again. Also check cancel, offline retry, revoked access and **Disconnect Dropbox only**. This live regression has not yet been run for the session changes above.
- [ ] The debug transfer screen is checked manually with both document pickers, cancellation, offline failure and retry. The live API test does not cover every picker interaction.
- [ ] Verify nested/empty folders, parent navigation, pagination and Vietnamese filenames on an authorized device. Root listing and Refresh have been checked.
- [ ] Create folder, rename and delete screens handle validation, conflicts and cancellation.
- [ ] Rapid taps, rate limits, rotation during active work and process restart receive full regression checks.
- [ ] The full MVP is integrated, the final demo is rehearsed and the presentation is complete.
- [ ] Confirm the team's member count and assign named owners; the course slide and six-person working plan differ. Decide the repository license.

The transfer screen is a **foreground, small-file academic demo**. Large upload sessions, background transfer recovery, progress percentages and in-flight cancellation are not implemented. A network failure after download begins can leave a partial local destination. General file browsing and file operations are separate team tasks.

## Team workflow

Keep `main` stable. Each member works on a feature branch, commits focused changes, pushes and opens a pull request into `main`. Review **Files changed**, run the relevant checks and obtain a teammate's approval before merging. Never share Dropbox credentials through Git or chat. [Team development without shared credentials](docs/team-development.md) explains how to work with mock responses and how to repeat a live check on an authorized device.
