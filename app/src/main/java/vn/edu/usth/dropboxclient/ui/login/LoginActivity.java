package vn.edu.usth.dropboxclient.ui.login;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.TextView;
import com.dropbox.core.oauth.DbxCredential;
import vn.edu.usth.dropboxclient.auth.DropboxAuthManager;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.BuildConfig;
import java.security.GeneralSecurityException;
import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean awaitingAuth;
    private boolean busy;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.login_root),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        status = findViewById(R.id.login_status);
        awaitingAuth = savedInstanceState != null && savedInstanceState.getBoolean("awaiting_auth");
        findViewById(R.id.button_continue).setOnClickListener(view -> {
            if (!DropboxAuthManager.isConfigured()) {
                status.setText(R.string.auth_setup_required);
                return;
            }
            awaitingAuth = true;
            status.setText(R.string.auth_waiting);
            DropboxAuthManager.start(this);
        });
    }

    private void openMain() {
        if (isFinishing() || isDestroyed()) return;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override protected void onResume() {
        super.onResume();
        if (busy) return;
        DbxCredential result = awaitingAuth
                ? DropboxAuthManager.result() : null;
        boolean returned = awaitingAuth;
        awaitingAuth = false;
        busy = true;
        findViewById(R.id.button_continue).setEnabled(false);
        io.execute(() -> {
            boolean signedIn = false;
            int message = returned ? R.string.auth_cancelled : R.string.login_notice;
            SessionManager session =
                    new SessionManager(this);
            try {
                if (result != null) session.save(result);
                DbxCredential stored = session.load();
                signedIn = stored != null && !BuildConfig.DROPBOX_APP_KEY.isEmpty()
                        && BuildConfig.DROPBOX_APP_KEY.equals(stored.getAppKey());
                if (stored != null && !signedIn) session.clear();
            } catch (GeneralSecurityException | IOException e) {
                session.clear();
                message = R.string.session_error;
            }
            boolean ready = signedIn;
            int text = message;
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                busy = false;
                findViewById(R.id.button_continue).setEnabled(true);
                if (ready) openMain(); else status.setText(text);
            });
        });
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("awaiting_auth", awaitingAuth);
        super.onSaveInstanceState(outState);
    }
    @Override protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }
}
