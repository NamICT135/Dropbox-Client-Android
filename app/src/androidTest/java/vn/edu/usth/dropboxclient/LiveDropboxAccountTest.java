package vn.edu.usth.dropboxclient;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.users.FullAccount;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import static org.junit.Assert.*;

/** Explicit, read-only acceptance check using the device's authorized project account. */
@RunWith(AndroidJUnit4.class)
public class LiveDropboxAccountTest {
    @Test public void getCurrentAccountReturnsLiveProjectAccount() throws Exception {
        Assume.assumeTrue("Enable only on the authorized project demo device",
                "true".equals(InstrumentationRegistry.getArguments().getString("liveDropbox")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DbxCredential credential = new SessionManager(context).load();
        assertNotNull("Connect the project account in the app before this test", credential);
        assertFalse("Configure the project's App key locally", BuildConfig.DROPBOX_APP_KEY.isEmpty());
        assertEquals("Use the credential belonging to this configured app",
                BuildConfig.DROPBOX_APP_KEY, credential.getAppKey());
        FullAccount account = new DropboxRepository(DropboxClientFactory.create(credential))
                .getCurrentAccount();
        assertNotNull(account);
        assertFalse("Dropbox must return an account ID", account.getAccountId().isEmpty());
        assertFalse("Dropbox must return a display name", account.getName().getDisplayName().isEmpty());
        // Never print the response, email, account ID, or credential into test reports.
    }
}
