package vn.edu.usth.dropboxclient.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.ui.main.MainActivity;

/** Device-local login; Dropbox authorization is offered separately from Account. */
public class LoginActivity extends AppCompatActivity {
    private EditText username, password, firstName, lastName, email;
    private TextView status;
    private LoginViewModel model;
    private boolean register;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        model = new ViewModelProvider(this).get(LoginViewModel.class);
        if (!model.isBusy() && new DemoAuthManager(this).currentUsername() != null) {
            openMain(); return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        username = findViewById(R.id.demo_username);
        password = findViewById(R.id.demo_password);
        firstName = findViewById(R.id.profile_first_name);
        lastName = findViewById(R.id.profile_last_name);
        email = findViewById(R.id.profile_email);
        status = findViewById(R.id.login_status);
        register = state != null && state.getBoolean("register");
        renderMode();
        findViewById(R.id.login_switch).setOnClickListener(v -> {
            if (model.isBusy()) return;
            register = !register;
            username.setError(null); password.setError(null); email.setError(null);
            status.setText(R.string.demo_login_help);
            renderMode();
        });
        findViewById(R.id.button_continue).setOnClickListener(v -> authenticate());
        findViewById(R.id.button_register).setOnClickListener(v -> authenticate());
        password.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) return false;
            authenticate(); return true;
        });
        OnBackPressedCallback back = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { /* Finish the pending local session write. */ }
        };
        getOnBackPressedDispatcher().addCallback(this, back);
        model.state().observe(this, result -> {
            back.setEnabled(result.busy);
            for (int id : new int[]{R.id.button_continue, R.id.button_register, R.id.login_switch,
                    R.id.demo_username, R.id.demo_password, R.id.profile_first_name,
                    R.id.profile_last_name, R.id.profile_email}) findViewById(id).setEnabled(!result.busy);
            findViewById(R.id.login_progress).setVisibility(result.busy ? View.VISIBLE : View.GONE);
            status.setText(result.message);
            if (result.success) { openMain(); return; }
            if (result.message == R.string.demo_user_exists || result.message == R.string.demo_invalid_username)
                username.setError(getString(result.message));
            if (result.message == R.string.demo_invalid_password || result.message == R.string.demo_invalid_credentials)
                password.setError(getString(result.message));
        });
    }
    private void renderMode() {
        ((TextView) findViewById(R.id.login_heading)).setText(register ? R.string.register_heading : R.string.login_heading);
        ((TextView) findViewById(R.id.login_switch)).setText(register ? R.string.register_switch : R.string.login_switch);
        ((TextView) findViewById(R.id.login_notice)).setText(register ? R.string.profile_note : R.string.demo_notice);
        findViewById(R.id.register_profile).setVisibility(register ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_register).setVisibility(register ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_continue).setVisibility(register ? View.GONE : View.VISIBLE);
    }
    private void authenticate() {
        if (model.isBusy()) return;
        username.setError(null); password.setError(null); email.setError(null);
        String user = username.getText().toString().trim();
        if (!user.matches("[a-zA-Z0-9_]{3,24}")) {
            username.setError(getString(R.string.demo_invalid_username));
            status.setText(R.string.demo_invalid_username); username.requestFocus(); return;
        }
        if (password.length() < 8 || password.length() > 128) {
            password.setError(getString(R.string.demo_invalid_password));
            status.setText(R.string.demo_invalid_password); password.requestFocus(); return;
        }
        String address = email.getText().toString().trim();
        if (register && !address.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
            email.setError(getString(R.string.profile_invalid_email));
            status.setText(R.string.profile_invalid_email); email.requestFocus(); return;
        }
        char[] secret = new char[password.length()];
        password.getText().getChars(0, password.length(), secret, 0);
        model.submit(register, user, secret, firstName.getText().toString(), lastName.getText().toString(), address);
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("register", register);
        super.onSaveInstanceState(outState);
    }
    private void openMain() {
        if (password != null) password.getText().clear();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    @Override protected void onDestroy() {
        if (password != null) password.getText().clear();
        super.onDestroy();
    }
}
