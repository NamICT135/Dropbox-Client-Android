
package vn.edu.usth.dropboxclient.ui.files;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;

public class FileBrowserFragment extends Fragment {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private String currentFolder = "";

    private TextView txtPath;
    private TextView txtStatus;
    private LinearLayout fileList;
    private ProgressBar progressBar;
    private Button btnParent;
    private DropboxRepository repository;

    public FileBrowserFragment() {
        super(R.layout.fragment_file_browser);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtPath = view.findViewById(R.id.txt_current_path);
        txtStatus = view.findViewById(R.id.txt_file_status);
        fileList = view.findViewById(R.id.layout_file_list);
        progressBar = view.findViewById(R.id.progress_files);
        btnParent = view.findViewById(R.id.btn_parent);

        Button btnRefresh = view.findViewById(R.id.btn_refresh);

        btnParent.setOnClickListener(v -> goToParent());
        btnRefresh.setOnClickListener(v -> loadFiles());

        loadFiles();
    }

    private void loadFiles() {
        if (getContext() == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        txtStatus.setText("Loading files...");
        txtPath.setText(
                currentFolder.isEmpty() ? "/" : currentFolder
        );
        fileList.removeAllViews();

        executor.execute(() -> {
            try {
                SessionManager session =
                        new SessionManager(requireContext());

                DbxCredential credential = session.load();

                if (credential == null) {
                    showStatus(
                            "Please connect your Dropbox account first."
                    );
                    return;
                }

                DropboxRepository repo = new DropboxRepository(
                        DropboxClientFactory.create(credential)
                );

                List<Metadata> files =
                        repo.listFolder(currentFolder);

                handler.post(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }

                    repository = repo;
                    progressBar.setVisibility(View.GONE);

                    txtStatus.setText(
                            files.isEmpty()
                                    ? "This folder is empty."
                                    : files.size() + " item(s)"
                    );

                    for (Metadata metadata : files) {
                        addFileItem(metadata);
                    }
                });

            } catch (Exception e) {
                String message = e.getMessage();

                if (message == null || message.isEmpty()) {
                    message = e.getClass().getSimpleName();
                }

                showStatus("Unable to load files: " + message);
            }
        });
    }

    private void addFileItem(Metadata metadata) {
        TextView item = new TextView(requireContext());

        boolean isFolder = metadata instanceof FolderMetadata;
        String icon = isFolder ? "📁 " : "📄 ";

        item.setText(icon + metadata.getName());
        item.setTextSize(16);
        item.setPadding(12, 16, 12, 16);

        fileList.addView(item);

        if (isFolder) {
            item.setOnClickListener(v -> {
                currentFolder = metadata.getPathLower();
                loadFiles();
            });
        }
    }

    private void goToParent() {
        if (currentFolder.isEmpty()) {
            return;
        }

        int index = currentFolder.lastIndexOf('/');

        if (index <= 0) {
            currentFolder = "";
        } else {
            currentFolder = currentFolder.substring(0, index);
        }

        loadFiles();
    }

    private void showStatus(String message) {
        handler.post(() -> {
            if (!isAdded() || getView() == null) {
                return;
            }

            progressBar.setVisibility(View.GONE);
            txtStatus.setText(message);
        });
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
}
