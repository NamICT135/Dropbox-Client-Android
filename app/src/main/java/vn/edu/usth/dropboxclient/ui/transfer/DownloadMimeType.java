package vn.edu.usth.dropboxclient.ui.transfer;

import java.util.Locale;

/** MIME supplied to ACTION_CREATE_DOCUMENT; unknown extensions stay binary. */
public final class DownloadMimeType {
    private DownloadMimeType() { }

    public static String forName(String name) {
        if (name == null) return "application/octet-stream";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "application/octet-stream";
        switch (name.substring(dot + 1).toLowerCase(Locale.ROOT)) {
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "txt": return "text/plain";
            case "csv": return "text/csv";
            case "zip": return "application/zip";
            default: return "application/octet-stream";
        }
    }
}
