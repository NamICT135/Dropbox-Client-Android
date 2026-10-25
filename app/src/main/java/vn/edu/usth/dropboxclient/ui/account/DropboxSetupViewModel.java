package vn.edu.usth.dropboxclient.ui.account;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.dropbox.core.DbxException;
import com.dropbox.core.oauth.DbxCredential;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.BuildConfig;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.DropboxConnection;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;

/** A single OAuth verification survives rotation without repeating the session write. */
public class DropboxSetupViewModel extends AndroidViewModel {
    static final class State {
        final boolean busy, connected;
        final int message;
        State(boolean busy, boolean connected, int message) {
            this.busy = busy; this.connected = connected; this.message = message;
        }
    }
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, false, 0));
    public DropboxSetupViewModel(@NonNull Application application) { super(application); }
    LiveData<State> state() { return state; }
    boolean isBusy() { return state.getValue() != null && state.getValue().busy; }
    void verify(DbxCredential credential, long revision) {
        if (isBusy()) return;
        state.setValue(new State(true, false, R.string.project_verifying));
        io.execute(() -> {
            int message = R.string.project_connection_error;
            boolean connected = false;
            SessionManager session = new SessionManager(getApplication());
            try {
                DropboxConnection.Result result = DropboxConnection.complete(credential,
                        BuildConfig.DROPBOX_APP_KEY, revision,
                        value -> new DropboxRepository(DropboxClientFactory.create(value)).getCurrentAccount(),
                        session::saveIfCurrent);
                connected = result == DropboxConnection.Result.CONNECTED;
                if (result == DropboxConnection.Result.STALE) message = R.string.auth_cancelled;
            } catch (DbxException | RuntimeException e) {
                message = R.string.project_connection_error;
            } catch (GeneralSecurityException | IOException e) {
                message = R.string.session_error;
            }
            state.postValue(new State(false, connected, message));
        });
    }
    @Override protected void onCleared() { io.shutdown(); }
}
