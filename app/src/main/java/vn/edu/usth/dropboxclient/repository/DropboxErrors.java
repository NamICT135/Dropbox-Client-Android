package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.AccessErrorException;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.UploadErrorException;
import vn.edu.usth.dropboxclient.R;

/** User-facing, bounded recovery guidance for common Dropbox failures. */
public final class DropboxErrors {
    private DropboxErrors() { }

    public static int message(DbxException error, int fallback) {
        if (error instanceof InvalidAccessTokenException) return R.string.project_expired;
        if (error instanceof NetworkIOException) return R.string.dropbox_network_error;
        if (error instanceof RateLimitException) return R.string.dropbox_rate_limit;
        if (error instanceof AccessErrorException
                && ((AccessErrorException) error).getAccessError().isNoPermission())
            return R.string.dropbox_scope_error;
        if (error instanceof DownloadErrorException) {
            DownloadErrorException e = (DownloadErrorException) error;
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound())
                return R.string.dropbox_not_found;
        }
        if (error instanceof ListFolderErrorException) {
            ListFolderErrorException e = (ListFolderErrorException) error;
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound())
                return R.string.dropbox_not_found;
        }
        if (error instanceof UploadErrorException) {
            UploadErrorException e = (UploadErrorException) error;
            if (e.errorValue.isPath() && e.errorValue.getPathValue().getReason().isInsufficientSpace())
                return R.string.dropbox_cloud_full;
        }
        return fallback;
    }
}
