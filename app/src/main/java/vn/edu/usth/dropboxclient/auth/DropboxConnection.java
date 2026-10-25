package vn.edu.usth.dropboxclient.auth;

import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxOAuthError;
import com.dropbox.core.oauth.DbxOAuthException;
import com.dropbox.core.oauth.DbxCredential;
import java.io.IOException;
import java.security.GeneralSecurityException;

/** Verifies an OAuth result before making it the device's Dropbox session. */
public final class DropboxConnection {
    public interface Verifier {
        void verify(DbxCredential credential) throws DbxException;
    }
    public interface Store {
        boolean saveIfCurrent(DbxCredential credential, long revision)
                throws GeneralSecurityException, IOException;
    }
    public enum Result { CONNECTED, CANCELLED, STALE, WRONG_APP }

    private DropboxConnection() { }

    /** Transient/configuration errors must not discard a usable refresh credential. */
    public static boolean isAuthorizationFailure(DbxException error) {
        if (error instanceof InvalidAccessTokenException) return true;
        if (!(error instanceof DbxOAuthException)) return false;
        DbxOAuthError oauth = ((DbxOAuthException) error).getDbxOAuthError();
        return oauth != null && DbxOAuthError.INVALID_GRANT.equals(oauth.getError());
    }

    public static Result complete(DbxCredential credential, String appKey, long revision,
                                  Verifier verifier, Store store)
            throws DbxException, GeneralSecurityException, IOException {
        if (credential == null) return Result.CANCELLED;
        if (!appKey.equals(credential.getAppKey())) return Result.WRONG_APP;
        verifier.verify(credential);
        return store.saveIfCurrent(credential, revision) ? Result.CONNECTED : Result.STALE;
    }
}
