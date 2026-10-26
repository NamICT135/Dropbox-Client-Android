# Upload and download

The Files screen opens transfers in every build. **Upload file** sends a selected Android document to the Dropbox folder currently open. Tapping a file shows its path and size; **Download from Dropbox** opens the save picker with that path filled in. JPG, PNG, WEBP, GIF and PDF files also have **Preview**. The Search tab can open matching folders in Files and matching files in the transfer screen.

## Setup

The Dropbox Developer App uses Scoped App Folder access and the scopes `account_info.read`, `files.metadata.read`, `files.content.read`, and `files.content.write`. Save scope changes in the App Console, then authorize again in the Android app. Configure the public App key in ignored `local.properties`; never commit a credential or App secret. Paths shown in Files are relative to the App Folder.

A local app account is required to open Files. A group member connects Dropbox once per device through **Account > Set up project storage**. Other local accounts on that device share the connection. Logging out of a local account leaves the Dropbox connection in place; **Disconnect project storage** removes it from that device.

## Manual acceptance

1. Open **Files**, enter a folder, tap **Upload file**, and choose a small disposable document.
2. Check the returned name, path and byte count, then return to Files and confirm that the list refreshes. Upload uses Dropbox ADD with automatic renaming, so an existing cloud file is not silently overwritten.
3. Tap that file, choose **Download from Dropbox**, and pick a new local destination. Compare its contents with the original.
4. Cancel each picker, try an invalid path, and test an unavailable file or offline connection. The screen should remain usable and show an error.
5. Rotate the device during a transfer: the active operation and result remain with the Activity's ViewModel. Keep the screen open until it finishes.
6. Search for the uploaded file, then preview an image or PDF under 20 MB. Confirm that Android opens the installed viewer and that unsupported types or missing viewers show a useful message.

`TransferActivity -> TransferViewModel -> DropboxTransferFactory -> DropboxTransferService -> DropboxRepository -> DbxClientV2 -> Dropbox API`

The Android document picker supplies content URIs. Provider queries, credential loading and network/stream I/O run on a worker executor. Preview checks metadata and actual streamed bytes against a 20 MiB limit, then `FileProvider` grants the viewer temporary read access. Cached previews older than 24 hours are removed before another preview; recent copies remain available to viewers. Opening a file from Search derives its parent folder for the transfer screen. Download obtains the server response before opening the local destination, so a server rejection cannot truncate an existing file. A failure after writing starts can still leave a partial local file; choose a new destination when retrying.

## Automated checks and limits

Run `.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`. On 26 October 2026 the builds and 41 unit tests passed; lint reported 0 errors and 8 warnings. On Pixel 10, 19 isolated device tests passed and 4 opt-in live tests were intentionally skipped. `TransferFlowTest` covers picker cancellation, invalid/unsupported paths, missing connections, and preserving the file's parent folder across recreation. `DropboxTransferServiceIoTest` covers exact bytes, rejected downloads preserving the destination, private content URIs, and preview limits even when actual bytes exceed reported size.

`LiveDropboxUiTest` passed **1/1 on authorized Pixel 6 on 26 October 2026**. It is opt-in and drives Files → upload → download → Search → preview with the real Dropbox API and compares downloaded bytes. System picker results and the viewer launch are intercepted with deterministic content URIs, so this verifies app callbacks, file streams and API integration, not manual navigation inside Android's picker or rendering inside a third-party viewer. Each run leaves a uniquely named tiny `ui_acceptance_*.png` cloud fixture; temporary local files and the test's local account are removed, and the previous local session is restored. Existing Dropbox credentials and files are preserved. Manual steps above remain necessary for choosing real documents and confirming the installed viewer renders them.

After installing matching app and test APKs with `adb install -r`, run only the live UI test on the authorized device:

```text
adb -s DEVICE_SERIAL shell am instrument -w -r -e class vn.edu.usth.dropboxclient.LiveDropboxUiTest -e liveDropboxUi true vn.edu.usth.dropboxclient.test/androidx.test.runner.AndroidJUnitRunner
```

For an authorized device, install APK updates with `adb install -r` and invoke the instrumentation runner directly. The Gradle connected-test runner previously uninstalled the app during cleanup, which would remove the device's saved Dropbox connection.

Transfers use a single foreground request. Large-file upload sessions, progress percentages, process-death recovery and automatic retry of uncertain writes are outside the MVP.
