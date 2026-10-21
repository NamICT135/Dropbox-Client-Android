
package vn.edu.usth.dropboxclient.ui.transfer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferTestActivity extends AppCompatActivity {

    private TextView resultText;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // File Picker for Upload
    private final ActivityResultLauncher<Intent> uploadLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK
                                || result.getData() == null) {
                            resultText.setText("File selection cancelled");
                            return;
                        }

                        Uri fileUri = result.getData().getData();

                        if (fileUri == null) {
                            resultText.setText("Invalid file URI");
                            return;
                        }

                        resultText.setText("Reading selected file...");
                        testReadFile(fileUri);
                    }
            );

    // File Picker for Download
    private final ActivityResultLauncher<Intent> downloadLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK
                                || result.getData() == null) {
                            resultText.setText("Download cancelled");
                            return;
                        }

                        Uri destinationUri = result.getData().getData();

                        if (destinationUri == null) {
                            resultText.setText("Invalid destination URI");
                            return;
                        }

                        resultText.setText("Saving test file...");
                        testDownload(destinationUri);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        Button uploadButton = new Button(this);
        uploadButton.setText("SELECT FILE FOR UPLOAD");

        Button downloadButton = new Button(this);
        downloadButton.setText("TEST DOWNLOAD");

        resultText = new TextView(this);
        resultText.setText("Choose Upload or Download");

        layout.addView(uploadButton);
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

        downloadButton.setOnClickListener(v ->
                downloadLauncher.launch(
                        FilePickerHelper.createDownloadIntent(
                                "download_test.txt"
                        )
                )
        );
    }

    // Test reading a small UTF-8 text file
    private void testReadFile(Uri fileUri) {

        executor.execute(() -> {
            try (InputStream input =
                         FileReadHelper.openFile(this, fileUri);
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalBytes = 0;

                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;

                    if (totalBytes > 4096) {
                        throw new IOException(
                                "Test file exceeds 4 KB"
                        );
                    }

                    output.write(buffer, 0, bytesRead);
                }

                String content = new String(
                        output.toByteArray(),
                        StandardCharsets.UTF_8
                );

                String name = FileInfoHelper.getFileName(
                        this, fileUri
                );

                long size = FileInfoHelper.getFileSize(
                        this, fileUri
                );

                showResult(
                        "UPLOAD FILE TEST\n\n"
                                + "Name: " + name
                                + "\nSize: " + size + " bytes"
                                + "\nContent:\n" + content
                );

            } catch (Exception e) {
                showResult("Read failed: " + e.getMessage());
            }
        });
    }

    // Test saving sample data to Android storage
    private void testDownload(Uri destinationUri) {

        executor.execute(() -> {
            String sampleContent =
                    "Hello from Dropbox Client!\n"
                            + "This is a test download file.\n";

            byte[] data = sampleContent.getBytes(
                    StandardCharsets.UTF_8
            );

            try (InputStream input =
                         new ByteArrayInputStream(data)) {

                FileWriteHelper.writeFile(
                        this, destinationUri, input
                );

                showResult(
                        "DOWNLOAD TEST SUCCESS!\n\n"
                                + "Saved file: download_test.txt"
                                + "\nSize: " + data.length + " bytes"
                                + "\nURI: " + destinationUri
                );

            } catch (Exception e) {
                showResult("Download failed: " + e.getMessage());
            }
        });
    }

    private void showResult(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                resultText.setText(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
