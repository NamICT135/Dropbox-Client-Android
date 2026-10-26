package vn.edu.usth.dropboxclient.ui.transfer;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewTypeTest {
    @Test public void acceptsSupportedImageAndPdfExtensionsCaseInsensitively() {
        assertEquals("application/pdf", TransferViewModel.previewMimeType("/Docs/Report.PDF"));
        assertEquals("image/jpeg", TransferViewModel.previewMimeType("/photo.JPEG"));
        assertEquals("image/png", TransferViewModel.previewMimeType("/image.png"));
        assertNull(TransferViewModel.previewMimeType("/archive.zip"));
        assertNull(TransferViewModel.previewMimeType(null));
    }
}
