package vn.edu.usth.dropboxclient.ui.files;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import java.util.List;

/** Merge Dropbox pages by stable metadata ID while retaining list order. */
public final class MetadataPages {
    private MetadataPages() { }

    public static void merge(List<Metadata> entries, List<Metadata> page) {
        for (Metadata incoming : page) {
            String key = key(incoming);
            int existing = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (key.equals(key(entries.get(i)))) { existing = i; break; }
            }
            if (existing < 0) entries.add(incoming);
            else entries.set(existing, incoming);
        }
    }

    private static String key(Metadata item) {
        String id = item instanceof FileMetadata ? ((FileMetadata) item).getId()
                : item instanceof FolderMetadata ? ((FolderMetadata) item).getId() : null;
        return id != null ? "id:" + id : "path:" + item.getPathLower();
    }
}
