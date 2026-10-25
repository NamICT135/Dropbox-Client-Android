package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
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

    /** Create a new folder at the given path. */
    public FolderMetadata createFolder(String path) throws DbxException {
        return client.files().createFolderV2(path).getMetadata();
    }

    /** Rename or relocate a file or folder from one path to another. */
    public Metadata renameFolder(String fromPath, String toPath) throws DbxException {
        return client.files().moveV2(fromPath, toPath).getMetadata();
    }

    /** Delete a file or folder at the given path. */
    public Metadata deleteFolder(String path) throws DbxException {
        return client.files().deleteV2(path).getMetadata();
    }
}
