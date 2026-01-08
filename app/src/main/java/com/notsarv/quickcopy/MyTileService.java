package com.notsarv.quickcopy;

import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class MyTileService extends TileService {
  @Override
  public void onStartListening() {
    super.onStartListening();

    Tile tile = getQsTile();
    if (isServiceRunning()) {
      tile.setState(Tile.STATE_ACTIVE);
    } else {
      tile.setState(Tile.STATE_INACTIVE);
    }
    tile.updateTile();
  }

  @Override
  public void onClick() {
    if (!isServiceRunning()) {
      return;
    }

    Intent triggerIntent = new Intent("com.notsarv.quickcopy.ACTION_TRIGGER_OCR");
    triggerIntent.setPackage(getPackageName());
    sendBroadcast(triggerIntent);
  }

  private boolean isServiceRunning() {
    String prefString =
        Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    return prefString != null
        && prefString.contains(getPackageName() + "/" + MyAccessibilityService.class.getName());
  }
}
