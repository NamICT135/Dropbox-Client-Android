
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileWriteHelper {

    public static void writeFile(
            Context context,
            Uri destinationUri,
            InputStream inputStream
    ) throws IOException {

        try (OutputStream outputStream =
                     context.getContentResolver()
                             .openOutputStream(destinationUri, "w")) {

            if (outputStream == null) {
                throw new IOException(
                        "Cannot open destination file"
                );
            }

            FileStreamHelper.copy(inputStream, outputStream);
        }
    }
}
