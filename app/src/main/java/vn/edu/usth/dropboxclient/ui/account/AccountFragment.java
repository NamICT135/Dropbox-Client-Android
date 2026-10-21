package vn.edu.usth.dropboxclient.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.users.FullAccount;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.BuildConfig;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
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
        DemoAuthManager demo = new DemoAuthManager(requireContext());
        TextView user = view.findViewById(R.id.demo_account);
        user.setText(getString(R.string.demo_account_details, demo.currentUsername()));
        SessionManager cloud = new SessionManager(requireContext());
        view.findViewById(R.id.account_retry).setOnClickListener(v -> load(view, cloud));
        view.findViewById(R.id.account_connect).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProjectDropboxActivity.class)));
        view.findViewById(R.id.account_disconnect).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext()).setMessage(R.string.project_disconnect_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.project_disconnect, (dialog, which) -> {
                            generation++;
                            cloud.clear();
                            load(view, cloud);
                        }).show());
        view.findViewById(R.id.account_logout).setOnClickListener(v -> {
            generation++;
            demo.logout();
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }
    @Override public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) load(view, new SessionManager(requireContext()));
    }
    private void load(View view, SessionManager cloud) {
        int request = ++generation;
        TextView info = view.findViewById(R.id.account_info);
        info.setText(R.string.account_loading);
        view.findViewById(R.id.account_retry).setEnabled(false);
        view.findViewById(R.id.account_disconnect).setVisibility(View.GONE);
        String format = getString(R.string.project_account_details);
        io.execute(() -> {
            String details = null;
            int message = R.string.project_not_connected;
            boolean connected = false;
            try {
                DbxCredential credential = cloud.load();
                if (credential != null && !BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) {
                    cloud.clear();
                } else if (credential != null) {
                    connected = true;
                    FullAccount account = new DropboxRepository(DropboxClientFactory.create(credential))
                            .getCurrentAccount();
                    details = String.format(format, account.getName().getDisplayName());
                }
            } catch (InvalidAccessTokenException e) {
                cloud.clear();
                connected = false;
                message = R.string.project_expired;
            } catch (DbxException e) {
                message = R.string.account_error;
            } catch (GeneralSecurityException | IOException e) {
                cloud.clear();
                connected = false;
                message = R.string.session_error;
            }
            String result = details;
            int error = message;
            boolean hasConnection = connected;
            main.post(() -> {
                if (request != generation || getView() != view) return;
                if (result != null) info.setText(result); else info.setText(error);
                view.findViewById(R.id.account_retry).setEnabled(true);
                view.findViewById(R.id.account_disconnect).setVisibility(hasConnection ? View.VISIBLE : View.GONE);
            });
        });
    }
    @Override public void onDestroyView() {
        generation++;
        io.shutdownNow();
        super.onDestroyView();
    }
}
