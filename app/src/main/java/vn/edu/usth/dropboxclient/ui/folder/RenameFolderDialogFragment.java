package vn.edu.usth.dropboxclient.ui.folder;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import vn.edu.usth.dropboxclient.R;

public class RenameFolderDialogFragment extends DialogFragment {

    private static final String ARG_CURRENT_PATH = "current_path";
    private static final String ARG_CURRENT_NAME = "current_name";

    public interface OnFolderRenameListener {
        void onFolderRenameConfirmed(String currentPath, String newName);
    }

    private OnFolderRenameListener listener;

    public static RenameFolderDialogFragment newInstance(String currentPath, String currentName) {
        RenameFolderDialogFragment fragment = new RenameFolderDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_PATH, currentPath);
        args.putString(ARG_CURRENT_NAME, currentName);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFolderRenameListener(OnFolderRenameListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (listener == null && context instanceof OnFolderRenameListener) {
            listener = (OnFolderRenameListener) context;
        } else if (listener == null && getParentFragment() instanceof OnFolderRenameListener) {
            listener = (OnFolderRenameListener) getParentFragment();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String currentPath = getArguments() != null ? getArguments().getString(ARG_CURRENT_PATH, "") : "";
        String currentName = getArguments() != null ? getArguments().getString(ARG_CURRENT_NAME, "") : "";

        Context context = requireContext();
        final EditText input = new EditText(context);
        input.setHint(R.string.folder_rename_hint);
        input.setText(currentName);
        if (currentName != null && !currentName.isEmpty()) {
            input.setSelection(0, currentName.length());
        }
        input.setSingleLine(true);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.folder_rename_title)
                .setView(input)
                .setPositiveButton(R.string.folder_rename, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String newName = input.getText() != null ? input.getText().toString() : "";
                FolderValidationHelper.ValidationError error =
                        FolderValidationHelper.validateRename(currentName, newName);
                if (error != FolderValidationHelper.ValidationError.NONE) {
                    input.setError(getErrorMessage(error));
                    return;
                }

                if (listener != null) {
                    listener.onFolderRenameConfirmed(currentPath, newName);
                }
                dismiss();
            });

            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    input.setError(null);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        });

        return dialog;
    }

    private String getErrorMessage(FolderValidationHelper.ValidationError error) {
        switch (error) {
            case EMPTY:
                return getString(R.string.folder_name_empty);
            case SAME_NAME:
                return getString(R.string.folder_name_same);
            case INVALID_CHARACTERS:
                return getString(R.string.folder_name_invalid_chars);
            case ENDS_WITH_PERIOD_OR_SPACE:
                return getString(R.string.folder_name_edge);
            case TOO_LONG:
                return getString(R.string.folder_name_too_long);
            default:
                return getString(R.string.folder_name_empty);
        }
    }
}
