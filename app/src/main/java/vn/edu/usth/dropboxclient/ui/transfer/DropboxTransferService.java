
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Context;
import android.net.Uri;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DropboxTransferService {

    private final DbxClientV2 client;
    private final Context context;

    public DropboxTransferService(
            Context context,
            DbxClientV2 client
    ) {
        if (context == null || client == null) {
            throw new IllegalArgumentException(
                    "Context and Dropbox client are required"
            );
        }

        this.context = context.getApplicationContext();
        this.client = client;
    }

    // Upload a local Android file to Dropbox
    public FileMetadata uploadFile(
            Uri localFileUri,
            String dropboxPath
    ) throws IOException, DbxException {

        validatePath(dropboxPath);

        if (localFileUri == null) {
            throw new IllegalArgumentException(
                    "Local file URI cannot be null"
            );
        }

        try (InputStream inputStream =
                     FileReadHelper.openFile(
                             context, localFileUri
                     )) {

            return client.files()
                    .uploadBuilder(dropboxPath)
                    .withMode(WriteMode.ADD)
                    .withAutorename(true)
                    .uploadAndFinish(inputStream);
        }
    }

    // Download a Dropbox file to Android storage
    public FileMetadata downloadFile(
            String dropboxPath,
            Uri destinationUri
    ) throws IOException, DbxException {

        validatePath(dropboxPath);

        if (destinationUri == null) {
            throw new IllegalArgumentException(
                    "Destination URI cannot be null"
            );
        }

        try (OutputStream outputStream =
                     context.getContentResolver()
                             .openOutputStream(destinationUri, "w")) {

            if (outputStream == null) {
                throw new IOException(
                        "Cannot open destination file"
                );
            }

            return client.files()
                    .downloadBuilder(dropboxPath)
                    .download(outputStream);
        }
    }

    private static void validatePath(String path) {
        if (path == null
                || !path.startsWith("/")
                || path.length() <= 1
                || path.endsWith("/")) {

            throw new IllegalArgumentException(
                    "Invalid Dropbox file path"
            );
        }
    }
}
