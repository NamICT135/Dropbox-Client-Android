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

The Android document picker supplies content URIs. Provider queries, credential loading and network/stream I/O run on a worker executor. Preview streams a bounded file into app-private cache, then `FileProvider` grants the viewer temporary read access. Download obtains the server response before opening the local destination, so a server rejection cannot truncate an existing file. A failure after writing starts can still leave a partial local file; choose a new destination when retrying.

## Automated checks and limits

Run `.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`. Unit and isolated device tests use local fixtures or an in-memory Dropbox transport. The opt-in `LiveDropboxSearchTest` passed a read-only search against the current Dropbox API without printing account or file details. The earlier opt-in `LiveDropboxTransferTest` passed a real account lookup, folder listing, small upload, download and exact byte comparison on Pixel 6. These live results verify the API routes, not every picker or viewer interaction in the current UI.

For an authorized device, install APK updates with `adb install -r` and invoke the instrumentation runner directly. The Gradle connected-test runner previously uninstalled the app during cleanup, which would remove the device's saved Dropbox connection.

Transfers use a single foreground request. Large-file upload sessions, progress percentages, process-death recovery and automatic retry of uncertain writes are outside the MVP.
