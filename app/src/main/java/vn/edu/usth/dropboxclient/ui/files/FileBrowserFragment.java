package vn.edu.usth.dropboxclient.ui.files;

import androidx.fragment.app.Fragment;

import vn.edu.usth.dropboxclient.R;

public class FileBrowserFragment extends Fragment {

    public FileBrowserFragment() {
        super(R.layout.fragment_file_browser);
    }

    private androidx.recyclerview.widget.RecyclerView recyclerViewFiles;
    private android.widget.ProgressBar progressBarLoading;
    private android.widget.LinearLayout layoutEmpty;
    private android.widget.LinearLayout layoutError;
    private android.widget.Button btnRetry;

    @Override
    public void onViewCreated(android.view.View view, android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View từ XML
        recyclerViewFiles = view.findViewById(R.id.recyclerViewFiles);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);
        btnRetry = view.findViewById(R.id.btnRetry);

        // Xử lý sự kiện nút Thử lại khi có lỗi
        btnRetry.setOnClickListener(v -> {
            // Gọi lại hàm tải dữ liệu của bạn ở đây
        });

    }

    // Hàm chuyển đổi trạng thái giao diện linh hoạt
    public void showState(State state) {
        recyclerViewFiles.setVisibility(android.view.View.GONE);
        progressBarLoading.setVisibility(android.view.View.GONE);
        layoutEmpty.setVisibility(android.view.View.GONE);
        layoutError.setVisibility(android.view.View.GONE);
        switch (state) {
            case LOADING:
                progressBarLoading.setVisibility(android.view.View.VISIBLE);
                break;
            case SUCCESS:
                recyclerViewFiles.setVisibility(android.view.View.VISIBLE);
                break;
            case EMPTY:
                layoutEmpty.setVisibility(android.view.View.VISIBLE);
                break;
            case ERROR:
                layoutError.setVisibility(android.view.View.VISIBLE);
                break;
        }
    }

    public enum State {
        LOADING, SUCCESS, EMPTY, ERROR

    }

}