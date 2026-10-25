package vn.edu.usth.dropboxclient;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationErrorException;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import vn.edu.usth.dropboxclient.repository.DropboxRepository;
import vn.edu.usth.dropboxclient.ui.folder.DropboxFolderService;

import static org.junit.Assert.*;

public class DropboxSdkFolderMockTest {

    private static final class StubRequestor extends HttpRequestor {
        private final int status;
        private final String json;
        String requestedUrl;

        StubRequestor(int status, String json) {
            this.status = status;
            this.json = json;
        }

        @Override
        public Response doGet(String url, Iterable<Header> headers) {
            throw new AssertionError("Folder endpoints must use POST");
        }

        @Override
        public Uploader startPut(String url, Iterable<Header> headers) {
            throw new AssertionError("Folder endpoints must use POST");
        }

        @Override
        public Uploader startPost(String url, Iterable<Header> headers) {
            requestedUrl = url;
            return new Uploader() {
                private final ByteArrayOutputStream body = new ByteArrayOutputStream();

                @Override
                public OutputStream getBody() {
                    return body;
                }

                @Override
                public void close() {}

                @Override
                public void abort() {}

                @Override
                public Response finish() {
                    return new Response(
                            status,
                            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                            Collections.singletonMap("X-Dropbox-Request-Id", Collections.singletonList("folder-unit-test"))
                    );
                }
            };
        }
    }

    private DropboxRepository createRepository(StubRequestor requestor) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("folder-unit-test")
                .withHttpRequestor(requestor).build();
        return new DropboxRepository(new DbxClientV2(config, UUID.randomUUID().toString()));
    }

    @Test
    public void createFolderParsesResponseSuccessfully() throws Exception {
        String json = "{\"metadata\":{\"name\":\"TestFolder\",\"id\":\"id:folder_abc123\","
                + "\"path_lower\":\"/testfolder\",\"path_display\":\"/TestFolder\"}}";
        StubRequestor transport = new StubRequestor(200, json);
        DropboxRepository repository = createRepository(transport);

        FolderMetadata metadata = repository.createFolder("/TestFolder");

        assertNotNull(metadata);
        assertEquals("TestFolder", metadata.getName());
        assertEquals("/TestFolder", metadata.getPathDisplay());
        assertEquals("id:folder_abc123", metadata.getId());
        assertEquals("https://api.dropboxapi.com/2/files/create_folder_v2", transport.requestedUrl);
    }

    @Test
    public void renameFolderParsesResponseSuccessfully() throws Exception {
        String json = "{\"metadata\":{\".tag\":\"folder\",\"name\":\"RenamedFolder\",\"id\":\"id:folder_abc123\","
                + "\"path_lower\":\"/renamedfolder\",\"path_display\":\"/RenamedFolder\"}}";
        StubRequestor transport = new StubRequestor(200, json);
        DropboxRepository repository = createRepository(transport);

        Metadata metadata = repository.renameFolder("/OldFolder", "/RenamedFolder");

        assertNotNull(metadata);
        assertEquals("RenamedFolder", metadata.getName());
        assertEquals("/RenamedFolder", metadata.getPathDisplay());
        assertEquals("https://api.dropboxapi.com/2/files/move_v2", transport.requestedUrl);
    }

    @Test
    public void deleteFolderParsesResponseSuccessfully() throws Exception {
        String json = "{\"metadata\":{\".tag\":\"folder\",\"name\":\"TargetFolder\",\"id\":\"id:folder_abc123\","
                + "\"path_lower\":\"/targetfolder\",\"path_display\":\"/TargetFolder\"}}";
        StubRequestor transport = new StubRequestor(200, json);
        DropboxRepository repository = createRepository(transport);

        Metadata metadata = repository.deleteFolder("/TargetFolder");

        assertNotNull(metadata);
        assertEquals("TargetFolder", metadata.getName());
        assertEquals("/TargetFolder", metadata.getPathDisplay());
        assertEquals("https://api.dropboxapi.com/2/files/delete_v2", transport.requestedUrl);
    }

    @Test
    public void createFolderPropagatesConflictException() {
        String errorJson = "{\"error_summary\":\"path/conflict/folder/..\","
                + "\"error\":{\".tag\":\"path\",\"path\":{\".tag\":\"conflict\",\"conflict\":{\".tag\":\"folder\"}}}}";
        StubRequestor transport = new StubRequestor(409, errorJson);
        DropboxRepository repository = createRepository(transport);

        assertThrows(CreateFolderErrorException.class, () -> repository.createFolder("/ExistingFolder"));
    }

    @Test
    public void renameFolderPropagatesConflictException() {
        String errorJson = "{\"error_summary\":\"to/conflict/file/..\","
                + "\"error\":{\".tag\":\"to\",\"to\":{\".tag\":\"conflict\",\"conflict\":{\".tag\":\"file\"}}}}";
        StubRequestor transport = new StubRequestor(409, errorJson);
        DropboxRepository repository = createRepository(transport);

        assertThrows(RelocationErrorException.class, () -> repository.renameFolder("/FromPath", "/ToExistingPath"));
    }

    @Test
    public void unauthenticatedAccessThrowsInvalidAccessTokenException() {
        String errorJson = "{\"error_summary\":\"invalid_access_token/\",\"error\":{\".tag\":\"invalid_access_token\"}}";
        StubRequestor transport = new StubRequestor(401, errorJson);
        DropboxRepository repository = createRepository(transport);

        assertThrows(InvalidAccessTokenException.class, () -> repository.createFolder("/NewFolder"));
    }

    @Test
    public void folderServiceValidationRejectsInvalidArguments() {
        StubRequestor transport = new StubRequestor(200, "{}");
        DropboxRepository repository = createRepository(transport);
        DropboxFolderService service = new DropboxFolderService(repository);

        // Blank name
        assertThrows(IllegalArgumentException.class, () -> service.createFolder("/", "   "));

        // Illegal characters
        assertThrows(IllegalArgumentException.class, () -> service.createFolder("/", "bad:name"));

        // Rename root
        assertThrows(IllegalArgumentException.class, () -> service.renameFolder("/", "newName"));

        // Rename same name
        assertThrows(IllegalArgumentException.class, () -> service.renameFolder("/FolderA", "FolderA"));

        // Delete root
        assertThrows(IllegalArgumentException.class, () -> service.deleteFolder("/"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteFolder(""));
    }
}
