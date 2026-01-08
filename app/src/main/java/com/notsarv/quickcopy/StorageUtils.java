package com.notsarv.quickcopy;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StorageUtils {

  public static String saveBitmapToInternalStorage(Context context, Bitmap bitmap) {
    String timeStamp =
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    String imageFileName = "OCR_" + timeStamp + ".jpg";

    File storageDir = new File(context.getFilesDir(), "scans");
    if (!storageDir.exists()) {
      storageDir.mkdirs();
    }

    File imageFile = new File(storageDir, imageFileName);

    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
      return imageFile.getAbsolutePath();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
