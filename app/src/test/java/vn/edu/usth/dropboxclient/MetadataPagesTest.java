package vn.edu.usth.dropboxclient;

import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import vn.edu.usth.dropboxclient.ui.files.MetadataPages;
import static org.junit.Assert.*;

public class MetadataPagesTest {
    @Test public void laterPageWithSameIdReplacesMetadataWithoutDuplicate() {
        ArrayList<Metadata> items = new ArrayList<>();
        MetadataPages.merge(items, Arrays.asList(
                new FolderMetadata("Tài liệu", "id:one"),
                new FolderMetadata("Ảnh", "id:two")));
        MetadataPages.merge(items, Arrays.asList(
                new FolderMetadata("Tài liệu mới", "id:one"),
                new FolderMetadata("Bài tập", "id:three")));
        assertEquals(3, items.size());
        assertEquals("Tài liệu mới", items.get(0).getName());
        assertEquals("Ảnh", items.get(1).getName());
        assertEquals("Bài tập", items.get(2).getName());
    }
}
