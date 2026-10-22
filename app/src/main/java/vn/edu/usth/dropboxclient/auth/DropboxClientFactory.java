package vn.edu.usth.dropboxclient.auth;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;

public final class DropboxClientFactory {
    private DropboxClientFactory() { }
    public static DbxRequestConfig requestConfig() {
        return DbxRequestConfig.newBuilder("usth-dropbox-client/1.0").build();
    }
    public static DbxClientV2 create(DbxCredential credential) {
        return new DbxClientV2(requestConfig(), credential);
    }
}
