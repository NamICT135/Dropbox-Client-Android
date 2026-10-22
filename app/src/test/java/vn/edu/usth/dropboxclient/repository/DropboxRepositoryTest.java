package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises SDK request/response behavior with an in-memory HTTP response, never a real token. */
public class DropboxRepositoryTest {
    private static final class StubRequestor extends HttpRequestor {
        private final int status;
        private final String json;
        String requestedUrl;
        StubRequestor(int status, String json) { this.status = status; this.json = json; }
        @Override public Response doGet(String url, Iterable<Header> headers) {
            throw new AssertionError("Account lookup must be POST");
        }
        @Override public Uploader startPut(String url, Iterable<Header> headers) {
            throw new AssertionError("Account lookup must be POST");
        }
        @Override public Uploader startPost(String url, Iterable<Header> headers) {
            requestedUrl = url;
            return new Uploader() {
                private final ByteArrayOutputStream body = new ByteArrayOutputStream();
                @Override public OutputStream getBody() { return body; }
                @Override public void close() { }
                @Override public void abort() { }
                @Override public Response finish() {
                    return new Response(status, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                            Collections.singletonMap("X-Dropbox-Request-Id", Collections.singletonList("unit-test")));
                }
            };
        }
    }
    private DropboxRepository repository(StubRequestor requestor) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("academic-test")
                .withHttpRequestor(requestor).build();
        return new DropboxRepository(new DbxClientV2(config, UUID.randomUUID().toString()));
    }
    @Test public void parsesAccountFromSdkApiResponse() throws Exception {
        String json = "{\"account_id\":\"dbid:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"name\":{\"given_name\":\"Project\",\"surname\":\"Demo\","
                + "\"familiar_name\":\"Project\",\"display_name\":\"Project Demo\",\"abbreviated_name\":\"PD\"},"
                + "\"email\":\"project@example.test\",\"email_verified\":true,\"disabled\":false,"
                + "\"locale\":\"en\",\"referral_link\":\"https://www.dropbox.com/referrals/test\","
                + "\"is_paired\":false,\"account_type\":{\".tag\":\"basic\"},"
                + "\"root_info\":{\".tag\":\"user\",\"root_namespace_id\":\"1\",\"home_namespace_id\":\"1\"}}";
        StubRequestor transport = new StubRequestor(200, json);
        FullAccount account = repository(transport).getCurrentAccount();
        assertEquals("Project Demo", account.getName().getDisplayName());
        assertEquals("project@example.test", account.getEmail());
        assertEquals("https://api.dropboxapi.com/2/users/get_current_account", transport.requestedUrl);
    }
    @Test public void revokedAccessPropagatesToCaller() {
        StubRequestor transport = new StubRequestor(401,
                "{\"error_summary\":\"invalid_access_token/\",\"error\":{\".tag\":\"invalid_access_token\"}}");
        assertThrows(InvalidAccessTokenException.class, () -> repository(transport).getCurrentAccount());
    }
}
