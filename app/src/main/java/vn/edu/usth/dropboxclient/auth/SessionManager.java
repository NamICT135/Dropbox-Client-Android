package vn.edu.usth.dropboxclient.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.dropbox.core.oauth.DbxCredential;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Credentials are encrypted at rest; the AES key never leaves Android Keystore. */
public final class SessionManager {
    private static final String ALIAS = "dropbox_session_key";
    private static final Object LOCK = new Object();
    private static final String REVISION = "revision";
    private final SharedPreferences preferences;
    public SessionManager(Context context) {
        preferences = context.getSharedPreferences("dropbox_session", Context.MODE_PRIVATE);
    }
    private SecretKey key() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    public long revision() {
        synchronized (LOCK) { return preferences.getLong(REVISION, 0); }
    }
    /** Keeps the current credential while invalidating older lookups and OAuth attempts. */
    public long beginOAuth() throws IOException {
        synchronized (LOCK) {
            long next = revision() + 1;
            if (!preferences.edit().putLong(REVISION, next).commit())
                throw new IOException("Unable to begin Dropbox authorization");
            return next;
        }
    }
    public void save(DbxCredential credential) throws GeneralSecurityException, IOException {
        synchronized (LOCK) { saveIfCurrent(credential, revision()); }
    }
    /** A late OAuth result may only replace the session present when OAuth began. */
    public boolean saveIfCurrent(DbxCredential credential, long expected)
            throws GeneralSecurityException, IOException {
        if (credential == null) throw new IllegalArgumentException("Missing credential");
        synchronized (LOCK) {
            if (revision() != expected) return false;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            String data = Base64.encodeToString(cipher.doFinal(
                    credential.toString().getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            if (!preferences.edit().putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString("credential", data).putLong(REVISION, expected + 1).commit())
                throw new IOException("Session write failed");
            return true;
        }
    }
    public DbxCredential load() throws GeneralSecurityException, IOException {
        synchronized (LOCK) {
            String data = preferences.getString("credential", null);
            if (data == null) return null;
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128,
                        Base64.decode(preferences.getString("iv", ""), Base64.NO_WRAP)));
                return DbxCredential.Reader.readFully(new String(cipher.doFinal(
                        Base64.decode(data, Base64.NO_WRAP)), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException | com.dropbox.core.json.JsonReadException e) {
                throw new IOException("Invalid session data", e);
            }
        }
    }
    /** Invalidates work started before logout or disconnect, even when no credential was saved. */
    public boolean clear() {
        synchronized (LOCK) { return clearIfCurrent(revision()); }
    }
    public boolean clearIfCurrent(long expected) {
        synchronized (LOCK) {
            if (revision() != expected) return false;
            return preferences.edit().remove("iv").remove("credential")
                    .putLong(REVISION, expected + 1).commit();
        }
    }
}
