
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Context;

import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;

import java.io.IOException;
import java.security.GeneralSecurityException;

import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.BuildConfig;

public final class DropboxTransferFactory {

    private DropboxTransferFactory() {
        // Utility class
    }

    public static DropboxTransferService create(
            Context context
    ) throws GeneralSecurityException, IOException {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context cannot be null"
            );
        }

        SessionManager sessionManager =
                new SessionManager(context);

        DbxCredential credential = sessionManager.load();

        if (credential == null) {
            throw new IOException(
                    "Dropbox account is not connected"
            );
        }
        if (!BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) {
            throw new IOException("Reconnect Dropbox using this app configuration");
        }

        DbxClientV2 client =
                DropboxClientFactory.create(credential);

        return new DropboxTransferService(
                context.getApplicationContext(),
                client
        );
    }
}
