package vn.edu.usth.dropboxclient.repository;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.SearchV2Result;
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
        assertTrue(account.getEmailVerified());
        assertEquals(com.dropbox.core.v2.userscommon.AccountType.BASIC, account.getAccountType());
        assertEquals("dbid:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", account.getAccountId());
        assertEquals("https://api.dropboxapi.com/2/users/get_current_account", transport.requestedUrl);
    }
    @Test public void revokedAccessPropagatesToCaller() {
        StubRequestor transport = new StubRequestor(401,
                "{\"error_summary\":\"invalid_access_token/\",\"error\":{\".tag\":\"invalid_access_token\"}}");
        assertThrows(InvalidAccessTokenException.class, () -> repository(transport).getCurrentAccount());
    }

    @Test public void listsRootAndUsesCursorForNextPage() throws Exception {
        StubRequestor first = new StubRequestor(200,
                "{\"entries\":[{\".tag\":\"folder\",\"name\":\"Lớp\",\"path_lower\":\"/lớp\","
                        + "\"path_display\":\"/Lớp\",\"id\":\"id:folder1\"}],"
                        + "\"cursor\":\"next-page\",\"has_more\":true}");
        ListFolderResult page = repository(first).listFolder("");
        assertEquals("https://api.dropboxapi.com/2/files/list_folder", first.requestedUrl);
        assertTrue(page.getHasMore());
        assertEquals("next-page", page.getCursor());
        assertTrue(page.getEntries().get(0) instanceof FolderMetadata);
        assertEquals("Lớp", page.getEntries().get(0).getName());

        StubRequestor second = new StubRequestor(200,
                "{\"entries\":[],\"cursor\":\"done\",\"has_more\":false}");
        ListFolderResult finalPage = repository(second).continueListFolder(page.getCursor());
        assertEquals("https://api.dropboxapi.com/2/files/list_folder/continue", second.requestedUrl);
        assertFalse(finalPage.getHasMore());
    }

    @Test public void searchesDropboxAndContinuesResults() throws Exception {
        StubRequestor first = new StubRequestor(200,
                "{\"matches\":[{\"metadata\":{\".tag\":\"metadata\",\"metadata\":{\".tag\":\"folder\","
                        + "\"name\":\"Reports\",\"path_lower\":\"/reports\",\"path_display\":\"/Reports\","
                        + "\"id\":\"id:reports\"}}}],\"has_more\":true,\"cursor\":\"search-next\"}");
        SearchV2Result page = repository(first).search("Reports");
        assertEquals("https://api.dropboxapi.com/2/files/search_v2", first.requestedUrl);
        assertEquals("Reports", page.getMatches().get(0).getMetadata().getMetadataValue().getName());
        assertTrue(page.getHasMore());

        StubRequestor second = new StubRequestor(200, "{\"matches\":[],\"has_more\":false}");
        SearchV2Result last = repository(second).continueSearch(page.getCursor());
        assertEquals("https://api.dropboxapi.com/2/files/search/continue_v2", second.requestedUrl);
        assertFalse(last.getHasMore());
    }

    @Test public void fileRenameAndDeleteUseDropboxMetadataAfterConfirmation() throws Exception {
        String moved = "{\"metadata\":{\".tag\":\"file\",\"name\":\"Báo cáo.pdf\","
                + "\"id\":\"id:file1\",\"client_modified\":\"2026-10-22T00:00:00Z\","
                + "\"server_modified\":\"2026-10-22T00:00:00Z\",\"rev\":\"123456789\",\"size\":3}}";
        StubRequestor moveTransport = new StubRequestor(200, moved);
        assertEquals("Báo cáo.pdf", repository(moveTransport)
                .renameFile("/old.pdf", "/Báo cáo.pdf").getName());
        assertTrue(moveTransport.requestedUrl.endsWith("/files/move_v2"));

        String deleted = "{\"metadata\":{\".tag\":\"file\",\"name\":\"Báo cáo.pdf\","
                + "\"id\":\"id:file1\",\"client_modified\":\"2026-10-22T00:00:00Z\","
                + "\"server_modified\":\"2026-10-22T00:00:00Z\",\"rev\":\"123456789\",\"size\":3}}";
        StubRequestor deleteTransport = new StubRequestor(200, deleted);
        assertEquals("Báo cáo.pdf", repository(deleteTransport)
                .deleteFile("/Báo cáo.pdf").getName());
        assertTrue(deleteTransport.requestedUrl.endsWith("/files/delete_v2"));
    }
}
