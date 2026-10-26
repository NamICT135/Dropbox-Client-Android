package vn.edu.usth.dropboxclient;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.dropboxclient.ui.transfer.DropboxTransferService;
import static org.junit.Assert.*;

/** Real Android stream handling against deterministic HTTP responses, without cloud credentials. */
@RunWith(AndroidJUnit4.class)
public class DropboxTransferServiceIoTest {
    private static final byte[] CONTENT = {0, 1, 2, (byte) 255, 10};
    private static final String METADATA = "{\"name\":\"roundtrip.bin\",\"id\":\"id:test123\","
            + "\"path_lower\":\"/roundtrip.bin\",\"path_display\":\"/roundtrip.bin\","
            + "\"client_modified\":\"2026-10-22T00:00:00Z\","
            + "\"server_modified\":\"2026-10-22T00:00:00Z\",\"rev\":\"123456789\",\"size\":5}";
    private static final class Transport extends HttpRequestor {
        final boolean rejectDownload;
        final String metadata;
        byte[] uploaded;
        Transport(boolean rejectDownload) { this(rejectDownload, METADATA); }
        Transport(boolean rejectDownload, String metadata) {
            this.rejectDownload = rejectDownload;
            this.metadata = metadata;
        }
        @Override public Response doGet(String url, Iterable<Header> headers) { throw new AssertionError("Expected POST"); }
        @Override public Uploader startPut(String url, Iterable<Header> headers) { throw new AssertionError("Expected POST"); }
        @Override public Uploader startPost(String url, Iterable<Header> headers) {
            return new Uploader() {
                final ByteArrayOutputStream body = new ByteArrayOutputStream();
                @Override public OutputStream getBody() { return body; }
                @Override public void close() { }
                @Override public void abort() { }
                @Override public Response finish() {
                    if (url.endsWith("/files/download")) {
                        if (rejectDownload) {
                            String error = "{\"error_summary\":\"path/not_found/\","
                                    + "\"error\":{\".tag\":\"path\",\"path\":{\".tag\":\"not_found\"}}}";
                            return new Response(409, new ByteArrayInputStream(error.getBytes(StandardCharsets.UTF_8)),
                                    Collections.emptyMap());
                        }
                        return new Response(200, new ByteArrayInputStream(CONTENT),
                                Collections.singletonMap("Dropbox-API-Result", Collections.singletonList(metadata)));
                    }
                    assertTrue(url.endsWith("/files/upload"));
                    uploaded = body.toByteArray();
                    return new Response(200, new ByteArrayInputStream(METADATA.getBytes(StandardCharsets.UTF_8)),
                            Collections.emptyMap());
                }
            };
        }
    }
    private DropboxTransferService service(Context context, Transport transport) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("transfer-io-test").withHttpRequestor(transport).build();
        return new DropboxTransferService(context, new DbxClientV2(config, UUID.randomUUID().toString()));
    }
    @Test public void streamsAndroidFilesThroughRepositoryWithoutChangingBytes() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Path input = Files.createTempFile(context.getCacheDir().toPath(), "transfer-in-", ".bin");
        Path output = Files.createTempFile(context.getCacheDir().toPath(), "transfer-out-", ".bin");
        try {
            Files.write(input, CONTENT);
            Transport transport = new Transport(false);
            DropboxTransferService service = service(context, transport);
            service.uploadFile(Uri.fromFile(input.toFile()), "/roundtrip.bin");
            assertArrayEquals(CONTENT, transport.uploaded);
            service.downloadFile("/roundtrip.bin", Uri.fromFile(output.toFile()));
            assertArrayEquals(CONTENT, Files.readAllBytes(output));
        } finally { Files.deleteIfExists(input); Files.deleteIfExists(output); }
    }
    @Test public void rejectedDownloadDoesNotTruncateExistingDestination() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Path output = Files.createTempFile(context.getCacheDir().toPath(), "transfer-kept-", ".bin");
        try {
            Files.write(output, CONTENT);
            DropboxTransferService service = service(context, new Transport(true));
            assertThrows(DbxException.class, () -> service.downloadFile("/missing.bin", Uri.fromFile(output.toFile())));
            assertArrayEquals(CONTENT, Files.readAllBytes(output));
        } finally { Files.deleteIfExists(output); }
    }

    @Test public void previewChecksSizeBeforeWritingAndUsesPrivateContentUri() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Path directory = context.getCacheDir().toPath().resolve("previews");
        Files.createDirectories(directory);
        Path output = Files.createTempFile(directory, "preview-test-", ".pdf");
        try {
            Files.write(output, CONTENT);
            DropboxTransferService service = service(context, new Transport(false));
            assertThrows(DropboxTransferService.PreviewTooLargeException.class,
                    () -> service.downloadPreview("/roundtrip.bin", output.toFile(), 4));
            assertArrayEquals(CONTENT, Files.readAllBytes(output));
            service.downloadPreview("/roundtrip.bin", output.toFile(), 5);
            assertArrayEquals(CONTENT, Files.readAllBytes(output));
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".preview", output.toFile());
            assertEquals("content", uri.getScheme());
        } finally { Files.deleteIfExists(output); }
    }

    @Test public void previewRejectsActualBytesExceedingReportedSize() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Path output = Files.createTempFile(context.getCacheDir().toPath(), "preview-limit-", ".bin");
        try {
            Transport transport = new Transport(false, METADATA.replace("\"size\":5", "\"size\":1"));
            DropboxTransferService service = service(context, transport);
            assertThrows(DropboxTransferService.PreviewTooLargeException.class,
                    () -> service.downloadPreview("/roundtrip.bin", output.toFile(), 4));
            assertTrue(Files.size(output) <= 4);
        } finally { Files.deleteIfExists(output); }
    }
}
