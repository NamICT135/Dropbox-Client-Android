package vn.edu.usth.dropboxclient.ui.transfer;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.ui.folder.FolderValidationHelper;

/** Keeps a foreground transfer and its result through screen rotation. */
public class TransferViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean busy;
        public final String message;
        public final String uploadedPath;

        State(boolean busy, String message, String uploadedPath) {
            this.busy = busy;
            this.message = message;
            this.uploadedPath = uploadedPath;
        }
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state;

    public TransferViewModel(@NonNull Application application) {
        super(application);
        state = new MutableLiveData<>(new State(false,
                application.getString(R.string.transfer_ready), null));
    }

    public LiveData<State> state() { return state; }
    public boolean isBusy() { return state.getValue() != null && state.getValue().busy; }

    public void showMessage(int message) {
        State current = state.getValue();
        state.setValue(new State(false, getApplication().getString(message),
                current == null ? null : current.uploadedPath));
    }

    public void upload(Uri source, String folderPath) {
        if (isBusy()) return;
        state.setValue(new State(true, getApplication().getString(R.string.transfer_uploading), null));
        io.execute(() -> {
            String message;
            String uploadedPath = null;
            try {
                Application app = getApplication();
                DropboxTransferService service;
                try {
                    service = DropboxTransferFactory.create(app);
                } catch (GeneralSecurityException | IOException e) {
                    state.postValue(new State(false,
                            app.getString(R.string.transfer_session_error), null));
                    return;
                }
                String name = FileInfoHelper.getFileName(app, source);
                String path = FolderValidationHelper.buildChildPath(folderPath, name);
                FileMetadata file = service.uploadFile(source, path);
                uploadedPath = file.getPathDisplay();
                message = app.getString(R.string.transfer_upload_success,
                        file.getName(), uploadedPath, file.getSize());
            } catch (DbxException e) {
                message = getApplication().getString(R.string.transfer_api_error);
            } catch (IOException | SecurityException e) {
                message = getApplication().getString(R.string.transfer_file_error);
            } catch (IllegalArgumentException e) {
                message = getApplication().getString(R.string.transfer_invalid_name);
            }
            state.postValue(new State(false, message, uploadedPath));
        });
    }

    public void download(String path, Uri destination) {
        if (isBusy()) return;
        state.setValue(new State(true, getApplication().getString(R.string.transfer_downloading), null));
        io.execute(() -> {
            String message;
            try {
                Application app = getApplication();
                DropboxTransferService service;
                try {
                    service = DropboxTransferFactory.create(app);
                } catch (GeneralSecurityException | IOException e) {
                    state.postValue(new State(false,
                            app.getString(R.string.transfer_session_error), null));
                    return;
                }
                FileMetadata file = service.downloadFile(path, destination);
                message = getApplication().getString(R.string.transfer_download_success,
                        file.getName(), file.getSize());
            } catch (DbxException e) {
                message = getApplication().getString(R.string.transfer_api_error);
            } catch (IOException | SecurityException e) {
                message = getApplication().getString(R.string.transfer_file_error);
            } catch (IllegalArgumentException e) {
                message = getApplication().getString(R.string.transfer_invalid_path);
            }
            state.postValue(new State(false, message, null));
        });
    }

    @Override protected void onCleared() {
        io.shutdown();
    }
}
