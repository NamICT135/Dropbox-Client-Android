
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.IOException;

public class FileInfoHelper {

    public static String getFileName(Context context, Uri uri) throws IOException {
        String fileName = null;

        try (Cursor cursor = context.getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                );

                if (index >= 0 && !cursor.isNull(index)) {
                    fileName = cursor.getString(index);
                }
            }
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IOException("Selected document has no file name");
        }
        return fileName;
    }

    public static long getFileSize(Context context, Uri uri) {
        long fileSize = -1;

        try (Cursor cursor = context.getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.SIZE
                );

                if (index >= 0 && !cursor.isNull(index)) {
                    fileSize = cursor.getLong(index);
                }
            }
        }

        return fileSize;
    }
}
