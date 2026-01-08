package com.notsarv.quickcopy;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MyAccessibilityService extends AccessibilityService {

  private BroadcastReceiver triggerReceiver;
  private final Executor executor = Executors.newSingleThreadExecutor();
  private DatabaseHelper databaseHelper;

  private static final String CHANNEL_ID = "ocr_service_channel";
  private static final int NOTIFICATION_ID = 100271;

  @Override
  public void onCreate() {
    super.onCreate();
    databaseHelper = new DatabaseHelper(this);

    triggerReceiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if ("com.notsarv.quickcopy.ACTION_TRIGGER_OCR".equals(intent.getAction())) {
              performOcrAction();
            }
          }
        };

    IntentFilter filter = new IntentFilter("com.notsarv.quickcopy.ACTION_TRIGGER_OCR");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(triggerReceiver, filter, Context.RECEIVER_EXPORTED);
    } else {
      registerReceiver(triggerReceiver, filter);
    }
  }

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    startForegroundServiceWithNotification();
  }

  private void startForegroundServiceWithNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(
              CHANNEL_ID, "QuickCopy Active", NotificationManager.IMPORTANCE_MAX);
      NotificationManager manager = getSystemService(NotificationManager.class);
      if (manager != null) manager.createNotificationChannel(channel);
    }

    Notification notification =
        new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuickCopy is Running")
            .setContentText("Service is ready")
            .setSmallIcon(R.drawable.ic_ocr)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build();

    startForeground(NOTIFICATION_ID, notification);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (triggerReceiver != null) unregisterReceiver(triggerReceiver);
  }

  private void performOcrAction() {
    performGlobalAction(GLOBAL_ACTION_BACK);
    new Handler(Looper.getMainLooper()).postDelayed(this::takeScreenshotAndProcess, 500);
  }

  private void takeScreenshotAndProcess() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      takeScreenshot(
          Display.DEFAULT_DISPLAY,
          executor,
          new TakeScreenshotCallback() {
            @Override
            public void onSuccess(@NonNull ScreenshotResult screenshotResult) {
              new Handler(Looper.getMainLooper())
                  .post(() -> OverlayHelper.showLoadingOverlay(MyAccessibilityService.this));

              Bitmap bitmap =
                  Bitmap.wrapHardwareBuffer(
                      screenshotResult.getHardwareBuffer(), screenshotResult.getColorSpace());

              if (bitmap != null) {
                processBitmap(bitmap);
                screenshotResult.getHardwareBuffer().close();
              }
            }

            @Override
            public void onFailure(int i) {
              showToast("Screenshot failed");
              OverlayHelper.hideOverlay();
            }
          });
    } else {
      showToast("Android 11+ required");
    }
  }

  private void processBitmap(Bitmap originalBitmap) {
    try {
      int statusBarHeight = getDimen("status_bar_height");
      int navBarHeight = getDimen("navigation_bar_height");

      int height = originalBitmap.getHeight() - statusBarHeight - navBarHeight;
      if (height <= 0) height = originalBitmap.getHeight(); // Safety check

      Bitmap croppedBitmap =
          Bitmap.createBitmap(
              originalBitmap, 0, statusBarHeight, originalBitmap.getWidth(), height);

      String savedPath = StorageUtils.saveBitmapToInternalStorage(this, croppedBitmap);

      runOCR(croppedBitmap, savedPath);

    } catch (Exception e) {
      e.printStackTrace();
      runOCR(originalBitmap, null);
    }
  }

  private void runOCR(Bitmap bitmap, String imagePath) {
    InputImage image = InputImage.fromBitmap(bitmap, 0);
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    recognizer
        .process(image)
        .addOnSuccessListener(
            visionText -> {
              String resultText =
                  visionText.getText().isEmpty() ? "No text found." : visionText.getText();

              if (imagePath != null) {
                executor.execute(() -> databaseHelper.addScan(imagePath, resultText));
              }

              new Handler(Looper.getMainLooper())
                  .post(
                      () -> {
                        OverlayHelper.hideOverlay();
                        OverlayHelper.showOverlay(MyAccessibilityService.this, resultText);
                      });
            })
        .addOnFailureListener(
            e -> {
              showToast("OCR Failed");
              OverlayHelper.hideOverlay();
            });
  }

  private int getDimen(String name) {
    int resourceId = getResources().getIdentifier(name, "dimen", "android");
    return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
  }

  private void showToast(String msg) {
    new Handler(Looper.getMainLooper())
        .post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {}

  @Override
  public void onInterrupt() {}
}
