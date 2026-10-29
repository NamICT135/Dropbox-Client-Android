package vn.edu.usth.dropboxclient;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.UUID;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.ui.transfer.TransferActivity;
import vn.edu.usth.dropboxclient.ui.transfer.FilePickerHelper;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Picker and transfer-screen acceptance without a cloud account or new dependencies. */
@RunWith(AndroidJUnit4.class)
public class TransferFlowTest {
    private Context context;
    private SharedPreferences preferences;
    private String previousUser;
    private String username;

    @Before public void createLocalSession() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assume.assumeTrue("Use an unconnected test device", new SessionManager(context).load() == null);
        preferences = context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE);
        previousUser = preferences.getString("current_user", null);
        username = "flow_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        char[] password = UUID.randomUUID().toString().toCharArray();
        try {
            assertEquals(DemoAuthManager.Result.SUCCESS, new DemoAuthManager(context).register(username, password));
        } finally { Arrays.fill(password, (char) 0); }
    }

    @After public void restoreLocalSession() {
        if (preferences == null) return;
        SharedPreferences.Editor restore = preferences.edit().remove("user." + username)
                .remove("profile." + username + ".name").remove("profile." + username + ".email");
        if (previousUser == null) restore.remove("current_user");
        else restore.putString("current_user", previousUser);
        assertTrue(restore.commit());
    }

    private Intent fileIntent(String path) {
        return new Intent(context, TransferActivity.class)
                .putExtra(TransferActivity.EXTRA_DOWNLOAD_PATH, path);
    }

    private void awaitResult(int message) {
        long deadline = SystemClock.uptimeMillis() + 10_000;
        AssertionError last = null;
        do {
            try {
                onView(withId(R.id.transfer_result)).perform(scrollTo()).check(matches(withText(message)));
                return;
            } catch (AssertionError e) { last = e; }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw last;
    }

    @Test public void fileFromSearchKeepsItsParentFolderAfterRecreation() {
        try (ActivityScenario<TransferActivity> scenario = ActivityScenario.launch(fileIntent("/course/slides.pdf"))) {
            onView(withId(R.id.transfer_folder)).check(matches(withText(context.getString(R.string.transfer_folder, "/course"))));
            onView(withId(R.id.transfer_path)).perform(scrollTo()).check(matches(withText("/course/slides.pdf")));
            scenario.recreate();
            onView(withId(R.id.transfer_folder)).check(matches(withText(context.getString(R.string.transfer_folder, "/course"))));
            onView(withId(R.id.transfer_path)).perform(scrollTo()).check(matches(withText("/course/slides.pdf")));
        }
    }

    @Test public void savePickerAdvertisesFileMime() {
        assertEquals("application/pdf", FilePickerHelper.createDownloadIntent("report.pdf").getType());
        assertEquals("image/jpeg", FilePickerHelper.createDownloadIntent("Ảnh.JPG").getType());
        assertEquals("image/png", FilePickerHelper.createDownloadIntent("image.png").getType());
        assertEquals("application/octet-stream", FilePickerHelper.createDownloadIntent("unknown.zzz").getType());
    }

    @Test public void cancellingBothDocumentPickersReenablesTransferActions() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        IntentFilter uploadFilter = new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);
        uploadFilter.addCategory(Intent.CATEGORY_OPENABLE);
        uploadFilter.addDataType("*/*");
        IntentFilter downloadFilter = new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);
        downloadFilter.addCategory(Intent.CATEGORY_OPENABLE);
        downloadFilter.addDataType("*/*");
        Instrumentation.ActivityResult cancelled = new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
        Instrumentation.ActivityMonitor upload = instrumentation.addMonitor(uploadFilter, cancelled, true);
        Instrumentation.ActivityMonitor download = instrumentation.addMonitor(downloadFilter, cancelled, true);
        try (ActivityScenario<TransferActivity> scenario = ActivityScenario.launch(fileIntent("/course/slides.pdf"))) {
            onView(withId(R.id.transfer_upload)).perform(scrollTo(), click());
            awaitResult(R.string.transfer_cancelled);
            assertEquals(1, upload.getHits());
            onView(withId(R.id.transfer_download)).perform(scrollTo(), click());
            awaitResult(R.string.transfer_cancelled);
            assertEquals(1, download.getHits());
            onView(withId(R.id.transfer_upload)).check(matches(isEnabled()));
            onView(withId(R.id.transfer_download)).check(matches(isEnabled()));
            onView(withId(R.id.transfer_preview)).check(matches(isEnabled()));
            scenario.recreate();
            onView(withId(R.id.transfer_download)).perform(scrollTo()).check(matches(isEnabled()));
        } finally {
            instrumentation.removeMonitor(upload);
            instrumentation.removeMonitor(download);
        }
    }

    @Test public void invalidPathsUnsupportedPreviewsAndMissingConnectionAreRecoverable() {
        try (ActivityScenario<TransferActivity> ignored = ActivityScenario.launch(fileIntent("invalid"))) {
            onView(withId(R.id.transfer_download)).perform(scrollTo(), click());
            awaitResult(R.string.transfer_invalid_path);
            onView(withId(R.id.transfer_path)).perform(scrollTo(), replaceText("/course/archive.zip"), closeSoftKeyboard());
            onView(withId(R.id.transfer_preview)).perform(scrollTo(), click());
            awaitResult(R.string.preview_unsupported);
            onView(withId(R.id.transfer_path)).perform(scrollTo(), replaceText("/course/slides.pdf"), closeSoftKeyboard());
            onView(withId(R.id.transfer_preview)).perform(scrollTo(), click());
            awaitResult(R.string.transfer_session_error);
            onView(withId(R.id.transfer_preview)).check(matches(isEnabled()));
            onView(withId(R.id.transfer_download)).check(matches(isEnabled()));
        }
    }
}
