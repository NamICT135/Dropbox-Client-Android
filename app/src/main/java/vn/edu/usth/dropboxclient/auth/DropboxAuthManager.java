package vn.edu.usth.dropboxclient.auth;

import android.app.Activity;
import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import java.util.Collections;
import vn.edu.usth.dropboxclient.BuildConfig;

public final class DropboxAuthManager {
    private DropboxAuthManager() { }
    public static boolean isConfigured() { return !BuildConfig.DROPBOX_APP_KEY.isEmpty(); }
    public static void start(Activity activity) {
        Auth.startOAuth2PKCE(activity, BuildConfig.DROPBOX_APP_KEY,
                DropboxClientFactory.requestConfig(), Collections.singletonList("account_info.read"));
    }
    public static DbxCredential result() { return Auth.getDbxCredential(); }
}
