
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public class FileReadHelper {

    public static InputStream openFile(
            Context context, Uri fileUri
    ) throws IOException {

        InputStream inputStream =
                context.getContentResolver()
                        .openInputStream(fileUri);

        if (inputStream == null) {
            throw new IOException("Cannot open selected file");
        }

        return inputStream;
    }
}
