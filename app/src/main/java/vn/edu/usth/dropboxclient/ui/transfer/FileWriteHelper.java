
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

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }
    }
}
