package vn.edu.usth.dropboxclient.ui.folder;

import android.os.Handler;
import android.os.Looper;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.dropboxclient.repository.DropboxRepository;

public class DropboxFolderService {

    public interface FolderCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    private final DropboxRepository repository;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public DropboxFolderService(DropboxRepository repository, Handler mainHandler) {
        if (repository == null) {
            throw new IllegalArgumentException("DropboxRepository cannot be null");
        }
        this.repository = repository;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = mainHandler;
    }

    public DropboxFolderService(DropboxRepository repository) {
        this(repository, createDefaultHandler());
    }

    public DropboxFolderService(DbxClientV2 client) {
        this(new DropboxRepository(client));
    }

    private static Handler createDefaultHandler() {
        try {
            Looper looper = Looper.getMainLooper();
            return looper != null ? new Handler(looper) : null;
        } catch (RuntimeException ignored) {
            return null; // Local JVM unit tests where android.os.Looper is not mocked
        }
    }

    private void postCallback(Runnable action) {
        if (mainHandler != null) {
            mainHandler.post(action);
        } else {
            action.run();
        }
    }

    /**
     * Synchronously creates a folder on Dropbox.
     */
    public FolderMetadata createFolder(String parentPath, String folderName)
            throws DbxException, IllegalArgumentException {
        FolderValidationHelper.ValidationError error =
                FolderValidationHelper.validateFolderName(folderName);
        if (error != FolderValidationHelper.ValidationError.NONE) {
            throw new IllegalArgumentException("Invalid folder name: " + error);
        }

        String path = FolderValidationHelper.buildChildPath(parentPath, folderName);
        return repository.createFolder(path);
    }

    /**
     * Asynchronously creates a folder on a background worker thread.
     */
    public void createFolderAsync(
            String parentPath,
            String folderName,
            FolderCallback<FolderMetadata> callback
    ) {
        executor.execute(() -> {
            try {
                FolderMetadata result = createFolder(parentPath, folderName);
                if (callback != null) {
                    postCallback(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                if (callback != null) {
                    postCallback(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Synchronously renames a folder on Dropbox.
     */
    public Metadata renameFolder(String currentPath, String newName)
            throws DbxException, IllegalArgumentException {
        if (currentPath == null || currentPath.trim().isEmpty() || currentPath.trim().equals("/")) {
            throw new IllegalArgumentException("Cannot rename root directory");
        }

        String currentName = FolderValidationHelper.getNameFromPath(currentPath);
        FolderValidationHelper.ValidationError error =
                FolderValidationHelper.validateRename(currentName, newName);
        if (error != FolderValidationHelper.ValidationError.NONE) {
            throw new IllegalArgumentException("Invalid new folder name: " + error);
        }

        String parentPath = FolderValidationHelper.getParentPath(currentPath);
        String newPath = FolderValidationHelper.buildChildPath(parentPath, newName);
        return repository.renameFolder(currentPath, newPath);
    }

    /**
     * Asynchronously renames a folder on a background worker thread.
     */
    public void renameFolderAsync(
            String currentPath,
            String newName,
            FolderCallback<Metadata> callback
    ) {
        executor.execute(() -> {
            try {
                Metadata result = renameFolder(currentPath, newName);
                if (callback != null) {
                    postCallback(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                if (callback != null) {
                    postCallback(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Synchronously deletes a folder on Dropbox.
     */
    public Metadata deleteFolder(String path)
            throws DbxException, IllegalArgumentException {
        if (path == null || path.trim().isEmpty() || path.trim().equals("/")) {
            throw new IllegalArgumentException("Cannot delete root directory");
        }

        return repository.deleteFolder(path.trim());
    }

    /**
     * Asynchronously deletes a folder on a background worker thread.
     */
    public void deleteFolderAsync(
            String path,
            FolderCallback<Metadata> callback
    ) {
        executor.execute(() -> {
            try {
                Metadata result = deleteFolder(path);
                if (callback != null) {
                    postCallback(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                if (callback != null) {
                    postCallback(() -> callback.onError(e));
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
