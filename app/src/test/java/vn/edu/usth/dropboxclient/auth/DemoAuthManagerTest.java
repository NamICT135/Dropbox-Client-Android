package vn.edu.usth.dropboxclient.auth;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DemoAuthManagerTest {
    private final Map<String, Object> values = new HashMap<>();
    // A small in-memory preferences implementation; no Android runtime or network required.
    private SharedPreferences preferences(boolean writesSucceed) {
        return (SharedPreferences) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SharedPreferences.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getString": return values.getOrDefault((String) args[0], args[1]);
                        case "contains": return values.containsKey(args[0]);
                        case "edit": return editor(writesSucceed);
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
    private SharedPreferences.Editor editor(boolean writesSucceed) {
        Map<String, Object> pending = new HashMap<>();
        return (SharedPreferences.Editor) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "putString": pending.put((String) args[0], args[1]); return proxy;
                        case "remove": pending.put((String) args[0], null); return proxy;
                        case "commit":
                        case "apply":
                            if (writesSucceed) pending.forEach((k, v) -> {
                                if (v == null) values.remove(k); else values.put(k, v);
                            });
                            return method.getName().equals("commit") ? writesSucceed : null;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
    @Test public void registerRestoreLogoutAndLoginWithoutEmail() throws Exception {
        SharedPreferences preferences = preferences(true);
        DemoAuthManager demo = new DemoAuthManager(preferences);
        char[] secret = UUID.randomUUID().toString().toCharArray();
        assertEquals(DemoAuthManager.Result.SUCCESS, demo.register(" Student_01 ", secret));
        assertEquals("student_01", new DemoAuthManager(preferences).currentUsername());
        demo.logout();
        assertNull(demo.currentUsername());
        assertEquals(DemoAuthManager.Result.SUCCESS, demo.login("STUDENT_01", secret));
        assertEquals("student_01", demo.currentUsername());
    }
    @Test public void rejectsDuplicateBadCredentialsAndEmailUsername() throws Exception {
        DemoAuthManager demo = new DemoAuthManager(preferences(true));
        char[] secret = UUID.randomUUID().toString().toCharArray();
        assertEquals(DemoAuthManager.Result.INVALID_USERNAME, demo.register("email@example.test", secret));
        assertEquals(DemoAuthManager.Result.INVALID_PASSWORD, demo.register("student", new char[0]));
        assertEquals(DemoAuthManager.Result.SUCCESS, demo.register("student", secret));
        assertEquals(DemoAuthManager.Result.USER_EXISTS, demo.register("STUDENT", secret));
        demo.logout();
        assertEquals(DemoAuthManager.Result.INVALID_CREDENTIALS,
                demo.login("student", UUID.randomUUID().toString().toCharArray()));
        assertNull(demo.currentUsername());
        assertEquals(DemoAuthManager.Result.INVALID_CREDENTIALS, demo.login("missing", secret));
    }
    @Test public void samePasswordHasDifferentSaltAndIsNotStoredAsPlaintext() throws Exception {
        DemoAuthManager demo = new DemoAuthManager(preferences(true));
        String secret = UUID.randomUUID().toString();
        demo.register("student_one", secret.toCharArray());
        demo.register("student_two", secret.toCharArray());
        assertNotEquals(values.get("user.student_one"), values.get("user.student_two"));
        assertFalse(values.get("user.student_one").toString().contains(secret));
    }
    @Test public void storageFailureIsReported() {
        DemoAuthManager demo = new DemoAuthManager(preferences(false));
        assertThrows(java.io.IOException.class,
                () -> demo.register("student", UUID.randomUUID().toString().toCharArray()));
        assertNull(demo.currentUsername());
    }
    @Test public void profileSurvivesLogoutAndIsScopedToUser() throws Exception {
        SharedPreferences store = preferences(true);
        DemoAuthManager demo = new DemoAuthManager(store);
        char[] secret = UUID.randomUUID().toString().toCharArray();
        assertEquals(DemoAuthManager.Result.SUCCESS,
                demo.register("alice", secret, " An ", " Nguyen ", " an@example.test "));
        assertEquals("An Nguyen", demo.currentDisplayName());
        assertEquals("an@example.test", demo.currentEmail());
        assertTrue(demo.logout());
        assertEquals("", demo.currentDisplayName());
        assertEquals("", demo.currentEmail());
        demo.register("bob", secret);
        assertEquals("bob", demo.currentDisplayName());
        assertEquals("", demo.currentEmail());
        demo.logout();
        DemoAuthManager restored = new DemoAuthManager(store);
        restored.login("ALICE", secret);
        assertEquals("An Nguyen", restored.currentDisplayName());
        assertEquals("an@example.test", restored.currentEmail());
    }
    @Test public void invalidProfileDoesNotCreateAnAccount() throws Exception {
        DemoAuthManager demo = new DemoAuthManager(preferences(true));
        char[] secret = UUID.randomUUID().toString().toCharArray();
        assertEquals(DemoAuthManager.Result.INVALID_PROFILE,
                demo.register("alice", secret, "An", "Nguyen", "invalid-email"));
        assertNull(demo.currentUsername());
        assertEquals(DemoAuthManager.Result.SUCCESS, demo.register("alice", secret));
    }
    @Test public void failedLogoutReportsFailureAndKeepsSession() throws Exception {
        DemoAuthManager demo = new DemoAuthManager(preferences(true));
        demo.register("alice", UUID.randomUUID().toString().toCharArray());
        assertFalse(new DemoAuthManager(preferences(false)).logout());
        assertEquals("alice", demo.currentUsername());
    }
}
