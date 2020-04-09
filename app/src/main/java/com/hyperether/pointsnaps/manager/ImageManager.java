package com.hyperether.pointsnaps.manager;

import android.os.Environment;

import com.hyperether.pointsnaps.App;
import com.hyperether.pointsnaps.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class for image handling. Uses FileManager
 *
 * @author Slobodan Prijic
 * @version 1.0 - 07/21/2015
 */
public class ImageManager {

    /**
     * Creating file in external storage reserved for captured image
     *
     * @return image file
     * @throws IOException
     */
    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = timeStamp + ".jpg";
        String appDir = App.getInstance().getApplicationContext().getString(R.string.app_name);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/" + appDir);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = new File(storageDir.getAbsolutePath() + "/" + imageFileName);
        return image;
    }
}
