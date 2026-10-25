package vn.edu.usth.dropboxclient;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.dropbox.core.oauth.DbxCredential;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionManagerTest {
    private Context isolatedContext() {
        return new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("test_" + name, mode);
            }
        };
    }
    @Test public void fullLogoutClearsDropboxAndDemoReloginNeedsOAuth() throws Exception {
        Context context = isolatedContext();
        SessionManager cloud = new SessionManager(context);
        DemoAuthManager demo = new DemoAuthManager(context);
        String marker = UUID.randomUUID().toString();
        char[] password = UUID.randomUUID().toString().toCharArray();
        try {
            cloud.clear();
            cloud.save(new DbxCredential(marker));
            demo.register("student", password);
            long oldRevision = cloud.revision();
            assertTrue(cloud.clear());
            demo.logout();
            assertNull(demo.currentUsername());
            assertNull(new SessionManager(context).load());
            assertFalse(cloud.saveIfCurrent(new DbxCredential(UUID.randomUUID().toString()),
                    oldRevision));
            assertEquals(DemoAuthManager.Result.SUCCESS, demo.login("student", password));
            assertNull(new SessionManager(context).load());
        } finally {
            cloud.clear();
            context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    @Test public void disconnectOnlyClearsDropboxAndInvalidatesOldWork() throws Exception {
        Context context = isolatedContext();
        SessionManager cloud = new SessionManager(context);
        DemoAuthManager demo = new DemoAuthManager(context);
        try {
            cloud.clear();
            demo.register("student", UUID.randomUUID().toString().toCharArray());
            long oldRevision = cloud.revision();
            cloud.save(new DbxCredential(UUID.randomUUID().toString()));
            long loadingRevision = cloud.revision();
            assertTrue(cloud.clear());
            assertEquals("student", demo.currentUsername());
            assertNull(cloud.load());
            assertFalse(cloud.saveIfCurrent(new DbxCredential(UUID.randomUUID().toString()),
                    oldRevision));
            assertFalse(cloud.clearIfCurrent(loadingRevision));
            assertNull(cloud.load());
            long beforeOAuth = cloud.revision();
            long oauthRevision = cloud.beginOAuth();
            assertFalse(cloud.clearIfCurrent(beforeOAuth));
            assertTrue(oauthRevision > beforeOAuth);
        } finally {
            cloud.clear();
            context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    @Test public void encryptedSessionRoundTripAndLogout() throws Exception {
        Context context = isolatedContext();
        SessionManager session = new SessionManager(context);
        session.clear();
        try {
            assertNull(session.load());
            String value = UUID.randomUUID().toString();
            session.save(new DbxCredential(value));
            assertEquals(value, new SessionManager(context).load().getAccessToken());
            session.beginOAuth(); // A cancelled attempt leaves the existing connection intact.
            assertEquals(value, new SessionManager(context).load().getAccessToken());
            String encrypted = context.getSharedPreferences("dropbox_session", Context.MODE_PRIVATE)
                    .getString("credential", "");
            assertFalse(encrypted.contains(value));
            session.clear();
            assertNull(new SessionManager(context).load());
        } finally { session.clear(); }
    }
    @Test public void corruptedSessionIsReported() throws Exception {
        Context context = isolatedContext();
        SessionManager session = new SessionManager(context);
        try {
            context.getSharedPreferences("dropbox_session", Context.MODE_PRIVATE).edit()
                    .putString("credential", "!").putString("iv", "!").commit();
            assertThrows(Exception.class, session::load);
        } finally { session.clear(); }
    }
}
