package com.notsarv.quickcopy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExportUtils {

  public static void exportData(Activity activity, List<ScanResult> scans) {
    AlertDialog progressDialog =
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Exporting Data")
            .setMessage("Packaging images and exporting data...")
            .setCancelable(false)
            .show();

    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              try {
                File exportDir = new File(activity.getCacheDir(), "exports");
                if (!exportDir.exists()) exportDir.mkdirs();

                File zipFile = new File(exportDir, "QuickCopy_Export.zip");
                if (zipFile.exists()) zipFile.delete();

                try (FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos)) {

                  JSONArray jsonArray = new JSONArray();
                  Set<String> addedFiles = new HashSet<>();

                  for (ScanResult scan : scans) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", scan.id);
                    obj.put("text", scan.text);
                    obj.put("timestamp", scan.timestamp);

                    if (scan.imagePath != null) {
                      File imgFile = new File(scan.imagePath);
                      if (imgFile.exists()) {
                        String fileName = imgFile.getName();
                        obj.put("image", fileName);

                        if (!addedFiles.contains(fileName)) {
                          addToZip(imgFile, zos);
                          addedFiles.add(fileName);
                        }
                      }
                    }
                    jsonArray.put(obj);
                  }
                  addTextToZip("data.json", jsonArray.toString(4), zos);
                }

                new Handler(Looper.getMainLooper())
                    .post(
                        () -> {
                          progressDialog.dismiss();
                          shareZip(activity, zipFile);
                        });

              } catch (Exception e) {
                new Handler(Looper.getMainLooper())
                    .post(
                        () -> {
                          progressDialog.dismiss();
                          Toast.makeText(
                                  activity, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                              .show();
                        });
                e.printStackTrace();
              }
            });
  }

  private static void addToZip(File file, ZipOutputStream zos) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
      ZipEntry zipEntry = new ZipEntry("images/" + file.getName());
      zos.putNextEntry(zipEntry);
      byte[] buffer = new byte[1024];
      int length;
      while ((length = fis.read(buffer)) >= 0) {
        zos.write(buffer, 0, length);
      }
      zos.closeEntry();
    }
  }

  private static void addTextToZip(String fileName, String content, ZipOutputStream zos)
      throws IOException {
    ZipEntry zipEntry = new ZipEntry(fileName);
    zos.putNextEntry(zipEntry);
    zos.write(content.getBytes());
    zos.closeEntry();
  }

  private static void shareZip(Context context, File zipFile) {
    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", zipFile);
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("application/zip");
    intent.putExtra(Intent.EXTRA_STREAM, uri);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    context.startActivity(Intent.createChooser(intent, "Save or Share Export"));
  }
}
