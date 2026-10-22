
package vn.edu.usth.dropboxclient;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class DropboxSdkTransferMockTest {

    private static final String FILE_PATH = "/test.txt";

    private static final byte[] FILE_CONTENT =
            "Hello Dropbox Transfer!"
                    .getBytes(StandardCharsets.UTF_8);

    private static final String FILE_METADATA =
            "{"
                    + "\"name\":\"test.txt\","
                    + "\"id\":\"id:test123\","
                    + "\"path_lower\":\"/test.txt\","
                    + "\"path_display\":\"/test.txt\","
                    + "\"client_modified\":\"2026-10-22T00:00:00Z\","
                    + "\"server_modified\":\"2026-10-22T00:00:00Z\","
                    + "\"rev\":\"123456789\","
                    + "\"size\":23"
                    + "}";

    // Fake HTTP transport for Dropbox SDK
    private static class FakeHttpRequestor
            extends HttpRequestor {

        String requestedUrl;
        byte[] uploadedBytes;

        @Override
        public Response doGet(
                String url,
                Iterable<Header> headers
        ) {
            throw new AssertionError(
                    "Dropbox transfer should not use HTTP GET"
            );
        }

        @Override
        public Uploader startPost(
                String url,
                Iterable<Header> headers
        ) {
            return createUploader(url);
        }

        @Override
        public Uploader startPut(
                String url,
                Iterable<Header> headers
        ) {
            return createUploader(url);
        }

        private Uploader createUploader(String url) {

            requestedUrl = url;

            return new Uploader() {

                private final ByteArrayOutputStream body =
                        new ByteArrayOutputStream();

                @Override
                public OutputStream getBody() {
                    return body;
                }

                @Override
                public void close() {
                }

                @Override
                public void abort() {
                }

                @Override
                public Response finish() {

                    // Dropbox Download API uses HTTP POST
                    if (url.contains("/files/download")) {
                        return downloadResponse();
                    }

                    // Simulate successful Upload
                    uploadedBytes = body.toByteArray();

                    byte[] json = FILE_METADATA.getBytes(
                            StandardCharsets.UTF_8
                    );

                    return new Response(
                            200,
                            new ByteArrayInputStream(json),
                            Collections.emptyMap()
                    );
                }
            };
        }

        private Response downloadResponse() {

            Map<String, List<String>> headers =
                    Collections.singletonMap(
                            "Dropbox-API-Result",
                            Collections.singletonList(FILE_METADATA)
                    );

            return new Response(
                    200,
                    new ByteArrayInputStream(FILE_CONTENT),
                    headers
            );
        }
    }

    private DbxClientV2 createFakeClient(
            FakeHttpRequestor requestor
    ) {

        DbxRequestConfig config =
                DbxRequestConfig.newBuilder("transfer-unit-test")
                        .withHttpRequestor(requestor)
                        .build();

        return new DbxClientV2(
                config,
                UUID.randomUUID().toString()
        );
    }

    // TEST 1: Verify Upload bytes
    @Test
    public void upload_shouldSendCorrectBytes()
            throws Exception {

        FakeHttpRequestor fake =
                new FakeHttpRequestor();

        DbxClientV2 client = createFakeClient(fake);

        try (InputStream input =
                     new ByteArrayInputStream(FILE_CONTENT)) {

            FileMetadata result = client.files()
                    .uploadBuilder(FILE_PATH)
                    .withMode(WriteMode.ADD)
                    .withAutorename(true)
                    .uploadAndFinish(input);

            assertEquals(
                    "test.txt",
                    result.getName()
            );

            assertArrayEquals(
                    FILE_CONTENT,
                    fake.uploadedBytes
            );

            assertNotNull(fake.requestedUrl);

            assertTrue(
                    fake.requestedUrl.contains("/files/upload")
            );
        }
    }

    // TEST 2: Verify Download bytes
    @Test
    public void download_shouldPreserveContent()
            throws Exception {

        FakeHttpRequestor fake =
                new FakeHttpRequestor();

        DbxClientV2 client = createFakeClient(fake);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        FileMetadata result = client.files()
                .downloadBuilder(FILE_PATH)
                .download(output);

        assertEquals(
                "test.txt",
                result.getName()
        );

        assertArrayEquals(
                FILE_CONTENT,
                output.toByteArray()
        );

        assertNotNull(fake.requestedUrl);

        assertTrue(
                fake.requestedUrl.contains("/files/download")
        );
    }
}
