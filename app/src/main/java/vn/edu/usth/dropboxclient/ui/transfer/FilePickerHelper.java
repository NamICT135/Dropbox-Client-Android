
package vn.edu.usth.dropboxclient.ui.transfer;

import android.content.Intent;

public class FilePickerHelper {


    public static Intent createDownloadIntent(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        return intent;
    }


    public static Intent createUploadIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");


        return intent;
    }
}
