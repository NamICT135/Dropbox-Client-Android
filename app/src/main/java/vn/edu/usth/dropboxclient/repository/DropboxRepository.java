package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;
import java.io.IOException;
import java.io.InputStream;

public class DropboxRepository {

    private final DbxClientV2 client;

    public DropboxRepository(DbxClientV2 client) {
        this.client = client;
    }

    public FullAccount getCurrentAccount() throws DbxException {
        return client.users().getCurrentAccount();
    }

    /** Caller owns the input stream. Existing cloud files are never silently overwritten. */
    public FileMetadata uploadFile(String path, InputStream input) throws DbxException, IOException {
        return client.files().uploadBuilder(path).withMode(WriteMode.ADD)
                .withAutorename(true).uploadAndFinish(input);
    }

    /** Caller must close the download response, including when opening a local file fails. */
    public DbxDownloader<FileMetadata> openDownload(String path) throws DbxException {
        return client.files().downloadBuilder(path).start();
    }
}
