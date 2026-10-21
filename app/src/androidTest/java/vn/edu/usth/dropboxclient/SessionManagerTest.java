package vn.edu.usth.dropboxclient;

import android.content.Context;
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
    @Test public void encryptedSessionRoundTripAndLogout() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        try {
            context.getSharedPreferences("dropbox_session", Context.MODE_PRIVATE).edit()
                    .putString("credential", "!").putString("iv", "!").commit();
            assertThrows(Exception.class, session::load);
        } finally { session.clear(); }
    }
}
