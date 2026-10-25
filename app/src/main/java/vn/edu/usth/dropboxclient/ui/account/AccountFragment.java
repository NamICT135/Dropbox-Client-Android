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
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.userscommon.AccountType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.BuildConfig;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.DropboxConnection;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;
import vn.edu.usth.dropboxclient.ui.transfer.TransferTestActivity;

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
        ((TextView) view.findViewById(R.id.local_profile)).setText(getString(
                R.string.account_profile_details, demo.currentDisplayName(),
                demo.currentEmail().isEmpty() ? getString(R.string.account_email_unset) : demo.currentEmail()));
        SessionManager cloud = new SessionManager(requireContext());
        if (BuildConfig.DEBUG) {
            view.findViewById(R.id.account_transfer_debug).setVisibility(View.VISIBLE);
            view.findViewById(R.id.account_transfer_debug).setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), TransferTestActivity.class)));
        }
        view.findViewById(R.id.account_retry).setOnClickListener(v -> load(view, cloud));
        view.findViewById(R.id.account_connect).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProjectDropboxActivity.class)));
        view.findViewById(R.id.account_disconnect).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext()).setMessage(R.string.project_disconnect_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.project_disconnect, (dialog, which) -> {
                            generation++;
                            if (cloud.clear()) load(view, cloud);
                            else showSessionError(view);
                        }).show());
        view.findViewById(R.id.account_logout).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext()).setMessage(R.string.account_logout_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.account_logout, (dialog, which) -> {
                            generation++;
                            if (!cloud.clear() || !demo.logout()) {
                                showSessionError(view);
                                return;
                            }
                            startActivity(new Intent(requireContext(), LoginActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }).show());
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
        view.findViewById(R.id.account_retry).setVisibility(View.GONE);
        view.findViewById(R.id.account_disconnect).setVisibility(View.GONE);
        view.findViewById(R.id.account_details).setVisibility(View.GONE);
        view.findViewById(R.id.account_progress).setVisibility(View.VISIBLE);
        io.execute(() -> {
            FullAccount details = null;
            int message = R.string.project_not_connected;
            boolean hasCredential = false;
            boolean retry = false;
            long revision = cloud.revision();
            boolean cleared = false;
            try {
                DbxCredential credential = cloud.load();
                if (credential != null && !BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) {
                    cleared = cloud.clearIfCurrent(revision);
                    message = cleared ? R.string.project_expired : R.string.session_error;
                    retry = !cleared;
                } else if (credential != null) {
                    hasCredential = true;
                    FullAccount account = new DropboxRepository(DropboxClientFactory.create(credential))
                            .getCurrentAccount();
                    details = account;
                }
            } catch (DbxException e) {
                if (DropboxConnection.isAuthorizationFailure(e)) {
                    cleared = cloud.clearIfCurrent(revision);
                    hasCredential = false;
                    message = cleared ? R.string.project_expired : R.string.session_error;
                    retry = !cleared;
                } else {
                    message = R.string.account_error;
                    retry = true;
                }
            } catch (GeneralSecurityException | IOException e) {
                cleared = cloud.clearIfCurrent(revision);
                hasCredential = false;
                message = R.string.session_error;
                retry = true;
            } catch (RuntimeException e) {
                message = R.string.account_error;
                retry = true;
            }
            FullAccount result = details;
            int error = message;
            boolean canDisconnect = hasCredential;
            boolean canRetry = retry || details != null;
            long completedRevision = revision + (cleared ? 1 : 0);
            main.post(() -> {
                if (request != generation || getView() != view
                        || new DemoAuthManager(requireContext()).currentUsername() == null) {
                    return;
                }
                if (cloud.revision() != completedRevision) {
                    load(view, cloud);
                    return;
                }
                view.findViewById(R.id.account_progress).setVisibility(View.GONE);
                if (result != null) {
                    info.setText(R.string.account_connected);
                    view.findViewById(R.id.account_details).setVisibility(View.VISIBLE);
                    ((TextView) view.findViewById(R.id.cloud_name)).setText(result.getName().getDisplayName());
                    ((TextView) view.findViewById(R.id.cloud_email)).setText(getString(
                            result.getEmailVerified() ? R.string.account_email_verified : R.string.account_email_unverified,
                            result.getEmail()));
                    int plan = result.getAccountType() == AccountType.BASIC ? R.string.account_plan_basic
                            : result.getAccountType() == AccountType.PRO ? R.string.account_plan_pro
                            : result.getAccountType() == AccountType.BUSINESS ? R.string.account_plan_business : R.string.account_plan_other;
                    ((TextView) view.findViewById(R.id.cloud_plan)).setText(plan);
                    ((TextView) view.findViewById(R.id.cloud_id)).setText(result.getAccountId());
                } else info.setText(error);
                ((TextView) view.findViewById(R.id.account_connect)).setText(
                        canDisconnect ? R.string.account_reconnect : R.string.project_setup);
                view.findViewById(R.id.account_retry).setEnabled(true);
                view.findViewById(R.id.account_retry).setVisibility(canRetry ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.account_disconnect).setVisibility(canDisconnect ? View.VISIBLE : View.GONE);
            });
        });
    }
    private void showSessionError(View view) {
        view.findViewById(R.id.account_progress).setVisibility(View.GONE);
        view.findViewById(R.id.account_details).setVisibility(View.GONE);
        view.findViewById(R.id.account_retry).setVisibility(View.VISIBLE);
        view.findViewById(R.id.account_retry).setEnabled(true);
        ((TextView) view.findViewById(R.id.account_info)).setText(R.string.session_error);
    }
    @Override public void onDestroyView() {
        generation++;
        io.shutdownNow();
        super.onDestroyView();
    }
}
