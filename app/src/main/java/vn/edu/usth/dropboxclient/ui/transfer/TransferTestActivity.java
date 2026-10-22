
package vn.edu.usth.dropboxclient.ui.transfer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.v2.files.FileMetadata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferTestActivity extends AppCompatActivity {

    private TextView resultText;
    private EditText dropboxPathInput;
    private Button uploadButton;
    private Button downloadButton;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> uploadLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK
                                || result.getData() == null
                                || result.getData().getData() == null) {
                            showResult("Upload selection cancelled");
                            return;
                        }

                        Uri uri = result.getData().getData();
                        String fileName = FileInfoHelper.getFileName(this, uri);

                        // Use a dedicated test folder on Dropbox
                        String dropboxPath = "/test_upload_" + fileName;

                        setBusy(true);
                        showResult("Uploading to Dropbox...");

                        executor.execute(() -> {
                            try {
                                DropboxTransferService service =
                                        DropboxTransferFactory.create(this);

                                FileMetadata metadata = service.uploadFile(
                                        uri, dropboxPath
                                );

                                showResult(
                                        "UPLOAD SUCCESS!\n"
                                                + "Name: " + metadata.getName()
                                                + "\nPath: " + metadata.getPathDisplay()
                                                + "\nSize: " + metadata.getSize()
                                                + " bytes"
                                );

                            } catch (Exception e) {
                                showResult(
                                        "Upload failed: "
                                                + e.getMessage()
                                );
                            } finally {
                                setBusy(false);
                            }
                        });
                    }
            );

    private final ActivityResultLauncher<Intent> downloadLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK
                                || result.getData() == null
                                || result.getData().getData() == null) {
                            showResult("Download cancelled");
                            return;
                        }

                        Uri destinationUri = result.getData().getData();
                        String dropboxPath =
                                dropboxPathInput.getText().toString().trim();

                        setBusy(true);
                        showResult("Downloading from Dropbox...");

                        executor.execute(() -> {
                            try {
                                DropboxTransferService service =
                                        DropboxTransferFactory.create(this);

                                FileMetadata metadata =
                                        service.downloadFile(
                                                dropboxPath,
                                                destinationUri
                                        );

                                showResult(
                                        "DOWNLOAD SUCCESS!\n"
                                                + "Name: " + metadata.getName()
                                                + "\nSize: " + metadata.getSize()
                                                + " bytes"
                                );

                            } catch (Exception e) {
                                showResult(
                                        "Download failed: "
                                                + e.getMessage()
                                );
                            } finally {
                                setBusy(false);
                            }
                        });
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int padding = (int) (20 *
                getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding, padding, padding);

        uploadButton = new Button(this);
        uploadButton.setText("UPLOAD TO DROPBOX");

        dropboxPathInput = new EditText(this);
        dropboxPathInput.setSingleLine(true);
        dropboxPathInput.setHint("Dropbox file path");
        dropboxPathInput.setText("/test_upload_test_upload.txt");

        downloadButton = new Button(this);
        downloadButton.setText("DOWNLOAD FROM DROPBOX");

        resultText = new TextView(this);
        resultText.setText("Dropbox Transfer Test");

        layout.addView(uploadButton);
        layout.addView(dropboxPathInput);
        layout.addView(downloadButton);
        layout.addView(resultText);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        setContentView(scrollView);

        uploadButton.setOnClickListener(v ->
                uploadLauncher.launch(
                        FilePickerHelper.createUploadIntent()
                )
        );

        downloadButton.setOnClickListener(v -> {
            String path = dropboxPathInput.getText()
                    .toString().trim();

            if (path.isEmpty() || !path.startsWith("/")
                    || path.length() <= 1) {
                showResult("Enter a valid Dropbox path");
                return;
            }

            String fileName = path.substring(path.lastIndexOf('/') + 1);

            if (fileName.isEmpty()) {
                showResult("Enter a file path, not a folder");
                return;
            }

            downloadLauncher.launch(
                    FilePickerHelper.createDownloadIntent(fileName)
            );
        });
    }

    private void showResult(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                resultText.setText(message);
            }
        });
    }

    private void setBusy(boolean busy) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                uploadButton.setEnabled(!busy);
                downloadButton.setEnabled(!busy);
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
