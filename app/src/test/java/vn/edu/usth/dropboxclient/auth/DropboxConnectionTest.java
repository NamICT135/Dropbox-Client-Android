package vn.edu.usth.dropboxclient.auth;

import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxOAuthError;
import com.dropbox.core.oauth.DbxOAuthException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class DropboxConnectionTest {
    private static final String APP_KEY = "testapp";

    private DbxCredential candidate() {
        return new DbxCredential(UUID.randomUUID().toString(), null, null, APP_KEY);
    }

    private static final class Store implements DropboxConnection.Store {
        long revision;
        DbxCredential saved;
        @Override public boolean saveIfCurrent(DbxCredential value, long expected) {
            if (revision != expected) return false;
            saved = value;
            revision++;
            return true;
        }
        void disconnect() { saved = null; revision++; }
    }

    @Test public void savesOnlyAfterAccountVerification() throws Exception {
        Store store = new Store();
        DbxCredential candidate = candidate();
        AtomicInteger verifications = new AtomicInteger();
        assertEquals(DropboxConnection.Result.CONNECTED,
                DropboxConnection.complete(candidate, APP_KEY, store.revision,
                        value -> { assertSame(candidate, value); verifications.incrementAndGet(); }, store));
        assertEquals(1, verifications.get());
        assertSame(candidate, store.saved);
    }

    @Test public void cancellationAndFailurePreserveExistingSession() throws Exception {
        Store store = new Store();
        DbxCredential existing = candidate();
        store.saved = existing;
        AtomicInteger verifications = new AtomicInteger();
        assertEquals(DropboxConnection.Result.CANCELLED,
                DropboxConnection.complete(null, APP_KEY, store.revision,
                        value -> verifications.incrementAndGet(), store));
        assertEquals(DropboxConnection.Result.WRONG_APP,
                DropboxConnection.complete(new DbxCredential(UUID.randomUUID().toString()),
                        APP_KEY, store.revision, value -> verifications.incrementAndGet(), store));
        assertThrows(IllegalStateException.class,
                () -> DropboxConnection.complete(candidate(), APP_KEY, store.revision,
                        value -> { throw new IllegalStateException("verification failed"); }, store));
        assertEquals(0, verifications.get());
        assertSame(existing, store.saved);
        assertEquals(0, store.revision);
    }

    @Test public void lateCallbackCannotRestoreSessionAfterLogoutOrDisconnect() throws Exception {
        Store store = new Store();
        long startedAt = store.revision;
        store.disconnect(); // Both actions invalidate the pending OAuth revision.
        assertEquals(DropboxConnection.Result.STALE,
                DropboxConnection.complete(candidate(), APP_KEY, startedAt,
                        value -> { }, store));
        assertNull(store.saved);
    }

    @Test public void failedSaveDoesNotClaimConnection() {
        assertThrows(IOException.class,
                () -> DropboxConnection.complete(candidate(), APP_KEY, 0,
                        value -> { }, (value, revision) -> { throw new IOException("write failed"); }));
    }
    @Test public void onlyRevokedRefreshGrantsInvalidateTheSession() {
        assertTrue(DropboxConnection.isAuthorizationFailure(new DbxOAuthException("test",
                new DbxOAuthError(DbxOAuthError.INVALID_GRANT, "revoked"))));
        assertFalse(DropboxConnection.isAuthorizationFailure(new DbxOAuthException("test",
                new DbxOAuthError(DbxOAuthError.UNKNOWN, "temporary failure"))));
        assertFalse(DropboxConnection.isAuthorizationFailure(new DbxOAuthException("test",
                new DbxOAuthError(DbxOAuthError.INVALID_REQUEST, "configuration"))));
    }
}
