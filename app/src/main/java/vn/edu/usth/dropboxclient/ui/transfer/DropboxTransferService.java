
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Context;
import android.net.Uri;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DropboxTransferService {

    private final DropboxRepository repository;
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
        this.repository = new DropboxRepository(client);
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

            return repository.uploadFile(dropboxPath, inputStream);
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

        // Check the server response before opening/truncating the destination.
        try (DbxDownloader<FileMetadata> download = repository.openDownload(dropboxPath);
             OutputStream outputStream =
                     context.getContentResolver()
                             .openOutputStream(destinationUri, "w")) {

            if (outputStream == null) {
                throw new IOException(
                        "Cannot open destination file"
                );
            }

            return download.download(outputStream);
        }
    }

    public FileMetadata downloadPreview(String dropboxPath, File destination, long maxBytes)
            throws IOException, DbxException {
        validatePath(dropboxPath);
        if (destination == null || maxBytes <= 0) throw new IllegalArgumentException("Invalid preview destination");
        try (DbxDownloader<FileMetadata> download = repository.openDownload(dropboxPath)) {
            if (download.getResult().getSize() > maxBytes) throw new PreviewTooLargeException();
            try (OutputStream output = new FileOutputStream(destination)) {
                return download.download(output);
            }
        }
    }

    public static final class PreviewTooLargeException extends IOException {
        public PreviewTooLargeException() { super("Preview exceeds the supported size"); }
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
