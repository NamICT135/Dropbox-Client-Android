package vn.edu.usth.dropboxclient.ui.main;

import android.os.Bundle;
import android.content.Intent;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.ui.account.AccountFragment;
import vn.edu.usth.dropboxclient.ui.files.FileBrowserFragment;
import vn.edu.usth.dropboxclient.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_tab";

    private int selectedTab = R.id.nav_account;
    private String nextFilesPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (new DemoAuthManager(this).currentUsername() == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left, bars.top, bars.right, bars.bottom
                    );

                    return insets;
                }
        );

        BottomNavigationView navigation = findViewById(R.id.navigation);

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(
                    KEY_SELECTED_TAB,
                    R.id.nav_account
            );
        }

        navigation.setSelectedItemId(selectedTab);

        // FragmentManager tự khôi phục Fragment khi Activity được tạo lại.
        if (savedInstanceState == null) {
            showTab(selectedTab);
        }

        navigation.setOnItemSelectedListener(item -> {
            int checkedId = item.getItemId();
            if (checkedId == R.id.nav_files
                    || checkedId == R.id.nav_search
                    || checkedId == R.id.nav_account) {
                selectedTab = checkedId;
                showTab(checkedId);
                return true;
            }
            return false;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (new DemoAuthManager(this).currentUsername() == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    private void showTab(int tabId) {
        Fragment screen;

        if (tabId == R.id.nav_account) {
            screen = new AccountFragment();
        } else if (tabId == R.id.nav_search) {
            screen = new SearchFragment();
        } else {
            screen = nextFilesPath == null ? new FileBrowserFragment()
                    : FileBrowserFragment.newInstance(nextFilesPath);
            nextFilesPath = null;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, screen)
                .commit();
    }

    public void openFiles(String path) {
        nextFilesPath = path;
        ((BottomNavigationView) findViewById(R.id.navigation)).setSelectedItemId(R.id.nav_files);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }
}
