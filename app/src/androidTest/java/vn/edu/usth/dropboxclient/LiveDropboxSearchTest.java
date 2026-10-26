package vn.edu.usth.dropboxclient;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dropbox.core.oauth.DbxCredential;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import static org.junit.Assert.*;

/** Opt-in, read-only search on an authorized device; never logs account or file details. */
@RunWith(AndroidJUnit4.class)
public class LiveDropboxSearchTest {
    @Test public void authorizedConnectionCanSearchAppFolder() throws Exception {
        Assume.assumeTrue("Explicitly enable on an authorized device",
                "true".equals(InstrumentationRegistry.getArguments().getString("liveDropboxSearch")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DbxCredential credential = new SessionManager(context).load();
        assertNotNull("Connect Dropbox first", credential);
        assertEquals(BuildConfig.DROPBOX_APP_KEY, credential.getAppKey());
        assertNotNull(new DropboxRepository(DropboxClientFactory.create(credential))
                .search("usth-acceptance-" + UUID.randomUUID()));
    }
}
