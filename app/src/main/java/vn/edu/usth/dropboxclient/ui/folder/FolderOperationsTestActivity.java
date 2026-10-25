package vn.edu.usth.dropboxclient.ui.folder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationErrorException;

import java.io.IOException;
import java.security.GeneralSecurityException;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;

public class FolderOperationsTestActivity extends AppCompatActivity
        implements CreateFolderDialogFragment.OnFolderCreateListener,
        RenameFolderDialogFragment.OnFolderRenameListener,
        DeleteFolderDialogFragment.OnFolderDeleteListener {

    private EditText editTargetPath;
    private Button btnCreateFolder;
    private Button btnRenameFolder;
    private Button btnDeleteFolder;
    private ProgressBar progressBar;
    private TextView textResult;

    private DropboxFolderService folderService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoAuthManager authManager = new DemoAuthManager(this);
        if (authManager.currentUsername() == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_folder_operations_test);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.folder_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        editTargetPath = findViewById(R.id.edit_target_path);
        btnCreateFolder = findViewById(R.id.btn_create_folder);
        btnRenameFolder = findViewById(R.id.btn_rename_folder);
        btnDeleteFolder = findViewById(R.id.btn_delete_folder);
        progressBar = findViewById(R.id.folder_progress);
        textResult = findViewById(R.id.text_folder_result);
        Button btnBack = findViewById(R.id.btn_folder_back);

        btnBack.setOnClickListener(v -> finish());

        initService();

        btnCreateFolder.setOnClickListener(v -> {
            String currentPath = getTargetPath();
            CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(currentPath);
            dialog.setOnFolderCreateListener(this);
            dialog.show(getSupportFragmentManager(), "CreateFolderDialog");
        });

        btnRenameFolder.setOnClickListener(v -> {
            String currentPath = getTargetPath();
            if (currentPath.equals("/") || currentPath.isEmpty()) {
                showResult("Cannot rename the root directory. Enter a folder path first.");
                return;
            }
            String currentName = FolderValidationHelper.getNameFromPath(currentPath);
            RenameFolderDialogFragment dialog =
                    RenameFolderDialogFragment.newInstance(currentPath, currentName);
            dialog.setOnFolderRenameListener(this);
            dialog.show(getSupportFragmentManager(), "RenameFolderDialog");
        });

        btnDeleteFolder.setOnClickListener(v -> {
            String currentPath = getTargetPath();
            if (currentPath.equals("/") || currentPath.isEmpty()) {
                showResult("Cannot delete the root directory. Enter a folder path first.");
                return;
            }
            String currentName = FolderValidationHelper.getNameFromPath(currentPath);
            DeleteFolderDialogFragment dialog =
                    DeleteFolderDialogFragment.newInstance(currentPath, currentName);
            dialog.setOnFolderDeleteListener(this);
            dialog.show(getSupportFragmentManager(), "DeleteFolderDialog");
        });
    }

    private void initService() {
        try {
            folderService = DropboxFolderFactory.create(this);
        } catch (GeneralSecurityException | IOException e) {
            showResult(getString(R.string.transfer_session_error));
        }
    }

    private String getTargetPath() {
        if (editTargetPath.getText() == null) return "/";
        String path = editTargetPath.getText().toString().trim();
        return path.isEmpty() ? "/" : path;
    }

    private void setBusy(boolean busy) {
        btnCreateFolder.setEnabled(!busy);
        btnRenameFolder.setEnabled(!busy);
        btnDeleteFolder.setEnabled(!busy);
        editTargetPath.setEnabled(!busy);
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void showResult(String message) {
        runOnUiThread(() -> textResult.setText(message));
    }

    @Override
    public void onFolderCreateConfirmed(String parentPath, String folderName) {
        if (folderService == null) {
            initService();
            if (folderService == null) return;
        }

        setBusy(true);
        showResult(getString(R.string.folder_action_creating));

        folderService.createFolderAsync(parentPath, folderName, new DropboxFolderService.FolderCallback<FolderMetadata>() {
            @Override
            public void onSuccess(FolderMetadata result) {
                setBusy(false);
                String fullPath = result.getPathDisplay();
                editTargetPath.setText(fullPath);
                showResult(getString(R.string.folder_create_success, result.getName())
                        + "\nPath: " + fullPath
                        + "\nID: " + result.getId());
            }

            @Override
            public void onError(Exception error) {
                setBusy(false);
                handleError(error);
            }
        });
    }

    @Override
    public void onFolderRenameConfirmed(String currentPath, String newName) {
        if (folderService == null) {
            initService();
            if (folderService == null) return;
        }

        setBusy(true);
        showResult(getString(R.string.folder_action_renaming));

        folderService.renameFolderAsync(currentPath, newName, new DropboxFolderService.FolderCallback<Metadata>() {
            @Override
            public void onSuccess(Metadata result) {
                setBusy(false);
                String fullPath = result.getPathDisplay();
                editTargetPath.setText(fullPath);
                showResult(getString(R.string.folder_rename_success, result.getName())
                        + "\nPath: " + fullPath);
            }

            @Override
            public void onError(Exception error) {
                setBusy(false);
                handleError(error);
            }
        });
    }

    @Override
    public void onFolderDeleteConfirmed(String path) {
        if (folderService == null) {
            initService();
            if (folderService == null) return;
        }

        setBusy(true);
        showResult(getString(R.string.folder_action_deleting));

        folderService.deleteFolderAsync(path, new DropboxFolderService.FolderCallback<Metadata>() {
            @Override
            public void onSuccess(Metadata result) {
                setBusy(false);
                editTargetPath.setText("/");
                showResult(getString(R.string.folder_delete_success, result.getName()));
            }

            @Override
            public void onError(Exception error) {
                setBusy(false);
                handleError(error);
            }
        });
    }

    private void handleError(Exception error) {
        if (error instanceof CreateFolderErrorException) {
            showResult(getString(R.string.folder_conflict_error));
        } else if (error instanceof RelocationErrorException) {
            showResult(getString(R.string.folder_conflict_error));
        } else if (error instanceof DeleteErrorException) {
            showResult(getString(R.string.folder_not_found_error));
        } else if (error instanceof DbxException) {
            showResult(getString(R.string.folder_generic_error, error.getMessage()));
        } else {
            showResult(getString(R.string.folder_generic_error,
                    error != null ? error.getMessage() : "Unknown error"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (folderService != null) {
            folderService.shutdown();
        }
    }
}
