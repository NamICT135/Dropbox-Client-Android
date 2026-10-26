package vn.edu.usth.dropboxclient.ui.transfer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.ui.account.ProjectDropboxActivity;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;

/** Foreground file transfer using Android document pickers and the shared Dropbox connection. */
public class TransferActivity extends AppCompatActivity {
    public static final String EXTRA_FOLDER_PATH = "folder_path";
    public static final String EXTRA_DOWNLOAD_PATH = "download_path";
    public static final String EXTRA_OPEN_PREVIEW = "open_preview";
    private static final String KEY_PENDING_PATH = "pending_download_path";
    private static final String KEY_PICKER_PENDING = "picker_pending";

    private TransferViewModel model;
    private EditText dropboxPathInput;
    private String folderPath;
    private String pendingDownloadPath;
    private boolean pickerPending;

    private final ActivityResultLauncher<Intent> uploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                pickerPending = false;
                renderBusy();
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null
                        || result.getData().getData() == null) {
                    model.showMessage(R.string.transfer_cancelled);
                    return;
                }
                model.upload(result.getData().getData(), folderPath);
            });

    private final ActivityResultLauncher<Intent> downloadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                String path = pendingDownloadPath;
                pendingDownloadPath = null;
                pickerPending = false;
                renderBusy();
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null
                        || result.getData().getData() == null) {
                    model.showMessage(R.string.transfer_cancelled);
                    return;
                }
                if (path == null) {
                    model.showMessage(R.string.transfer_invalid_path);
                    return;
                }
                model.download(path, result.getData().getData());
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (new DemoAuthManager(this).currentUsername() == null) {
            openLogin();
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transfer_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        model = new ViewModelProvider(this).get(TransferViewModel.class);
        String initialPath = getIntent().getStringExtra(EXTRA_DOWNLOAD_PATH);
        folderPath = getIntent().getStringExtra(EXTRA_FOLDER_PATH);
        if (folderPath == null) {
            int slash = initialPath == null ? -1 : initialPath.lastIndexOf('/');
            folderPath = slash <= 0 ? "" : initialPath.substring(0, slash);
        }
        dropboxPathInput = findViewById(R.id.transfer_path);
        if (state != null) {
            pendingDownloadPath = state.getString(KEY_PENDING_PATH);
            pickerPending = state.getBoolean(KEY_PICKER_PENDING);
        } else if (initialPath != null) {
            dropboxPathInput.setText(initialPath);
        }
        ((TextView) findViewById(R.id.transfer_folder)).setText(getString(
                R.string.transfer_folder, folderPath.isEmpty() ? "/" : folderPath));
        findViewById(R.id.transfer_upload).setOnClickListener(v -> {
            if (model.isBusy() || pickerPending) return;
            pickerPending = true;
            renderBusy();
            try {
                uploadLauncher.launch(FilePickerHelper.createUploadIntent());
            } catch (ActivityNotFoundException e) {
                pickerPending = false;
                model.showMessage(R.string.transfer_picker_error);
            }
        });
        findViewById(R.id.transfer_download).setOnClickListener(v -> {
            if (model.isBusy() || pickerPending) return;
            String path = checkedPath();
            if (path == null) return;
            pendingDownloadPath = path;
            pickerPending = true;
            renderBusy();
            try {
                downloadLauncher.launch(FilePickerHelper.createDownloadIntent(
                        path.substring(path.lastIndexOf('/') + 1)));
            } catch (ActivityNotFoundException e) {
                pendingDownloadPath = null;
                pickerPending = false;
                model.showMessage(R.string.transfer_picker_error);
            }
        });
        findViewById(R.id.transfer_preview).setOnClickListener(v -> {
            if (model.isBusy() || pickerPending) return;
            String path = checkedPath();
            if (path != null) model.preview(path);
        });
        findViewById(R.id.transfer_setup).setOnClickListener(v ->
                startActivity(new Intent(this, ProjectDropboxActivity.class)));
        findViewById(R.id.transfer_back).setOnClickListener(v -> finish());
        OnBackPressedCallback busyBack = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                Toast.makeText(TransferActivity.this, R.string.transfer_stay_open,
                        Toast.LENGTH_SHORT).show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, busyBack);
        model.state().observe(this, result -> {
            ((TextView) findViewById(R.id.transfer_result)).setText(result.message);
            if (result.uploadedPath != null) dropboxPathInput.setText(result.uploadedPath);
            busyBack.setEnabled(result.busy);
            renderBusy();
            if (result.previewFile != null) {
                model.consumePreview();
                openPreview(result);
            }
        });
        renderBusy();
        if (state == null && initialPath != null
                && getIntent().getBooleanExtra(EXTRA_OPEN_PREVIEW, false))
            model.preview(initialPath);
    }

    private String checkedPath() {
        String path = dropboxPathInput.getText().toString().trim();
        if (path.startsWith("/") && path.length() > 1 && !path.endsWith("/")) return path;
        dropboxPathInput.setError(getString(R.string.transfer_invalid_path));
        model.showMessage(R.string.transfer_invalid_path);
        return null;
    }

    private void openPreview(TransferViewModel.State result) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".preview", result.previewFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, result.previewMime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            model.showMessage(R.string.preview_no_viewer);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (new DemoAuthManager(this).currentUsername() == null) openLogin();
    }

    private void renderBusy() {
        if (model == null) return;
        boolean enabled = !model.isBusy() && !pickerPending;
        findViewById(R.id.transfer_upload).setEnabled(enabled);
        findViewById(R.id.transfer_download).setEnabled(enabled);
        findViewById(R.id.transfer_preview).setEnabled(enabled);
        findViewById(R.id.transfer_setup).setEnabled(enabled);
        findViewById(R.id.transfer_back).setEnabled(enabled);
        dropboxPathInput.setEnabled(enabled);
        findViewById(R.id.transfer_progress).setVisibility(
                model.isBusy() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString(KEY_PENDING_PATH, pendingDownloadPath);
        state.putBoolean(KEY_PICKER_PENDING, pickerPending);
        super.onSaveInstanceState(state);
    }
}
