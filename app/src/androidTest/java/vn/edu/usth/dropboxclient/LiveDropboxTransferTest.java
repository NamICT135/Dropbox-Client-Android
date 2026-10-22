package vn.edu.usth.dropboxclient;

import android.content.Context;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import vn.edu.usth.dropboxclient.ui.transfer.DropboxTransferService;
import static org.junit.Assert.*;

/** Opt-in: uploads a tiny synthetic file to App Folder, then downloads and compares bytes. */
@RunWith(AndroidJUnit4.class)
public class LiveDropboxTransferTest {
    @Test public void authorizedProjectCanListUploadAndDownload() throws Exception {
        Assume.assumeTrue("Explicitly enable on the project demo device",
                "true".equals(InstrumentationRegistry.getArguments().getString("liveDropboxTransfer")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DbxCredential credential = new SessionManager(context).load();
        assertNotNull("Connect Dropbox and grant the four scopes before running", credential);
        assertFalse(BuildConfig.DROPBOX_APP_KEY.isEmpty());
        assertEquals(BuildConfig.DROPBOX_APP_KEY, credential.getAppKey());
        DbxClientV2 client = DropboxClientFactory.create(credential);
        assertNotNull(new DropboxRepository(client).getCurrentAccount());
        assertNotNull(client.files().listFolderBuilder("").withLimit(1L).start());
        Path input = Files.createTempFile(context.getCacheDir().toPath(), "live-in-", ".txt");
        Path output = Files.createTempFile(context.getCacheDir().toPath(), "live-out-", ".txt");
        byte[] expected = "USTH Dropbox transfer acceptance\n".getBytes(StandardCharsets.UTF_8);
        try {
            Files.write(input, expected);
            DropboxTransferService service = new DropboxTransferService(context, client);
            FileMetadata uploaded = service.uploadFile(Uri.fromFile(input.toFile()),
                    "/demo_transfer_" + UUID.randomUUID() + ".txt");
            service.downloadFile(uploaded.getPathDisplay(), Uri.fromFile(output.toFile()));
            assertArrayEquals(expected, Files.readAllBytes(output));
            // Leave the uniquely named synthetic cloud file for the presenter's verification.
            // Never print account information, credentials or API response bodies.
        } finally { Files.deleteIfExists(input); Files.deleteIfExists(output); }
    }
}
