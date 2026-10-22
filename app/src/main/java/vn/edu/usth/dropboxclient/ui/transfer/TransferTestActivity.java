package vn.edu.usth.dropboxclient.ui.transfer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.ui.account.ProjectDropboxActivity;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;

/** Debug acceptance screen using the shared project connection and Android document pickers. */
public class TransferTestActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView resultText;
    private EditText dropboxPathInput;
    private boolean busy;
    private String pendingDownloadPath;

    private final ActivityResultLauncher<Intent> uploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null
                        || result.getData().getData() == null) {
                    showResult(R.string.transfer_cancelled); setBusy(false); return;
                }
                Uri uri = result.getData().getData();
                setBusy(true);
                showResult(R.string.transfer_uploading);
                executor.execute(() -> {
                    try {
                        DropboxTransferService service;
                        try { service = DropboxTransferFactory.create(this); }
                        catch (GeneralSecurityException | IOException e) {
                            showResult(R.string.transfer_session_error); return;
                        }
                        // Provider queries and streams both belong on the worker thread.
                        String name = FileInfoHelper.getFileName(this, uri);
                        FileMetadata metadata = service.uploadFile(uri, "/test_upload_" + name);
                        runOnUiThread(() -> {
                            if (isDestroyed() || isFinishing()) return;
                            dropboxPathInput.setText(metadata.getPathDisplay());
                            resultText.setText(getString(R.string.transfer_upload_success,
                                    metadata.getName(), metadata.getPathDisplay(), metadata.getSize()));
                        });
                    } catch (DbxException e) {
                        showResult(R.string.transfer_api_error);
                    } catch (IOException | SecurityException | IllegalArgumentException e) {
                        showResult(R.string.transfer_file_error);
                    } finally { setBusy(false); }
                });
            });

    private final ActivityResultLauncher<Intent> downloadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                String path = pendingDownloadPath;
                pendingDownloadPath = null;
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null
                        || result.getData().getData() == null) {
                    showResult(R.string.transfer_cancelled); setBusy(false); return;
                }
                if (path == null) {
                    showResult(R.string.transfer_invalid_path); setBusy(false); return;
                }
                Uri uri = result.getData().getData();
                setBusy(true);
                showResult(R.string.transfer_downloading);
                executor.execute(() -> {
                    try {
                        DropboxTransferService service;
                        try { service = DropboxTransferFactory.create(this); }
                        catch (GeneralSecurityException | IOException e) {
                            showResult(R.string.transfer_session_error); return;
                        }
                        FileMetadata metadata = service.downloadFile(path, uri);
                        runOnUiThread(() -> {
                            if (!isDestroyed() && !isFinishing()) resultText.setText(getString(
                                    R.string.transfer_download_success, metadata.getName(), metadata.getSize()));
                        });
                    } catch (DbxException e) {
                        showResult(R.string.transfer_api_error);
                    } catch (IOException | SecurityException | IllegalArgumentException e) {
                        showResult(R.string.transfer_file_error);
                    } finally { setBusy(false); }
                });
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (new DemoAuthManager(this).currentUsername() == null) {
            startActivity(new Intent(this, LoginActivity.class)); finish(); return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transfer_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        resultText = findViewById(R.id.transfer_result);
        dropboxPathInput = findViewById(R.id.transfer_path);
        if (state != null) pendingDownloadPath = state.getString("pending_download_path");
        findViewById(R.id.transfer_upload).setOnClickListener(v -> {
            if (busy) return;
            setBusy(true);
            uploadLauncher.launch(FilePickerHelper.createUploadIntent());
        });
        findViewById(R.id.transfer_download).setOnClickListener(v -> {
            if (busy) return;
            String path = dropboxPathInput.getText().toString().trim();
            if (!path.startsWith("/") || path.length() <= 1 || path.endsWith("/")) {
                dropboxPathInput.setError(getString(R.string.transfer_invalid_path));
                showResult(R.string.transfer_invalid_path); return;
            }
            pendingDownloadPath = path;
            setBusy(true);
            downloadLauncher.launch(FilePickerHelper.createDownloadIntent(path.substring(path.lastIndexOf('/') + 1)));
        });
        findViewById(R.id.transfer_setup).setOnClickListener(v ->
                startActivity(new Intent(this, ProjectDropboxActivity.class)));
        findViewById(R.id.transfer_back).setOnClickListener(v -> finish());
    }

    private void showResult(int message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) resultText.setText(message);
        });
    }
    private void setBusy(boolean value) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            busy = value;
            findViewById(R.id.transfer_upload).setEnabled(!value);
            findViewById(R.id.transfer_download).setEnabled(!value);
            findViewById(R.id.transfer_setup).setEnabled(!value);
            dropboxPathInput.setEnabled(!value);
        });
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("pending_download_path", pendingDownloadPath);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
