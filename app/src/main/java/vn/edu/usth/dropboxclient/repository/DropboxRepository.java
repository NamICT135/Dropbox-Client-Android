
package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.users.FullAccount;

import java.util.ArrayList;
import java.util.List;

public class DropboxRepository {

    private final DbxClientV2 client;

    public DropboxRepository(DbxClientV2 client) {
        this.client = client;
    }

    public FullAccount getCurrentAccount() throws DbxException {
        return client.users().getCurrentAccount();
    }

    public List<Metadata> listFolder(String path) throws DbxException {
        List<Metadata> files = new ArrayList<>();

        ListFolderResult result = client.files().listFolder(path);

        while (true) {
            files.addAll(result.getEntries());

            if (!result.getHasMore()) {
                break;
            }

            result = client.files().listFolderContinue(result.getCursor());
        }

        return files;
    }
}
    