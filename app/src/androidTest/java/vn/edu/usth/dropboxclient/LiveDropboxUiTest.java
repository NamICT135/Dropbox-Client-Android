package vn.edu.usth.dropboxclient;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.ui.main.MainActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.allOf;

/** Opt-in UI/API round trip. System picker/viewer results are deterministic test doubles. */
@RunWith(AndroidJUnit4.class)
public class LiveDropboxUiTest {
    private void await(Runnable assertion) {
        long deadline = SystemClock.uptimeMillis() + 30_000;
        AssertionError last = null;
        do {
            try { assertion.run(); return; }
            catch (NoMatchingViewException | AssertionError e) { last = new AssertionError(e); }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw last;
    }

    private Instrumentation.ActivityMonitor monitor(String action, String mime,
                                                     Instrumentation.ActivityResult result) throws Exception {
        IntentFilter filter = new IntentFilter(action);
        if (Intent.ACTION_OPEN_DOCUMENT.equals(action) || Intent.ACTION_CREATE_DOCUMENT.equals(action))
            filter.addCategory(Intent.CATEGORY_OPENABLE);
        filter.addDataType(mime);
        return InstrumentationRegistry.getInstrumentation().addMonitor(filter, result, true);
    }

    @Test public void filesUploadSearchDownloadAndPreviewUseAuthorizedDropbox() throws Exception {
        Assume.assumeTrue("Explicitly enable on an authorized test device",
                "true".equals(InstrumentationRegistry.getArguments().getString("liveDropboxUi")));
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        assertNotNull("Connect Dropbox before running", new SessionManager(context).load());
        SharedPreferences preferences = context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE);
        String previousUser = preferences.getString("current_user", null);
        String id = UUID.randomUUID().toString().replace("-", "");
        String username = "ui_" + id.substring(0, 12);
        Path directory = context.getCacheDir().toPath().resolve("previews");
        Files.createDirectories(directory);
        Path input = directory.resolve("ui_acceptance_" + id + ".png");
        Path output = Files.createTempFile(directory, "ui-download-", ".png");
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xff2463eb);
        try (OutputStream stream = Files.newOutputStream(input)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally { bitmap.recycle(); }
        char[] password = UUID.randomUUID().toString().toCharArray();
        Instrumentation.ActivityMonitor upload = null, download = null, preview = null;
        try {
            assertEquals(DemoAuthManager.Result.SUCCESS, new DemoAuthManager(context).register(username, password));
            upload = monitor(Intent.ACTION_OPEN_DOCUMENT, "*/*", new Instrumentation.ActivityResult(
                    Activity.RESULT_OK, new Intent().setData(FileProvider.getUriForFile(context,
                            context.getPackageName() + ".preview", input.toFile()))));
            download = monitor(Intent.ACTION_CREATE_DOCUMENT, "*/*", new Instrumentation.ActivityResult(
                    Activity.RESULT_OK, new Intent().setData(FileProvider.getUriForFile(context,
                            context.getPackageName() + ".preview", output.toFile()))));
            preview = monitor(Intent.ACTION_VIEW, "image/png", new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));
            try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
                onView(withId(R.id.nav_files)).perform(click());
                await(() -> onView(withId(R.id.files_upload)).check(matches(isEnabled())));
                onView(withId(R.id.files_upload)).perform(click());
                await(() -> onView(withId(R.id.transfer_upload)).check(matches(isDisplayed())));
                onView(withId(R.id.transfer_upload)).perform(scrollTo(), click());
                String cloudPath = "/" + input.getFileName();
                await(() -> onView(withId(R.id.transfer_path)).perform(scrollTo()).check(matches(withText(cloudPath))));
                assertEquals(1, upload.getHits());
                onView(withId(R.id.transfer_download)).perform(scrollTo(), click());
                await(() -> onView(withId(R.id.transfer_result)).perform(scrollTo())
                        .check(matches(withText(context.getString(R.string.transfer_download_success, input.getFileName(), input.toFile().length())))));
                assertEquals(1, download.getHits());
                assertArrayEquals(Files.readAllBytes(input), Files.readAllBytes(output));
                onView(withId(R.id.transfer_back)).perform(scrollTo(), click());
                onView(withId(R.id.nav_search)).perform(click());
                await(() -> onView(withId(R.id.search_query)).check(matches(isDisplayed())));
                onView(withId(R.id.search_query)).perform(replaceText(input.getFileName().toString()), closeSoftKeyboard());
                onView(withId(R.id.search_button)).perform(click());
                await(() -> onView(allOf(withId(R.id.file_name), withText(input.getFileName().toString())))
                        .check(matches(isDisplayed())));
                onView(allOf(withId(R.id.file_name), withText(input.getFileName().toString()))).perform(click());
                await(() -> onView(withId(R.id.transfer_preview)).perform(scrollTo()).check(matches(isDisplayed())));
                onView(withId(R.id.transfer_preview)).perform(scrollTo(), click());
                await(() -> onView(withId(R.id.transfer_result)).perform(scrollTo()).check(matches(withText(R.string.preview_opening))));
                assertEquals(1, preview.getHits());
            }
            // Leave only this uniquely named tiny synthetic cloud fixture for verification.
        } finally {
            Arrays.fill(password, (char) 0);
            if (upload != null) instrumentation.removeMonitor(upload);
            if (download != null) instrumentation.removeMonitor(download);
            if (preview != null) instrumentation.removeMonitor(preview);
            SharedPreferences.Editor restore = preferences.edit().remove("user." + username)
                    .remove("profile." + username + ".name").remove("profile." + username + ".email");
            if (previousUser == null) restore.remove("current_user"); else restore.putString("current_user", previousUser);
            assertTrue(restore.commit());
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

}
