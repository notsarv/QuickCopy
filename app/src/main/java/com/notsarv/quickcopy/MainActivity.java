package com.notsarv.quickcopy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final int NOTIFICATION_REQUEST_CODE = 1001;

  private MaterialCardView cardPermissions;
  private MaterialButton btnGrantOverlay, btnGrantAccessibility;
  private RecyclerView recyclerView;
  private DatabaseHelper databaseHelper;
  private SwipeRefreshLayout swipeRefreshLayout;

  private TextInputLayout searchLayout;
  private TextInputEditText etSearch;
  private ImageButton searchBtn;

  private ImageButton exportBtn;

  private final ActivityResultLauncher<Intent> permissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(), r -> checkPermissions());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    databaseHelper = new DatabaseHelper(this);

    cardPermissions = findViewById(R.id.cardPermissions);
    btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
    btnGrantAccessibility = findViewById(R.id.btnGrantAccessibility);
    recyclerView = findViewById(R.id.recyclerHistory);

    searchLayout = findViewById(R.id.searchLayout);
    etSearch = findViewById(R.id.etSearch);
    searchBtn = findViewById(R.id.searchBtn);

    exportBtn = findViewById(R.id.exportBtn);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    swipeRefreshLayout = findViewById(R.id.swipeRefresh);
    swipeRefreshLayout.setOnRefreshListener(
        () -> {
          loadHistory();
          swipeRefreshLayout.setRefreshing(false);
        });

    exportBtn.setOnClickListener(
        v -> {
          List<ScanResult> allScans = databaseHelper.getAllScans();
          if (allScans.isEmpty()) {
            Toast.makeText(MainActivity.this, "No Data to export!", Toast.LENGTH_SHORT).show();
            return;
          }
          ExportUtils.exportData(MainActivity.this, allScans);
        });

    searchBtn.setOnClickListener(
        v -> {
          if (searchLayout.getVisibility() == View.VISIBLE) {
            loadHistory();
            searchLayout.setVisibility(View.GONE);
            searchBtn.setImageResource(R.drawable.ic_search);
          } else {
            searchLayout.setVisibility(View.VISIBLE);
            searchBtn.setImageResource(R.drawable.ic_close);
          }
        });

    etSearch.addTextChangedListener(
        new android.text.TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            performSearch(s.toString().trim());
          }

          @Override
          public void afterTextChanged(android.text.Editable s) {}
        });

    btnGrantOverlay.setOnClickListener(
        v -> {
          Intent intent =
              new Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  Uri.parse("package:" + getPackageName()));
          permissionLauncher.launch(intent);
        });

    btnGrantAccessibility.setOnClickListener(
        v -> {
          Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
          permissionLauncher.launch(intent);
        });

    checkBatteryOptimization();
	  checkNotificationPermission();
  }

  @Override
  protected void onResume() {
    super.onResume();
    checkPermissions();
    loadHistory();
  }

  private void checkPermissions() {
    boolean overlayOk =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    boolean accessibilityOk = isAccessibilityEnabled();

    if (overlayOk && accessibilityOk) {
      cardPermissions.setVisibility(View.GONE);
    } else {
      cardPermissions.setVisibility(View.VISIBLE);

      if (overlayOk) {
        btnGrantOverlay.setText("Overlay Granted");
        btnGrantOverlay.setIconResource(R.drawable.ic_check);
        btnGrantOverlay.setEnabled(false);
      }
      if (accessibilityOk) {
        btnGrantAccessibility.setText("Service Active");
        btnGrantAccessibility.setEnabled(false);
      }
    }
  }

  private boolean isAccessibilityEnabled() {
    String prefString =
        Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    return prefString != null
        && prefString.contains(getPackageName() + "/" + MyAccessibilityService.class.getName());
  }

  private void loadHistory() {
    List<ScanResult> list = databaseHelper.getAllScans();
    HistoryAdapter adapter = new HistoryAdapter(list, this::showDetailDialog);
    recyclerView.setAdapter(adapter);
  }

  private void showDetailDialog(ScanResult scan) {
    Intent intent = new Intent(this, DetailActivity.class);
    intent.putExtra(DetailActivity.EXTRA_SCAN_ID, scan.id);
    intent.putExtra(DetailActivity.EXTRA_SCAN_TEXT, scan.text);
    intent.putExtra(DetailActivity.EXTRA_SCAN_IMAGE, scan.imagePath);
    intent.putExtra(DetailActivity.EXTRA_SCAN_DATE, scan.timestamp);
    startActivity(intent);
  }

  private void checkBatteryOptimization() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Intent intent = new Intent();
      String packageName = getPackageName();
      PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
      if (!pm.isIgnoringBatteryOptimizations(packageName)) {
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
      }
    }
  }

  private void performSearch(String query) {
    if (query.isEmpty()) {
      loadHistory();
      return;
    }

    List<ScanResult> filteredList = databaseHelper.searchScans(query);
    HistoryAdapter adapter = new HistoryAdapter(filteredList, this::showDetailDialog);
    recyclerView.setAdapter(adapter);
  }

  private void checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
              MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            MainActivity.this,
            new String[] {Manifest.permission.POST_NOTIFICATIONS},
            NOTIFICATION_REQUEST_CODE);
      }
    }
  }
}
