package com.notsarv.quickcopy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class OverlayHelper {

  private static WindowManager windowManager;
  private static View overlayView;

  public static void showOverlay(Context context, String text) {
    if (overlayView != null) {
      hideOverlay();
    }

    windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

    int layoutType;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
      layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    }

    WindowManager.LayoutParams params =
        new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);

    params.gravity = Gravity.CENTER;

    overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_layout, null);

    overlayView.setOnKeyListener(
        (v, keyCode, event) -> {
          if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            hideOverlay();
            return true;
          }
          return false;
        });

    overlayView.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
            hideOverlay();
            return true;
          }
          return false;
        });

    EditText editText = overlayView.findViewById(R.id.ocr_text_view);
    Button copyBtn = overlayView.findViewById(R.id.copy_btn);
    ImageButton closeBtn = overlayView.findViewById(R.id.close_btn);

    editText.setText(text);

    copyBtn.setOnClickListener(
        v -> {
          ClipboardManager clipboard =
              (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("OCR_TEXT", editText.getText());
          clipboard.setPrimaryClip(clip);
          Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
          hideOverlay();
        });

    closeBtn.setOnClickListener(v -> hideOverlay());

    windowManager.addView(overlayView, params);
  }

  public static void showLoadingOverlay(Context context) {
    if (overlayView != null) {
      hideOverlay();
    }

    windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

    int layoutType;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
      layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    }

    WindowManager.LayoutParams params =
        new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);

    params.gravity = Gravity.CENTER;

    overlayView = LayoutInflater.from(context).inflate(R.layout.loading_overlay_layout, null);

    overlayView.setOnKeyListener(
        (v, keyCode, event) -> {
          if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            hideOverlay();
            return true;
          }
          return false;
        });

    overlayView.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
            hideOverlay();
            return true;
          }
          return false;
        });

    windowManager.addView(overlayView, params);
  }

  public static void hideOverlay() {
    if (windowManager != null && overlayView != null) {
      windowManager.removeView(overlayView);
      overlayView = null;
    }
  }
}
