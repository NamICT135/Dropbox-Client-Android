
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
import java.io.FilterOutputStream;

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

        File temporary = File.createTempFile("dropbox-download-", ".tmp", context.getCacheDir());
        try {
            FileMetadata metadata;
            try (DbxDownloader<FileMetadata> download = repository.openDownload(dropboxPath);
                 OutputStream staged = new FileOutputStream(temporary)) {
                metadata = download.download(staged);
            }
            VerifiedDownload.verify(temporary, metadata.getSize(), metadata.getContentHash());
            // SAF cannot atomically replace every provider URI. Never delete the user's URI:
            // ACTION_CREATE_DOCUMENT may return an existing document selected for replacement.
            boolean opened = false;
            try (OutputStream destination = context.getContentResolver().openOutputStream(destinationUri, "rwt")) {
                if (destination == null) throw new IOException("Cannot open destination file");
                opened = true;
                VerifiedDownload.copy(temporary, destination);
            } catch (IOException e) {
                if (opened) throw new PartialDestinationException(e);
                throw e;
            }
            return metadata;
        } finally {
            if (!temporary.delete()) temporary.deleteOnExit();
        }
    }

    public FileMetadata downloadPreview(String dropboxPath, File destination, long maxBytes)
            throws IOException, DbxException {
        validatePath(dropboxPath);
        if (destination == null || maxBytes <= 0) throw new IllegalArgumentException("Invalid preview destination");
        try (DbxDownloader<FileMetadata> download = repository.openDownload(dropboxPath)) {
            if (download.getResult().getSize() > maxBytes) throw new PreviewTooLargeException();
            // Enforce the limit on actual bytes as well as server metadata.
            try (OutputStream output = new FilterOutputStream(new FileOutputStream(destination)) {
                private long written;

                @Override public void write(int value) throws IOException {
                    if (written >= maxBytes) throw new PreviewTooLargeException();
                    out.write(value);
                    written++;
                }

                @Override public void write(byte[] buffer, int offset, int length) throws IOException {
                    if (length > maxBytes - written) throw new PreviewTooLargeException();
                    out.write(buffer, offset, length);
                    written += length;
                }
            }) {
                return download.download(output);
            }
        }
    }

    public static final class PreviewTooLargeException extends IOException {
        public PreviewTooLargeException() { super("Preview exceeds the supported size"); }
    }

    public static final class PartialDestinationException extends IOException {
        public PartialDestinationException(IOException cause) {
            super("The destination may contain a partial file; choose a new location before retrying", cause);
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
