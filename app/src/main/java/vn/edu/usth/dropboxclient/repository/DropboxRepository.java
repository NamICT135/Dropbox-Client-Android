package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;

public class DropboxRepository {

    private final DbxClientV2 client;

    public DropboxRepository(DbxClientV2 client) {
        this.client = client;
    }

    public FullAccount getCurrentAccount() throws DbxException {
        return client.users().getCurrentAccount();
    }
}