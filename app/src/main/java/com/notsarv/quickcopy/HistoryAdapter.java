package com.notsarv.quickcopy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

  private final List<ScanResult> scans;
  private final OnItemClickListener listener;

  public interface OnItemClickListener {
    void onItemClick(ScanResult scan);
  }

  public HistoryAdapter(List<ScanResult> scans, OnItemClickListener listener) {
    this.scans = scans;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_history, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    ScanResult scan = scans.get(position);

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    holder.tvDate.setText(sdf.format(new Date(scan.timestamp)));

    holder.tvPreview.setText(scan.text.replace("\n", " ").trim());

    Glide.with(holder.itemView.getContext())
        .load(scan.imagePath)
        .transform(new RoundedCorners(16))
        .into(holder.imgScreenshot);

    holder.itemView.setOnClickListener(v -> listener.onItemClick(scan));
  }

  @Override
  public int getItemCount() {
    return scans.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    ImageView imgScreenshot;
    TextView tvDate, tvPreview;

    ViewHolder(View itemView) {
      super(itemView);
      imgScreenshot = itemView.findViewById(R.id.imgScreenshot);
      tvDate = itemView.findViewById(R.id.tvDate);
      tvPreview = itemView.findViewById(R.id.tvPreview);
    }
  }
}
