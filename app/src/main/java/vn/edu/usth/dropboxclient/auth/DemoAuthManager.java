package vn.edu.usth.dropboxclient.auth;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Device-local academic accounts. This does not grant Dropbox access. */
public final class DemoAuthManager {
    public enum Result { SUCCESS, INVALID_USERNAME, INVALID_PASSWORD, USER_EXISTS, INVALID_CREDENTIALS, INVALID_PROFILE }
    private static final int ITERATIONS = 120_000;
    private final SharedPreferences preferences;

    public DemoAuthManager(Context context) {
        this(context.getSharedPreferences("demo_accounts", Context.MODE_PRIVATE));
    }

    DemoAuthManager(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public String currentUsername() { return preferences.getString("current_user", null); }

    public String currentDisplayName() {
        String user = currentUsername();
        if (user == null) return "";
        String name = preferences.getString("profile." + user + ".name", "");
        return name.isEmpty() ? user : name;
    }

    public String currentEmail() {
        String user = currentUsername();
        return user == null ? "" : preferences.getString("profile." + user + ".email", "");
    }

    // Call register/login on a worker thread: password derivation is intentionally expensive.
    public synchronized Result register(String username, char[] password)
            throws GeneralSecurityException, IOException {
        return register(username, password, "", "", "");
    }

    public synchronized Result register(String username, char[] password,
                                        String firstName, String lastName, String email)
            throws GeneralSecurityException, IOException {
        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim();
        if (firstName.length() > 60 || lastName.length() > 60 || email.length() > 254
                || (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
            return Result.INVALID_PROFILE;
        String user = normalize(username);
        if (!user.matches("[a-z0-9_]{3,24}")) return Result.INVALID_USERNAME;
        if (password.length < 8 || password.length > 128) return Result.INVALID_PASSWORD;
        if (preferences.contains("user." + user)) return Result.USER_EXISTS;
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] digest = derive(password, salt);
        String record = encode(salt) + ":" + encode(digest);
        if (!preferences.edit().putString("user." + user, record)
                .putString("profile." + user + ".name", (firstName + " " + lastName).trim())
                .putString("profile." + user + ".email", email)
                .putString("current_user", user).commit()) throw new IOException("Unable to save demo account");
        return Result.SUCCESS;
    }

    public synchronized Result login(String username, char[] password)
            throws GeneralSecurityException, IOException {
        String user = normalize(username);
        if (!user.matches("[a-z0-9_]{3,24}") || password.length < 8 || password.length > 128)
            return Result.INVALID_CREDENTIALS;
        String record = preferences.getString("user." + user, null);
        if (record == null) return Result.INVALID_CREDENTIALS;
        try {
            String[] parts = record.split(":", -1);
            if (parts.length != 2) throw new IOException("Invalid demo account data");
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            if (salt.length != 16 || expected.length != 32) throw new IOException("Invalid demo account data");
            byte[] actual = derive(password, salt);
            boolean matches = MessageDigest.isEqual(expected, actual);
            Arrays.fill(actual, (byte) 0);
            if (!matches) return Result.INVALID_CREDENTIALS;
            if (!preferences.edit().putString("current_user", user).commit())
                throw new IOException("Unable to save demo session");
            return Result.SUCCESS;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid demo account data", e);
        }
    }

    /** Removes only the demo session. The Account screen also clears Dropbox on full logout. */
    public boolean logout() { return preferences.edit().remove("current_user").commit(); }

    private static String normalize(String username) { return username.trim().toLowerCase(Locale.ROOT); }
    private static String encode(byte[] data) { return Base64.getEncoder().encodeToString(data); }
    private static byte[] derive(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
