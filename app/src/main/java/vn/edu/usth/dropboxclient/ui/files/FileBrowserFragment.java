package vn.edu.usth.dropboxclient.ui.files;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationErrorException;
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
import vn.edu.usth.dropboxclient.ui.folder.CreateFolderDialogFragment;
import vn.edu.usth.dropboxclient.ui.folder.DeleteFolderDialogFragment;
import vn.edu.usth.dropboxclient.ui.folder.DropboxFolderFactory;
import vn.edu.usth.dropboxclient.ui.folder.DropboxFolderService;
import vn.edu.usth.dropboxclient.ui.folder.FolderValidationHelper;
import vn.edu.usth.dropboxclient.ui.folder.RenameFolderDialogFragment;
import vn.edu.usth.dropboxclient.ui.transfer.TransferActivity;
import vn.edu.usth.dropboxclient.ui.transfer.TransferViewModel;

public class FileBrowserFragment extends Fragment implements
        CreateFolderDialogFragment.OnFolderCreateListener,
        RenameFolderDialogFragment.OnFolderRenameListener,
        DeleteFolderDialogFragment.OnFolderDeleteListener {
    private static final String KEY_PATH = "folder_path";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Metadata> entries = new ArrayList<>();
    private ExecutorService io;
    private FileAdapter adapter;
    private String path = "";
    private String cursor;
    private int generation;
    private boolean busy;
    private boolean actionBusy;
    private boolean actionNeedsRefresh;
    private boolean firstResume = true;
    private View content;

    public FileBrowserFragment() { super(R.layout.fragment_file_browser); }

    public static FileBrowserFragment newInstance(String startPath) {
        FileBrowserFragment fragment = new FileBrowserFragment();
        Bundle arguments = new Bundle();
        arguments.putString(KEY_PATH, startPath);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        content = view;
        firstResume = true;
        io = Executors.newSingleThreadExecutor();
        if (state != null) path = state.getString(KEY_PATH, "");
        else if (getArguments() != null) path = getArguments().getString(KEY_PATH, "");
        adapter = new FileAdapter();
        RecyclerView list = view.findViewById(R.id.recyclerViewFiles);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> refresh());
        view.findViewById(R.id.files_refresh).setOnClickListener(v -> {
            clearActionStatus();
            refresh();
        });
        view.findViewById(R.id.files_up).setOnClickListener(v -> {
            if (path.isEmpty()) return;
            int slash = path.lastIndexOf('/');
            openFolder(slash <= 0 ? "" : path.substring(0, slash));
        });
        view.findViewById(R.id.files_more).setOnClickListener(v -> load(true));
        view.findViewById(R.id.files_create).setOnClickListener(v -> {
            if (busy || actionBusy || actionNeedsRefresh) return;
            CreateFolderDialogFragment.newInstance(path)
                    .show(getChildFragmentManager(), "createFolder");
        });
        view.findViewById(R.id.files_upload).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TransferActivity.class)
                        .putExtra(TransferActivity.EXTRA_FOLDER_PATH, path)));
        updateHeader();
        refresh();
    }

    @Override public void onResume() {
        super.onResume();
        if (content != null && !firstResume) refresh();
        firstResume = false;
    }

    private void openFolder(String folderPath) {
        if (actionBusy) return;
        clearActionStatus();
        path = folderPath;
        refresh();
    }

    private void clearActionStatus() {
        if (content != null) content.findViewById(R.id.files_action_status).setVisibility(View.GONE);
    }

    private void refresh() {
        if (content == null || actionBusy) return;
        cursor = null;
        int previousCount = entries.size();
        entries.clear();
        adapter.notifyItemRangeRemoved(0, previousCount);
        updateHeader();
        load(false);
    }

    private void load(boolean nextPage) {
        if (content == null || io == null || actionBusy
                || (nextPage && (busy || cursor == null))) return;
        final int request = ++generation;
        final View view = content;
        final String requestedPath = path;
        final String requestedCursor = nextPage ? cursor : null;
        busy = true;
        view.findViewById(R.id.files_refresh).setEnabled(false);
        view.findViewById(R.id.files_create).setEnabled(false);
        view.findViewById(R.id.files_upload).setEnabled(false);
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
                    view.findViewById(R.id.files_create).setEnabled(false);
                    view.findViewById(R.id.files_upload).setEnabled(false);
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
                actionNeedsRefresh = false;
                view.findViewById(R.id.files_create).setEnabled(true);
                view.findViewById(R.id.files_upload).setEnabled(true);
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

    @Override public void onFolderCreateConfirmed(String parentPath, String folderName) {
        runFolderAction(R.string.folder_action_creating, R.string.folder_create_success,
                folderName, service -> service.createFolder(parentPath, folderName));
    }

    @Override public void onFolderRenameConfirmed(String currentPath, String newName) {
        runFolderAction(R.string.folder_action_renaming, R.string.folder_rename_success,
                newName, service -> service.renameFolder(currentPath, newName));
    }

    @Override public void onFolderDeleteConfirmed(String folderPath) {
        runFolderAction(R.string.folder_action_deleting, R.string.folder_delete_success,
                FolderValidationHelper.getNameFromPath(folderPath),
                service -> service.deleteFolder(folderPath));
    }

    private interface FolderAction {
        Metadata run(DropboxFolderService service) throws DbxException;
    }

    private void runFolderAction(int progressText, int successText, String name, FolderAction action) {
        if (content == null || io == null || busy || actionBusy || actionNeedsRefresh) return;
        actionBusy = true;
        View view = content;
        int request = ++generation;
        view.findViewById(R.id.files_create).setEnabled(false);
        view.findViewById(R.id.files_upload).setEnabled(false);
        view.findViewById(R.id.files_refresh).setEnabled(false);
        view.findViewById(R.id.files_up).setEnabled(false);
        showActionStatus(view, getString(progressText));
        io.execute(() -> {
            Exception failure = null;
            DropboxFolderService service = null;
            try {
                service = DropboxFolderFactory.create(view.getContext());
                action.run(service);
            } catch (InvalidAccessTokenException e) {
                new SessionManager(view.getContext()).clear();
                failure = e;
            } catch (DbxException | GeneralSecurityException | IOException | IllegalArgumentException e) {
                failure = e;
            } finally {
                if (service != null) service.shutdown();
            }
            Exception error = failure;
            main.post(() -> {
                if (content != view || request != generation) return;
                actionBusy = false;
                view.findViewById(R.id.files_refresh).setEnabled(true);
                updateHeader();
                if (error == null) {
                    showActionStatus(view, getString(successText, name));
                    refresh();
                } else {
                    actionNeedsRefresh = true;
                    view.findViewById(R.id.files_create).setEnabled(false);
                    view.findViewById(R.id.files_upload).setEnabled(false);
                    showActionStatus(view, getString(folderErrorMessage(error)));
                }
            });
        });
    }

    private void showActionStatus(View view, String message) {
        TextView status = view.findViewById(R.id.files_action_status);
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void showFileDetails(FileMetadata file) {
        String filePath = file.getPathDisplay() != null
                ? file.getPathDisplay() : file.getPathLower();
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(file.getName())
                .setMessage(getString(R.string.files_file_details,
                        filePath == null ? file.getName() : filePath, file.getSize()))
                .setNegativeButton(android.R.string.cancel, null);
        if (filePath != null) dialog.setPositiveButton(R.string.transfer_download,
                (ignored, which) -> startActivity(new Intent(requireContext(), TransferActivity.class)
                        .putExtra(TransferActivity.EXTRA_FOLDER_PATH, path)
                        .putExtra(TransferActivity.EXTRA_DOWNLOAD_PATH, filePath)));
        if (TransferViewModel.previewMimeType(filePath) != null)
            dialog.setNeutralButton(R.string.preview_action,
                    (ignored, which) -> startActivity(new Intent(requireContext(), TransferActivity.class)
                            .putExtra(TransferActivity.EXTRA_FOLDER_PATH, path)
                            .putExtra(TransferActivity.EXTRA_DOWNLOAD_PATH, filePath)
                            .putExtra(TransferActivity.EXTRA_OPEN_PREVIEW, true)));
        dialog.show();
    }

    private void showFolderMenu(Metadata item) {
        if (busy || actionBusy || actionNeedsRefresh) return;
        String folderPath = item.getPathDisplay();
        if (folderPath == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(new String[]{getString(R.string.folder_rename),
                        getString(R.string.folder_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        RenameFolderDialogFragment.newInstance(folderPath, item.getName())
                                .show(getChildFragmentManager(), "renameFolder");
                    } else {
                        DeleteFolderDialogFragment.newInstance(folderPath, item.getName())
                                .show(getChildFragmentManager(), "deleteFolder");
                    }
                }).show();
    }

    private int folderErrorMessage(Exception error) {
        if (error instanceof InvalidAccessTokenException) return R.string.project_expired;
        if (error instanceof CreateFolderErrorException) {
            CreateFolderErrorException value = (CreateFolderErrorException) error;
            if (value.errorValue.isPath() && value.errorValue.getPathValue().isConflict())
                return R.string.folder_conflict_error;
        }
        if (error instanceof RelocationErrorException) {
            RelocationErrorException value = (RelocationErrorException) error;
            if (value.errorValue.isTo() && value.errorValue.getToValue().isConflict())
                return R.string.folder_conflict_error;
            if (value.errorValue.isFromLookup()
                    && value.errorValue.getFromLookupValue().isNotFound())
                return R.string.folder_not_found_error;
        }
        if (error instanceof DeleteErrorException) {
            DeleteErrorException value = (DeleteErrorException) error;
            if (value.errorValue.isPathLookup()
                    && value.errorValue.getPathLookupValue().isNotFound())
                return R.string.folder_not_found_error;
        }
        if (error instanceof GeneralSecurityException || error instanceof IOException)
            return R.string.transfer_session_error;
        if (error instanceof IllegalArgumentException) return R.string.folder_invalid_path;
        return R.string.folder_api_error;
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
        actionBusy = false;
        io.shutdownNow();
        io = null;
        content = null;
        adapter = null;
        super.onDestroyView();
    }

    private enum State { LOADING, SUCCESS, EMPTY, ERROR }

    private final class FileAdapter extends RecyclerView.Adapter<FileAdapter.Holder> {
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dropbox_entry, parent, false);
            return new Holder(row);
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Metadata item = entries.get(position);
            boolean folder = item instanceof FolderMetadata;
            holder.label.setText(item.getName());
            holder.itemView.setContentDescription(getString(
                    folder ? R.string.files_folder_item : R.string.files_file_item, item.getName()));
            holder.icon.setImageResource(folder ? R.drawable.ic_folder : R.drawable.ic_file);
            holder.menu.setVisibility(folder ? View.VISIBLE : View.GONE);
            holder.menu.setOnClickListener(folder ? v -> showFolderMenu(item) : null);
            holder.itemView.setOnClickListener(v -> {
                if (actionBusy) return;
                if (folder) {
                    String folderPath = item.getPathDisplay();
                    if (folderPath != null) openFolder(folderPath);
                } else if (item instanceof FileMetadata) {
                    showFileDetails((FileMetadata) item);
                }
            });
            holder.itemView.setOnLongClickListener(folder ? v -> {
                showFolderMenu(item);
                return true;
            } : null);
        }

        @Override public int getItemCount() { return entries.size(); }

        private final class Holder extends RecyclerView.ViewHolder {
            final TextView label;
            final ImageView icon;
            final View menu;
            Holder(View row) {
                super(row);
                label = row.findViewById(R.id.file_name);
                icon = row.findViewById(R.id.file_icon);
                menu = row.findViewById(R.id.file_menu);
            }
        }
    }
}
