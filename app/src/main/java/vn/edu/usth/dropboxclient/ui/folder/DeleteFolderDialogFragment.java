package vn.edu.usth.dropboxclient.ui.folder;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import vn.edu.usth.dropboxclient.R;

public class DeleteFolderDialogFragment extends DialogFragment {

    private static final String ARG_PATH = "path";
    private static final String ARG_FOLDER_NAME = "folder_name";

    public interface OnFolderDeleteListener {
        void onFolderDeleteConfirmed(String path);
    }

    private OnFolderDeleteListener listener;

    public static DeleteFolderDialogFragment newInstance(String path, String folderName) {
        DeleteFolderDialogFragment fragment = new DeleteFolderDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);
        args.putString(ARG_FOLDER_NAME, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFolderDeleteListener(OnFolderDeleteListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (listener == null && context instanceof OnFolderDeleteListener) {
            listener = (OnFolderDeleteListener) context;
        } else if (listener == null && getParentFragment() instanceof OnFolderDeleteListener) {
            listener = (OnFolderDeleteListener) getParentFragment();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String path = getArguments() != null ? getArguments().getString(ARG_PATH, "") : "";
        String folderName = getArguments() != null ? getArguments().getString(ARG_FOLDER_NAME, "") : "";

        String message = getString(R.string.folder_delete_confirm, folderName);

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.folder_delete_title)
                .setMessage(message)
                .setPositiveButton(R.string.folder_delete, (dialog, which) -> {
                    if (listener != null) {
                        listener.onFolderDeleteConfirmed(path);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dismiss())
                .create();
    }
}
