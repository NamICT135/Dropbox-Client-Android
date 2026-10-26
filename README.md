# Dropbox Client Android

A six-member USTH Mobile Application Development project built with **Java and XML** (`vn.edu.usth.dropboxclient`). The app uses a local account for entry and a separately authorized Dropbox account for real cloud operations. Dropbox access is limited to the Developer App's **Scoped App Folder**.

## Features

- Create and sign in to a local account without a personal email. Local logout keeps the device's Dropbox connection; **Disconnect project storage** removes it explicitly.
- Connect Dropbox with OAuth 2.0 PKCE and show the account returned by `DropboxRepository.getCurrentAccount()`.
- Browse root and nested folders, return to a parent folder, refresh and load more pages. Files, empty folders and errors have visible states.
- Create, rename and delete folders with validation, conflict feedback and delete confirmation.
- Upload from Android's document picker into the folder currently open in **Files**. Tap a file for details and use **Download from Dropbox** to choose a local save destination.

The local account is a coursework entry mechanism, not a production identity service or a separate Dropbox account for each user. Local accounts on the same device share its authorized project App Folder.

## Run

1. Open this repository's root in Android Studio. Use the bundled JBR 21 and Android SDK 36.1; the app supports Android 8.0 (API 26) and later.
2. Allow Android Studio to create the ignored `local.properties` for your SDK. To use the real Dropbox API, add `DROPBOX_APP_KEY=your_app_key` there and rebuild. The App key is a public application identifier; never commit a password, token or App secret.
3. In the Dropbox Developer App, enable `account_info.read`, `files.metadata.read`, `files.content.read` and `files.content.write`. Save changes and authorize again if scopes changed.
4. Run `app` on an emulator or Android device. Create a local username and password, open **Account > Set up project storage**, and approve the Dropbox request.
5. Open **Files** to browse and manage the App Folder. Use **Upload file** in the current folder; tap a file to download it.

For a presentation sequence and explanation of the code path, see [Run and present the app](docs/run-demo-vi.md).

On Windows, verify the project with:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

## How it works

```text
LoginActivity (local account)
  -> MainActivity
     -> FileBrowserFragment / AccountFragment
        -> DropboxRepository
           -> DbxClientV2 -> Dropbox API
```

`SessionManager` encrypts the Dropbox credential with Android Keystore and excludes it from backup. `DropboxClientFactory` creates the SDK client. File transfers use Android content URIs through `TransferActivity`, `TransferViewModel` and `DropboxTransferService`. Network and file I/O run on worker executors; UI updates return to the main thread.

## Verification and limits

On 26 October 2026, `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` (**39/39**) and `lintDebug` (**0 errors, 7 warnings**) passed for the integrated source. All **3 UI tests** passed on Pixel 10 for local account flow, Account/Files navigation and project setup. Earlier live checks on Pixel 6 verified account lookup, folder listing, folder create/rename/delete, a small upload and exact-byte download through the Dropbox API. The new Files-to-transfer picker flow still needs a fresh end-to-end check on an authorized device. See [transfer acceptance](docs/transfer-review.md).

Use small files. Large upload sessions, background recovery, progress percentages, and automatic retry of uncertain writes are outside this MVP. A failure after a download starts writing may leave a partial destination. Each newly installed device must complete its own Dropbox authorization.

[Team development without shared credentials](docs/team-development.md) explains how members build and test without sharing the project's Dropbox login.
