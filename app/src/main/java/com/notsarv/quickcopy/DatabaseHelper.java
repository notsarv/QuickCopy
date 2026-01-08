package com.notsarv.quickcopy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

  private static final String DB_NAME = "QuickCopy.db";
  private static final int DB_VERSION = 1;
  private static final String TABLE_SCANS = "scans";

  public DatabaseHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE "
            + TABLE_SCANS
            + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "image_path TEXT, "
            + "ocr_text TEXT, "
            + "timestamp INTEGER)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCANS);
    onCreate(db);
  }

  public long addScan(String imagePath, String text) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("image_path", imagePath);
    values.put("ocr_text", text);
    values.put("timestamp", System.currentTimeMillis());
    return db.insert(TABLE_SCANS, null, values);
  }

  public List<ScanResult> searchScans(String query) {
    List<ScanResult> list = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();

    String sql =
        "SELECT * FROM " + TABLE_SCANS + " WHERE ocr_text LIKE ? " + " ORDER BY timestamp DESC";

    Cursor cursor = db.rawQuery(sql, new String[] {"%" + query + "%"});

    if (cursor.moveToFirst()) {
      do {
        list.add(
            new ScanResult(
                cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3)));
      } while (cursor.moveToNext());
    }
    cursor.close();
    return list;
  }

  public List<ScanResult> getAllScans() {
    List<ScanResult> list = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SCANS + " ORDER BY timestamp DESC", null);

    if (cursor.moveToFirst()) {
      do {
        list.add(
            new ScanResult(
                cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3)));
      } while (cursor.moveToNext());
    }
    cursor.close();
    return list;
  }

  public void deleteScan(long id) {
    getWritableDatabase().delete(TABLE_SCANS, "id=?", new String[] {String.valueOf(id)});
  }
}
