package vn.edu.usth.dropboxclient.ui.folder;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import vn.edu.usth.dropboxclient.R;

public class CreateFolderDialogFragment extends DialogFragment {

    private static final String ARG_PARENT_PATH = "parent_path";

    public interface OnFolderCreateListener {
        void onFolderCreateConfirmed(String parentPath, String folderName);
    }

    private OnFolderCreateListener listener;

    public static CreateFolderDialogFragment newInstance(String parentPath) {
        CreateFolderDialogFragment fragment = new CreateFolderDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARENT_PATH, parentPath != null ? parentPath : "/");
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFolderCreateListener(OnFolderCreateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (listener == null && context instanceof OnFolderCreateListener) {
            listener = (OnFolderCreateListener) context;
        } else if (listener == null && getParentFragment() instanceof OnFolderCreateListener) {
            listener = (OnFolderCreateListener) getParentFragment();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String parentPath = getArguments() != null ? getArguments().getString(ARG_PARENT_PATH, "/") : "/";

        Context context = requireContext();
        final EditText input = new EditText(context);
        input.setHint(R.string.folder_create_hint);
        input.setSingleLine(true);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.folder_create_title)
                .setView(input)
                .setPositiveButton(R.string.folder_create, null) // Set null to override later for validation
                .setNegativeButton(R.string.btn_cancel, (d, which) -> dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String folderName = input.getText() != null ? input.getText().toString() : "";
                FolderValidationHelper.ValidationError error = FolderValidationHelper.validateFolderName(folderName);
                if (error != FolderValidationHelper.ValidationError.NONE) {
                    input.setError(getErrorMessage(error));
                    return;
                }

                if (listener != null) {
                    listener.onFolderCreateConfirmed(parentPath, folderName);
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
            case INVALID_CHARACTERS:
                return getString(R.string.folder_name_invalid_chars);
            case TOO_LONG:
                return getString(R.string.folder_name_too_long);
            case ENDS_WITH_PERIOD_OR_SPACE:
                return getString(R.string.folder_name_edge);
            default:
                return getString(R.string.folder_name_empty);
        }
    }
}
