package com.lookie.camerafilterrealtime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FiltersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  public interface FiltersCallback {
    void onFilterClick(FilterItem filter, int index);
  }

  private FiltersCallback callback;

  int currentFilterIndex = 0;

  List<FilterItem> dataSet = new ArrayList<>();

  public FiltersAdapter(FiltersCallback callback) {
    this.callback = callback;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
    View itemView = LayoutInflater.from(viewGroup.getContext()).
        inflate(R.layout.list_filter_item, viewGroup, false);
    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int i) {
    final FilterItem filterItem = dataSet.get(i);
    MyViewHolder viewHolder = (MyViewHolder) holder;
    viewHolder.name.setText(filterItem.filterName);
    viewHolder.line.setVisibility(currentFilterIndex == i ? View.VISIBLE : View.INVISIBLE);
    viewHolder.main.setOnClickListener(v -> callback.onFilterClick(filterItem, i));
  }

  @Override
  public int getItemCount() {
    return dataSet.size();
  }

  public static class MyViewHolder extends RecyclerView.ViewHolder {

    TextView name;
    LinearLayout main;
    View line;

    MyViewHolder(View v) {
      super(v);
      this.main = v.findViewById(R.id.main);
      this.name = v.findViewById(R.id.name);
      this.line = v.findViewById(R.id.line);
    }
  }
}