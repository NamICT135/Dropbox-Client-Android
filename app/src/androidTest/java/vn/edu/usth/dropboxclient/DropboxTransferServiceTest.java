
package vn.edu.usth.dropboxclient;

import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import vn.edu.usth.dropboxclient.ui.transfer.DropboxTransferService;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DropboxTransferServiceTest {

    private DropboxTransferService service;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();

        DbxRequestConfig config =
                DbxRequestConfig.newBuilder("transfer-test").build();

        // Dummy client: no real Dropbox connection
        DbxClientV2 fakeClient =
                new DbxClientV2(config, "dummy-test-token");

        service = new DropboxTransferService(
                context, fakeClient
        );
    }

    // Test 1: Upload must reject null URI
    @Test
    public void upload_nullUri_shouldFail() throws Exception {
        try {
            service.uploadFile(null, "/test.txt");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Passed
        }
    }

    // Test 2: Upload must reject invalid Dropbox path
    @Test
    public void upload_invalidPath_shouldFail() throws Exception {
        try {
            service.uploadFile(
                    Uri.parse("content://test/file"),
                    "invalid_path"
            );
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Passed
        }
    }

    // Test 3: Download must reject null URI
    @Test
    public void download_nullUri_shouldFail() throws Exception {
        try {
            service.downloadFile("/test.txt", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Passed
        }
    }

    // Test 4: Download must reject invalid Dropbox path
    @Test
    public void download_invalidPath_shouldFail() throws Exception {
        try {
            service.downloadFile(
                    "invalid_path",
                    Uri.parse("content://test/file")
            );
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Passed
        }
    }
}
