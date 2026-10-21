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
    @Test public void demoLogoutPreservesProjectConnection() throws Exception {
        Context context = isolatedContext();
        SessionManager cloud = new SessionManager(context);
        DemoAuthManager demo = new DemoAuthManager(context);
        String marker = UUID.randomUUID().toString();
        try {
            cloud.save(new DbxCredential(marker));
            demo.register("student", UUID.randomUUID().toString().toCharArray());
            demo.logout();
            assertNull(demo.currentUsername());
            assertEquals(marker, new SessionManager(context).load().getAccessToken());
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
