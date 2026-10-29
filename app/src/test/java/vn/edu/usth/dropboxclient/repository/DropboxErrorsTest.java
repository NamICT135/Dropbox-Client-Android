package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.NetworkIOException;
import com.dropbox.core.AccessErrorException;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.files.DownloadError;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.LookupError;
import com.dropbox.core.v2.files.UploadError;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadWriteFailed;
import com.dropbox.core.v2.files.WriteError;
import com.dropbox.core.v2.auth.AccessError;
import com.dropbox.core.v2.auth.NoPermissionError;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import vn.edu.usth.dropboxclient.R;
import static org.junit.Assert.assertEquals;

public class DropboxErrorsTest {
    @Test public void mapsNetworkRateLimitAndMissingFile() {
        assertEquals(R.string.dropbox_network_error, DropboxErrors.message(
                new NetworkIOException(new IOException("offline")), R.string.transfer_api_error));
        assertEquals(R.string.dropbox_rate_limit, DropboxErrors.message(
                new RateLimitException("request", "slow down", 2, TimeUnit.SECONDS),
                R.string.transfer_api_error));
        assertEquals(R.string.dropbox_not_found, DropboxErrors.message(
                new DownloadErrorException("request", "missing", null,
                        DownloadError.path(LookupError.NOT_FOUND)), R.string.transfer_api_error));
        assertEquals(R.string.dropbox_scope_error, DropboxErrors.message(
                new AccessErrorException("request", "scope", AccessError.noPermission(NoPermissionError.OTHER)),
                R.string.transfer_api_error));
        assertEquals(R.string.dropbox_cloud_full, DropboxErrors.message(
                new UploadErrorException("request", "full", null,
                        UploadError.path(new UploadWriteFailed(WriteError.INSUFFICIENT_SPACE, "test-session"))),
                R.string.transfer_api_error));
    }
}
