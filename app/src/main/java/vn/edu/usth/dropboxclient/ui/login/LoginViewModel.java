package vn.edu.usth.dropboxclient.ui.login;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;

/** Keeps password derivation and its result across Activity recreation. */
public class LoginViewModel extends AndroidViewModel {
    public static final class State {
        final boolean busy;
        final boolean success;
        final int message;
        State(boolean busy, boolean success, int message) {
            this.busy = busy; this.success = success; this.message = message;
        }
    }
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(false, false, R.string.demo_login_help));
    public LoginViewModel(@NonNull Application application) { super(application); }
    public LiveData<State> state() { return state; }
    public boolean isBusy() { return state.getValue() != null && state.getValue().busy; }
    public void submit(boolean register, String user, char[] password,
                       String firstName, String lastName, String email) {
        if (isBusy()) { Arrays.fill(password, (char) 0); return; }
        state.setValue(new State(true, false, R.string.demo_working));
        io.execute(() -> {
            int message = R.string.demo_storage_error;
            boolean success = false;
            try {
                DemoAuthManager demo = new DemoAuthManager(getApplication());
                DemoAuthManager.Result result = register
                        ? demo.register(user, password, firstName, lastName, email)
                        : demo.login(user, password);
                success = result == DemoAuthManager.Result.SUCCESS;
                switch (result) {
                    case INVALID_USERNAME: message = R.string.demo_invalid_username; break;
                    case INVALID_PASSWORD: message = R.string.demo_invalid_password; break;
                    case USER_EXISTS: message = R.string.demo_user_exists; break;
                    case INVALID_PROFILE: message = R.string.profile_invalid; break;
                    case INVALID_CREDENTIALS: message = R.string.demo_invalid_credentials; break;
                    default: message = R.string.demo_notice;
                }
            } catch (GeneralSecurityException | IOException e) {
                message = R.string.demo_storage_error;
            } finally { Arrays.fill(password, (char) 0); }
            state.postValue(new State(false, success, message));
        });
    }
    @Override protected void onCleared() { io.shutdown(); }
}
