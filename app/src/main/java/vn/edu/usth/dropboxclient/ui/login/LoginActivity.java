package vn.edu.usth.dropboxclient.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.ui.main.MainActivity;

/** Demo login is local and independent from the project Dropbox connection. */
public class LoginActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private EditText username;
    private EditText password;
    private TextView status;
    private DemoAuthManager demo;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        demo = new DemoAuthManager(this);
        if (demo.currentUsername() != null) { openMain(); return; }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        username = findViewById(R.id.demo_username);
        password = findViewById(R.id.demo_password);
        status = findViewById(R.id.login_status);
        findViewById(R.id.button_continue).setOnClickListener(v -> authenticate(false));
        findViewById(R.id.button_register).setOnClickListener(v -> authenticate(true));
    }

    private void authenticate(boolean register) {
        if (busy) return;
        username.setError(null);
        password.setError(null);
        String user = username.getText().toString();
        if (user.trim().isEmpty()) {
            username.setError(getString(R.string.demo_invalid_username));
            status.setText(R.string.demo_invalid_username);
            username.requestFocus();
            return;
        }
        if (password.length() < 8 || password.length() > 128) {
            password.setError(getString(R.string.demo_invalid_password));
            status.setText(R.string.demo_invalid_password);
            password.requestFocus();
            return;
        }
        char[] secret = new char[password.length()];
        password.getText().getChars(0, password.length(), secret, 0);
        busy = true;
        setButtonsEnabled(false);
        status.setText(R.string.demo_working);
        io.execute(() -> {
            int message;
            boolean success = false;
            try {
                DemoAuthManager.Result result = register ? demo.register(user, secret) : demo.login(user, secret);
                success = result == DemoAuthManager.Result.SUCCESS;
                switch (result) {
                    case INVALID_USERNAME: message = R.string.demo_invalid_username; break;
                    case INVALID_PASSWORD: message = R.string.demo_invalid_password; break;
                    case USER_EXISTS: message = R.string.demo_user_exists; break;
                    case INVALID_CREDENTIALS: message = R.string.demo_invalid_credentials; break;
                    default: message = R.string.demo_notice;
                }
            } catch (GeneralSecurityException | IOException e) {
                message = R.string.demo_storage_error;
            } finally { Arrays.fill(secret, (char) 0); }
            boolean ready = success;
            int text = message;
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                busy = false;
                setButtonsEnabled(true);
                if (ready) {
                    openMain();
                } else {
                    status.setText(text);
                    if (text == R.string.demo_invalid_username || text == R.string.demo_user_exists) {
                        username.setError(getString(text));
                    } else if (text == R.string.demo_invalid_password
                            || text == R.string.demo_invalid_credentials) {
                        password.setError(getString(text));
                    }
                }
            });
        });
    }
    private void setButtonsEnabled(boolean enabled) {
        findViewById(R.id.button_continue).setEnabled(enabled);
        findViewById(R.id.button_register).setEnabled(enabled);
        username.setEnabled(enabled);
        password.setEnabled(enabled);
    }
    private void openMain() {
        if (password != null) password.getText().clear();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    @Override protected void onDestroy() {
        if (password != null) password.getText().clear();
        io.shutdown();
        super.onDestroy();
    }
}
