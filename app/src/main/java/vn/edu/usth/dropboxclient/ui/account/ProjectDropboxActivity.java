package vn.edu.usth.dropboxclient.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.dropbox.core.oauth.DbxCredential;
import java.io.IOException;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.auth.DropboxAuthManager;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;

/** One project-account OAuth setup per device, separate from demo user login. */
public class ProjectDropboxActivity extends AppCompatActivity {
    private DropboxSetupViewModel model;
    private boolean awaitingAuth;
    private long authRevision;
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
        model = new ViewModelProvider(this).get(DropboxSetupViewModel.class);
        status = findViewById(R.id.project_status);
        OnBackPressedCallback back = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { /* Keep verification visible until it completes. */ }
        };
        getOnBackPressedDispatcher().addCallback(this, back);
        model.state().observe(this, result -> {
            back.setEnabled(result.busy);
            findViewById(R.id.project_connect).setEnabled(!result.busy);
            findViewById(R.id.project_back).setEnabled(!result.busy);
            if (result.connected) { finish(); return; }
            if (result.message != 0) status.setText(result.message);
        });
        awaitingAuth = state != null && state.getBoolean("awaiting_auth");
        authRevision = state == null ? -1 : state.getLong("auth_revision", -1);
        findViewById(R.id.project_connect).setOnClickListener(v -> {
            if (model.isBusy() || awaitingAuth) return;
            if (!DropboxAuthManager.isConfigured()) { status.setText(R.string.auth_setup_required); return; }
            try {
                authRevision = new SessionManager(this).beginOAuth();
                awaitingAuth = true;
                status.setText(R.string.auth_waiting);
                DropboxAuthManager.start(this);
            } catch (IOException e) {
                awaitingAuth = false;
                status.setText(R.string.session_error);
            } catch (RuntimeException e) {
                awaitingAuth = false;
                status.setText(R.string.project_launch_error);
            }
        });
        findViewById(R.id.project_back).setOnClickListener(v -> finish());
    }
    @Override protected void onResume() {
        super.onResume();
        if (status == null) return;
        if (new DemoAuthManager(this).currentUsername() == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        if (!awaitingAuth || model.isBusy()) return;
        awaitingAuth = false;
        SessionManager session = new SessionManager(this);
        if (session.revision() != authRevision) {
            status.setText(R.string.auth_cancelled);
            return;
        }
        DbxCredential credential;
        try { credential = DropboxAuthManager.result(); }
        catch (RuntimeException e) { status.setText(R.string.auth_cancelled); return; }
        if (credential == null) { status.setText(R.string.auth_cancelled); return; }
        model.verify(credential, authRevision);
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("awaiting_auth", awaitingAuth);
        outState.putLong("auth_revision", authRevision);
        super.onSaveInstanceState(outState);
    }
}
