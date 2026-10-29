package vn.edu.usth.dropboxclient.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SearchMatchV2;
import com.dropbox.core.v2.files.SearchV2Result;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.dropboxclient.BuildConfig;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.auth.DropboxClientFactory;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import vn.edu.usth.dropboxclient.repository.DropboxErrors;
import vn.edu.usth.dropboxclient.ui.main.MainActivity;
import vn.edu.usth.dropboxclient.ui.transfer.TransferActivity;

/** Searches the authorized App Folder through Dropbox, without loading files on the UI thread. */
public class SearchFragment extends Fragment {
    private static final String KEY_QUERY = "query";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Metadata> entries = new ArrayList<>();
    private final Set<String> seenPaths = new HashSet<>();
    private ExecutorService io;
    private RecyclerView.Adapter<ResultHolder> adapter;
    private View content;
    private String query = "";
    private String cursor;
    private int generation;
    private boolean busy;

    public SearchFragment() { super(R.layout.fragment_search); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        content = view;
        io = Executors.newSingleThreadExecutor();
        RecyclerView list = view.findViewById(R.id.search_results);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ResultAdapter();
        list.setAdapter(adapter);
        EditText input = view.findViewById(R.id.search_query);
        view.findViewById(R.id.search_button).setOnClickListener(v -> search(input.getText().toString()));
        input.setOnEditorActionListener((field, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            search(field.getText().toString());
            return true;
        });
        view.findViewById(R.id.search_more).setOnClickListener(v -> load(true));
        if (state != null) {
            query = state.getString(KEY_QUERY, "");
            input.setText(query);
            search(query);
        } else showState(R.string.search_ready, false, false);
    }

    private void search(String text) {
        query = text.trim();
        cursor = null;
        int count = entries.size();
        entries.clear();
        seenPaths.clear();
        adapter.notifyItemRangeRemoved(0, count);
        if (query.isEmpty()) {
            generation++;
            busy = false;
            content.findViewById(R.id.search_progress).setVisibility(View.GONE);
            content.findViewById(R.id.search_more).setVisibility(View.GONE);
            showState(R.string.search_enter_query, false, false);
        } else load(false);
    }

    private void load(boolean nextPage) {
        if (content == null || io == null || query.isEmpty()
                || (nextPage && (busy || cursor == null))) return;
        int request = ++generation;
        View view = content;
        String requestedQuery = query;
        long localRevision = new vn.edu.usth.dropboxclient.auth.DemoAuthManager(view.getContext()).sessionRevision();
        long sessionRevision = new SessionManager(view.getContext()).revision();
        String requestedCursor = nextPage ? cursor : null;
        busy = true;
        view.findViewById(R.id.search_more).setVisibility(View.GONE);
        view.findViewById(R.id.search_progress).setVisibility(View.VISIBLE);
        if (!nextPage) showState(R.string.search_loading, false, false);
        io.execute(() -> {
            SearchV2Result page = null;
            int error = R.string.search_error;
            try {
                SessionManager session = new SessionManager(view.getContext());
                DbxCredential credential = session.load();
                if (credential == null || !BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) {
                    error = R.string.files_connect_required;
                } else {
                    DropboxRepository repository = new DropboxRepository(DropboxClientFactory.create(credential));
                    page = requestedCursor == null
                            ? repository.search(requestedQuery)
                            : repository.continueSearch(requestedCursor);
                }
            } catch (InvalidAccessTokenException e) {
                new SessionManager(view.getContext()).clearIfCurrent(sessionRevision);
                error = R.string.project_expired;
            } catch (GeneralSecurityException | IOException e) {
                error = R.string.session_error;
            } catch (DbxException e) {
                error = DropboxErrors.message(e, R.string.search_error);
            }
            SearchV2Result result = page;
            int message = error;
            main.post(() -> {
                if (content != view || request != generation
                        || new vn.edu.usth.dropboxclient.auth.DemoAuthManager(view.getContext()).sessionRevision() != localRevision) return;
                if (new SessionManager(view.getContext()).revision()
                        != sessionRevision + (message == R.string.project_expired ? 1 : 0)) {
                    busy = false;
                    search(query);
                    return;
                }
                busy = false;
                view.findViewById(R.id.search_progress).setVisibility(View.GONE);
                if (result == null) {
                    if (nextPage) {
                        showState(R.string.search_more_error, true, !entries.isEmpty());
                        view.findViewById(R.id.search_more).setVisibility(View.VISIBLE);
                    } else showState(message, true, false);
                    return;
                }
                int start = entries.size();
                for (SearchMatchV2 match : result.getMatches()) {
                    if (match.getMetadata().isMetadata()) {
                        Metadata metadata = match.getMetadata().getMetadataValue();
                        String path = metadata.getPathLower() != null
                                ? metadata.getPathLower() : metadata.getPathDisplay();
                        if ((metadata instanceof FileMetadata || metadata instanceof FolderMetadata)
                                && path != null && seenPaths.add(path)) entries.add(metadata);
                    }
                }
                adapter.notifyItemRangeInserted(start, entries.size() - start);
                cursor = result.getHasMore() ? result.getCursor() : null;
                showState(entries.isEmpty() ? R.string.search_empty : R.string.search_count,
                        false, !entries.isEmpty());
                view.findViewById(R.id.search_more).setVisibility(cursor == null ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void showState(int message, boolean error, boolean hasResults) {
        if (content == null) return;
        TextView status = content.findViewById(R.id.search_status);
        status.setText(message == R.string.search_count
                ? getResources().getQuantityString(R.plurals.search_results_count,
                        entries.size(), entries.size()) : getString(message));
        status.setTextColor(requireContext().getColor(error ? R.color.status_error : R.color.text_muted));
        content.findViewById(R.id.search_results).setVisibility(hasResults ? View.VISIBLE : View.GONE);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        View view = getView();
        state.putString(KEY_QUERY, view == null ? query
                : ((EditText) view.findViewById(R.id.search_query)).getText().toString().trim());
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

    private final class ResultAdapter extends RecyclerView.Adapter<ResultHolder> {
        @NonNull @Override public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new ResultHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dropbox_entry, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ResultHolder holder, int position) {
            Metadata item = entries.get(position);
            boolean folder = item instanceof FolderMetadata;
            String path = item.getPathDisplay() != null ? item.getPathDisplay() : item.getPathLower();
            holder.name.setText(item.getName());
            holder.path.setText(path);
            holder.path.setVisibility(View.VISIBLE);
            holder.icon.setImageResource(folder ? R.drawable.ic_folder : R.drawable.ic_file);
            holder.itemView.setContentDescription(getString(
                    folder ? R.string.files_folder_item : R.string.files_file_item, item.getName()));
            holder.itemView.setOnClickListener(v -> {
                if (path == null) return;
                if (folder) ((MainActivity) requireActivity()).openFiles(path);
                else startActivity(new Intent(requireContext(), TransferActivity.class)
                        .putExtra(TransferActivity.EXTRA_DOWNLOAD_PATH, path));
            });
        }

        @Override public int getItemCount() { return entries.size(); }
    }

    private static final class ResultHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView path;
        final ImageView icon;
        ResultHolder(View row) {
            super(row);
            name = row.findViewById(R.id.file_name);
            path = row.findViewById(R.id.file_path);
            icon = row.findViewById(R.id.file_icon);
            row.findViewById(R.id.file_menu).setVisibility(View.GONE);
        }
    }
}
