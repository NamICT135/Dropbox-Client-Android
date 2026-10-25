package vn.edu.usth.dropboxclient.auth;

import android.app.Activity;
import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import java.util.Arrays;
import vn.edu.usth.dropboxclient.BuildConfig;

public final class DropboxAuthManager {
    private DropboxAuthManager() { }
    public static boolean isConfigured() { return !BuildConfig.DROPBOX_APP_KEY.isEmpty(); }
    public static void start(Activity activity) {
        Auth.startOAuth2PKCE(activity, BuildConfig.DROPBOX_APP_KEY,
                DropboxClientFactory.requestConfig(), Arrays.asList("account_info.read",
                        "files.metadata.read", "files.content.read", "files.content.write"));
    }
    public static DbxCredential result() { return Auth.getDbxCredential(); }
}
