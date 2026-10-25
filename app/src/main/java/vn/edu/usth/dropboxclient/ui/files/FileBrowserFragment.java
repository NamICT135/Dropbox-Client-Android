package vn.edu.usth.dropboxclient.ui.files;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.BuildConfig;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;

public class FileBrowserFragment extends Fragment {
    private static final String KEY_PATH = "folder_path";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Metadata> entries = new ArrayList<>();
    private ExecutorService io;
    private FileAdapter adapter;
    private String path = "";
    private String cursor;
    private int generation;
    private boolean busy;
    private View content;

    public FileBrowserFragment() { super(R.layout.fragment_file_browser); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        content = view;
        io = Executors.newSingleThreadExecutor();
        if (state != null) path = state.getString(KEY_PATH, "");
        adapter = new FileAdapter();
        RecyclerView list = view.findViewById(R.id.recyclerViewFiles);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> refresh());
        view.findViewById(R.id.files_refresh).setOnClickListener(v -> refresh());
        view.findViewById(R.id.files_up).setOnClickListener(v -> {
            if (path.isEmpty()) return;
            int slash = path.lastIndexOf('/');
            openFolder(slash <= 0 ? "" : path.substring(0, slash));
        });
        view.findViewById(R.id.files_more).setOnClickListener(v -> load(true));
        updateHeader();
        refresh();
    }

    private void openFolder(String folderPath) {
        path = folderPath;
        refresh();
    }

    private void refresh() {
        if (content == null) return;
        cursor = null;
        int previousCount = entries.size();
        entries.clear();
        adapter.notifyItemRangeRemoved(0, previousCount);
        updateHeader();
        load(false);
    }

    private void load(boolean nextPage) {
        if (content == null || io == null || (nextPage && (busy || cursor == null))) return;
        final int request = ++generation;
        final View view = content;
        final String requestedPath = path;
        final String requestedCursor = nextPage ? cursor : null;
        busy = true;
        view.findViewById(R.id.files_refresh).setEnabled(false);
        view.findViewById(R.id.files_more).setVisibility(View.GONE);
        view.findViewById(R.id.files_page_error).setVisibility(View.GONE);
        if (!nextPage) showState(State.LOADING);
        io.execute(() -> {
            ListFolderResult result = null;
            int error = R.string.files_load_error;
            try {
                SessionManager session = new SessionManager(view.getContext());
                DbxCredential credential = session.load();
                if (credential == null || !BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) {
                    error = R.string.files_connect_required;
                } else {
                    DropboxRepository repository = new DropboxRepository(DropboxClientFactory.create(credential));
                    result = requestedCursor == null
                            ? repository.listFolder(requestedPath)
                            : repository.continueListFolder(requestedCursor);
                }
            } catch (InvalidAccessTokenException e) {
                new SessionManager(view.getContext()).clear();
                error = R.string.project_expired;
            } catch (GeneralSecurityException | IOException e) {
                error = R.string.session_error;
            } catch (DbxException e) {
                error = R.string.files_load_error;
            }
            ListFolderResult page = result;
            int message = error;
            main.post(() -> {
                if (content != view || request != generation) return;
                busy = false;
                view.findViewById(R.id.files_refresh).setEnabled(true);
                if (page == null) {
                    if (nextPage) {
                        view.findViewById(R.id.files_page_error).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.files_more).setVisibility(View.VISIBLE);
                    } else {
                        ((TextView) view.findViewById(R.id.tvErrorMessage)).setText(message);
                        showState(State.ERROR);
                    }
                    return;
                }
                int start = entries.size();
                entries.addAll(page.getEntries());
                adapter.notifyItemRangeInserted(start, page.getEntries().size());
                cursor = page.getHasMore() ? page.getCursor() : null;
                showState(entries.isEmpty() ? State.EMPTY : State.SUCCESS);
                view.findViewById(R.id.files_more).setVisibility(cursor == null ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void updateHeader() {
        if (content == null) return;
        ((TextView) content.findViewById(R.id.files_path)).setText(
                getString(R.string.files_path, path.isEmpty() ? "/" : path));
        content.findViewById(R.id.files_up).setEnabled(!path.isEmpty());
    }

    private void showState(State state) {
        content.findViewById(R.id.recyclerViewFiles).setVisibility(state == State.SUCCESS ? View.VISIBLE : View.GONE);
        content.findViewById(R.id.progressBarLoading).setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        content.findViewById(R.id.layoutEmpty).setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        content.findViewById(R.id.layoutError).setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        state.putString(KEY_PATH, path);
        super.onSaveInstanceState(state);
    }

    @Override public void onDestroyView() {
        generation++;
        io.shutdownNow();
        io = null;
        content = null;
        adapter = null;
        super.onDestroyView();
    }

    private enum State { LOADING, SUCCESS, EMPTY, ERROR }

    private final class FileAdapter extends RecyclerView.Adapter<FileAdapter.Holder> {
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            TextView label = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dropbox_entry, parent, false);
            return new Holder(label);
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Metadata item = entries.get(position);
            boolean folder = item instanceof FolderMetadata;
            holder.label.setText(getString(folder ? R.string.files_folder_item : R.string.files_file_item,
                    item.getName()));
            holder.label.setOnClickListener(folder ? v -> {
                String folderPath = item.getPathDisplay();
                if (folderPath != null) openFolder(folderPath);
            } : null);
        }

        @Override public int getItemCount() { return entries.size(); }

        private final class Holder extends RecyclerView.ViewHolder {
            final TextView label;
            Holder(TextView label) { super(label); this.label = label; }
        }
    }
}
