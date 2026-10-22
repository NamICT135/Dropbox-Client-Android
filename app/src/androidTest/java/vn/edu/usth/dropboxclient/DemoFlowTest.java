package vn.edu.usth.dropboxclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.EditText;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.auth.DemoAuthManager;
import vn.edu.usth.dropboxclient.auth.SessionManager;
import vn.edu.usth.dropboxclient.ui.login.LoginActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DemoFlowTest {
    private void awaitText(int id, int text) {
        long deadline = SystemClock.uptimeMillis() + 10_000;
        AssertionError last = null;
        do {
            try { onView(withId(id)).check(matches(withText(text))); return; }
            catch (NoMatchingViewException | AssertionError e) { last = new AssertionError(e); }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw last;
    }

    private void assertPasswordLength(int length) {
        onView(withId(R.id.demo_password)).check((view, error) -> {
            if (error != null) throw error;
            assertEquals("A failed attempt must preserve the entered password", length,
                    ((EditText) view).length());
        });
    }

    @Test public void failedRegistrationCanBeCorrectedAndExistingUserCanSwitchToLogin() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assume.assumeTrue("Use an unconnected demo device", new SessionManager(context).load() == null);
        SharedPreferences preferences = context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE);
        String previousUser = preferences.getString("current_user", null);
        String username = "retry_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        char[] password = UUID.randomUUID().toString().toCharArray();
        new DemoAuthManager(context).logout();
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.demo_username)).perform(replaceText("invalid name"));
            onView(withId(R.id.demo_password)).perform(replaceText(new String(password)), closeSoftKeyboard());
            onView(withId(R.id.button_register)).perform(scrollTo(), click());
            awaitText(R.id.login_status, R.string.demo_invalid_username);
            assertPasswordLength(password.length);
            onView(withId(R.id.demo_username)).perform(replaceText(username), closeSoftKeyboard());
            onView(withId(R.id.button_register)).perform(scrollTo(), click());
            awaitView(R.id.demo_account);
            onView(withId(R.id.account_logout)).perform(scrollTo(), click());
            awaitView(R.id.demo_username);
            onView(withId(R.id.demo_username)).perform(replaceText(username));
            onView(withId(R.id.demo_password)).perform(replaceText(new String(password)), closeSoftKeyboard());
            onView(withId(R.id.button_register)).perform(scrollTo(), click());
            awaitText(R.id.login_status, R.string.demo_user_exists);
            assertPasswordLength(password.length);
            onView(withId(R.id.button_continue)).perform(scrollTo(), click());
            awaitView(R.id.demo_account);
            onView(withId(R.id.account_logout)).perform(scrollTo(), click());
        } finally {
            Arrays.fill(password, (char) 0);
            SharedPreferences.Editor restore = preferences.edit().remove("user." + username);
            if (previousUser == null) restore.remove("current_user"); else restore.putString("current_user", previousUser);
            restore.commit();
        }
    }

    private void awaitView(int id) {
        long deadline = SystemClock.uptimeMillis() + 10_000;
        AssertionError last = null;
        do {
            try { onView(withId(id)).check(matches(isDisplayed())); return; }
            catch (NoMatchingViewException | AssertionError e) { last = new AssertionError(e); }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw last;
    }

    @Test public void registerLoginLogoutAndOpenProjectSetupWithoutEmail() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Never replace a real project credential or exercise a connected account from this UI test.
        Assume.assumeTrue("UI demo acceptance requires an unconnected test device",
                new SessionManager(context).load() == null);
        SharedPreferences preferences = context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE);
        String previousUser = preferences.getString("current_user", null);
        String username = "demo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        char[] password = UUID.randomUUID().toString().toCharArray();
        new DemoAuthManager(context).logout();
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.demo_username)).perform(replaceText(username));
            onView(withId(R.id.demo_password)).perform(replaceText(new String(password)), closeSoftKeyboard());
            onView(withId(R.id.button_register)).perform(click());
            awaitView(R.id.demo_account);
            onView(withId(R.id.demo_account)).check(matches(withText("Demo user: " + username)));
            onView(withId(R.id.account_connect)).perform(scrollTo(), click());
            onView(withId(R.id.project_connect)).check(matches(withText(R.string.project_connect)));
            onView(withId(R.id.project_back)).perform(scrollTo(), click());
            onView(withId(R.id.account_transfer_debug)).perform(scrollTo(), click());
            onView(withId(R.id.transfer_upload)).check(matches(isDisplayed()));
            onView(withId(R.id.transfer_path)).perform(replaceText("invalid_path"), closeSoftKeyboard());
            onView(withId(R.id.transfer_download)).perform(scrollTo(), click());
            onView(withId(R.id.transfer_result)).check(matches(withText(R.string.transfer_invalid_path)));
            onView(withId(R.id.transfer_back)).perform(scrollTo(), click());
            onView(withId(R.id.account_logout)).perform(scrollTo(), click());
            awaitView(R.id.demo_username);
            onView(withId(R.id.demo_username)).check(matches(isDisplayed()));
            onView(withId(R.id.demo_username)).perform(replaceText(username));
            onView(withId(R.id.demo_password)).perform(replaceText(new String(password)), closeSoftKeyboard());
            onView(withId(R.id.button_continue)).perform(click());
            awaitView(R.id.demo_account);
            onView(withId(R.id.demo_account)).check(matches(withText("Demo user: " + username)));
            onView(withId(R.id.account_logout)).perform(scrollTo(), click());
            awaitView(R.id.demo_username);
            onView(withId(R.id.demo_username)).check(matches(isDisplayed()));
        } finally {
            Arrays.fill(password, (char) 0);
            SharedPreferences.Editor restore = preferences.edit().remove("user." + username);
            if (previousUser == null) restore.remove("current_user"); else restore.putString("current_user", previousUser);
            restore.commit();
        }
    }
}
