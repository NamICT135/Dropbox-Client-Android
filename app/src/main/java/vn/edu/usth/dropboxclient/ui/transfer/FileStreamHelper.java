
package vn.edu.usth.dropboxclient.ui.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileStreamHelper {

    private static final int BUFFER_SIZE = 8192;

    public static long copy(
            InputStream input,
            OutputStream output
    ) throws IOException {

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long totalBytes = 0;

        while ((bytesRead = input.read(buffer)) != -1) {
            if (bytesRead == 0) {
                continue;
            }

            output.write(buffer, 0, bytesRead);
            totalBytes += bytesRead;
        }

        output.flush();

        return totalBytes;
    }
}
