package vn.edu.usth.dropboxclient.ui.account;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.dropbox.core.DbxException;
import com.dropbox.core.oauth.DbxCredential;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.BuildConfig;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.auth.DropboxAuthManager;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;

/** One project-account OAuth setup per device, separate from demo user login. */
public class ProjectDropboxActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean awaitingAuth;
    private boolean busy;
    private TextView status;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (new DemoAuthManager(this).currentUsername() == null) {
            startActivity(new Intent(this, LoginActivity.class)); finish(); return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_project_dropbox);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.project_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        status = findViewById(R.id.project_status);
        awaitingAuth = state != null && state.getBoolean("awaiting_auth");
        findViewById(R.id.project_connect).setOnClickListener(v -> {
            if (busy || awaitingAuth) return;
            if (!DropboxAuthManager.isConfigured()) { status.setText(R.string.auth_setup_required); return; }
            try {
                awaitingAuth = true;
                status.setText(R.string.auth_waiting);
                DropboxAuthManager.start(this);
            } catch (ActivityNotFoundException | IllegalStateException e) {
                awaitingAuth = false;
                status.setText(R.string.project_launch_error);
            }
        });
        findViewById(R.id.project_back).setOnClickListener(v -> finish());
    }
    @Override protected void onResume() {
        super.onResume();
        if (status == null || !awaitingAuth || busy) return;
        awaitingAuth = false;
        DbxCredential credential = DropboxAuthManager.result();
        if (credential == null) { status.setText(R.string.auth_cancelled); return; }
        busy = true;
        findViewById(R.id.project_connect).setEnabled(false);
        status.setText(R.string.project_verifying);
        io.execute(() -> {
            int message = R.string.project_connection_error;
            boolean connected = false;
            try {
                if (!BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey()))
                    throw new IOException("Unexpected Dropbox application");
                new DropboxRepository(DropboxClientFactory.create(credential)).getCurrentAccount();
                // Do not replace the existing connection when authorization or verification fails.
                new SessionManager(this).save(credential);
                connected = true;
            } catch (DbxException e) {
                message = R.string.project_connection_error;
            } catch (GeneralSecurityException | IOException e) {
                message = R.string.session_error;
            }
            boolean ready = connected;
            int text = message;
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                busy = false;
                findViewById(R.id.project_connect).setEnabled(true);
                if (ready) finish(); else status.setText(text);
            });
        });
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("awaiting_auth", awaitingAuth);
        super.onSaveInstanceState(outState);
    }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }
}
