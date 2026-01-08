package com.notsarv.quickcopy;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class DetailActivity extends AppCompatActivity {

  public static final String EXTRA_SCAN_ID = "extra_scan_id";
  public static final String EXTRA_SCAN_TEXT = "extra_scan_text";
  public static final String EXTRA_SCAN_IMAGE = "extra_scan_image";
  public static final String EXTRA_SCAN_DATE = "extra_scan_date";

  private DatabaseHelper databaseHelper;
  private long scanId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detail);

    databaseHelper = new DatabaseHelper(this);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle("");
    }

    scanId = getIntent().getLongExtra(EXTRA_SCAN_ID, -1);
    String text = getIntent().getStringExtra(EXTRA_SCAN_TEXT);
    String imagePath = getIntent().getStringExtra(EXTRA_SCAN_IMAGE);

    ImageView imageView = findViewById(R.id.detail_image);
    TextView textView = findViewById(R.id.detail_text);
    ExtendedFloatingActionButton fabCopy = findViewById(R.id.fab_copy);

    if (imagePath != null) {
      Glide.with(this).load(imagePath).into(imageView);
      imageView.setOnClickListener(v -> showFullImage(imagePath));
    }

    textView.setText(text);

    fabCopy.setOnClickListener(
        v -> {
          copyToClipboard(textView.getText().toString());
        });
  }

  private void showFullImage(String path) {
    Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.dialog_full_image);

    ImageView fullImg = dialog.findViewById(R.id.full_image_view);
    Glide.with(this).load(path).into(fullImg);

    dialog.show();
  }

  private void copyToClipboard(String text) {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("OCR Result", text);
    clipboard.setPrimaryClip(clip);
    Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    } else if (item.getItemId() == R.id.action_delete) {
      deleteAndExit();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void deleteAndExit() {
    if (scanId != -1) {
      databaseHelper.deleteScan(scanId);
      Toast.makeText(this, "Scan deleted", Toast.LENGTH_SHORT).show();
      finish();
    }
  }
}
