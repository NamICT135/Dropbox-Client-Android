package vn.edu.usth.dropboxclient.ui.transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Verifies a completed temporary download before touching a document provider. */
public final class VerifiedDownload {
    private static final int BLOCK_SIZE = 4 * 1024 * 1024;
    private VerifiedDownload() { }

    public static void verify(File file, long expectedSize, String contentHash) throws IOException {
        if (expectedSize < 0 || file.length() != expectedSize)
            throw new IOException("Downloaded byte count does not match Dropbox metadata");
        if (contentHash != null && !contentHash.equalsIgnoreCase(hash(file)))
            throw new IOException("Downloaded content hash does not match Dropbox metadata");
    }

    // Dropbox content_hash = SHA-256 of concatenated SHA-256 hashes of 4 MiB blocks.
    public static String hash(File file) throws IOException {
        try {
            MessageDigest all = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (FileInputStream input = new FileInputStream(file)) {
                int read;
                int inBlock = 0;
                MessageDigest block = MessageDigest.getInstance("SHA-256");
                while ((read = input.read(buffer)) != -1) {
                    int offset = 0;
                    while (offset < read) {
                        int count = Math.min(read - offset, BLOCK_SIZE - inBlock);
                        block.update(buffer, offset, count);
                        offset += count;
                        inBlock += count;
                        if (inBlock == BLOCK_SIZE) {
                            all.update(block.digest());
                            block = MessageDigest.getInstance("SHA-256");
                            inBlock = 0;
                        }
                    }
                }
                if (inBlock > 0) all.update(block.digest());
            }
            StringBuilder result = new StringBuilder(64);
            for (byte b : all.digest()) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    public static void copy(File file, OutputStream output) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            FileStreamHelper.copy(input, output);
        }
    }
}
