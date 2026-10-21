package vn.edu.usth.dropboxclient.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.users.FullAccount;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;

public class AccountFragment extends Fragment {
    private ExecutorService io;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int generation;
    public AccountFragment() { super(R.layout.fragment_account); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        io = Executors.newSingleThreadExecutor();
        SessionManager session = new SessionManager(requireContext());
        view.findViewById(R.id.account_retry).setOnClickListener(v -> load(view, session));
        view.findViewById(R.id.account_logout).setOnClickListener(v -> {
            generation++;
            session.clear();
            showLogin();
        });
        load(view, session);
    }
    private void load(View view, SessionManager session) {
        int request = ++generation;
        TextView info = view.findViewById(R.id.account_info);
        info.setText(R.string.account_loading);
        view.findViewById(R.id.account_retry).setEnabled(false);
        String accountFormat = getString(R.string.account_details);
        io.execute(() -> {
            String details = null;
            int error = R.string.account_error;
            boolean login = false;
            try {
                DbxCredential credential = session.load();
                if (credential == null) {
                    login = true;
                } else {
                    FullAccount account = new DropboxRepository(DropboxClientFactory.create(credential))
                            .getCurrentAccount();
                    // Refresh token remains stable; the SDK refreshes the in-memory access token.
                    details = String.format(accountFormat, account.getName().getDisplayName(),
                            account.getEmail(), account.getAccountId());
                }
            } catch (InvalidAccessTokenException e) {
                session.clear();
                login = true;
            } catch (DbxException e) {
                error = R.string.account_error;
            } catch (GeneralSecurityException | IOException e) {
                session.clear();
                error = R.string.session_error;
            }
            String result = details;
            int message = error;
            boolean needsLogin = login;
            main.post(() -> {
                if (request != generation || getView() != view) return;
                if (needsLogin) { showLogin(); return; }
                if (result != null) info.setText(result); else info.setText(message);
                view.findViewById(R.id.account_retry).setEnabled(true);
            });
        });
    }
    private void showLogin() {
        startActivity(new Intent(requireContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    @Override public void onDestroyView() {
        generation++;
        io.shutdownNow();
        super.onDestroyView();
    }
}
