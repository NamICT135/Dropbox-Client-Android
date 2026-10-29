package vn.edu.usth.dropboxclient;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import vn.edu.usth.dropboxclient.ui.transfer.DownloadMimeType;
import vn.edu.usth.dropboxclient.ui.transfer.VerifiedDownload;
import static org.junit.Assert.*;

public class DownloadSafetyTest {
    @Test public void savePickerUsesSpecificMimeForKnownExtensions() {
        assertEquals("application/pdf", DownloadMimeType.forName("Báo cáo.PDF"));
        assertEquals("image/jpeg", DownloadMimeType.forName("ảnh.JPG"));
        assertEquals("image/jpeg", DownloadMimeType.forName("photo.jpeg"));
        assertEquals("image/png", DownloadMimeType.forName("image.PNG"));
        assertEquals("application/octet-stream", DownloadMimeType.forName("unknown.custom"));
        assertEquals("application/octet-stream", DownloadMimeType.forName("no-extension"));
    }

    @Test public void sizeAndHashMustMatchBeforeDestinationIsOpened() throws Exception {
        File temp = File.createTempFile("download-test-", ".tmp");
        try {
            Files.write(temp.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
            assertEquals("4f8b42c22dd3729b519ba6f68d2da7cc5b2d606d05daed5ad5128cc03e6c6358",
                    VerifiedDownload.hash(temp));
            VerifiedDownload.verify(temp, 3, VerifiedDownload.hash(temp));
            assertThrows(IOException.class, () -> VerifiedDownload.verify(temp, 4, null));
            assertThrows(IOException.class, () -> VerifiedDownload.verify(temp, 3, "bad-hash"));
        } finally { Files.deleteIfExists(temp.toPath()); }
    }

    @Test public void writeFailureAfterPartialCopyIsNotReportedAsSuccess() throws Exception {
        File temp = File.createTempFile("download-partial-", ".tmp");
        try {
            Files.write(temp.toPath(), new byte[20000]);
            AtomicInteger written = new AtomicInteger();
            OutputStream failing = new OutputStream() {
                @Override public void write(int value) throws IOException {
                    if (written.incrementAndGet() > 9000) throw new IOException("out of space");
                }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                    for (int i = offset; i < offset + length; i++) write(bytes[i]);
                }
            };
            assertThrows(IOException.class, () -> VerifiedDownload.copy(temp, failing));
            assertTrue("Failure must happen after bytes were written", written.get() > 9000);
        } finally { Files.deleteIfExists(temp.toPath()); }
    }
}
