package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private final List<ListItem> items;

    public ListAdapter(List<ListItem> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemTextView;
        public ImageView itemImageView;

        public ViewHolder(View view) {
            super(view);
            itemTextView = view.findViewById(com.example.myapplication.R.id.itemTextView);
            itemImageView = view.findViewById(com.example.myapplication.R.id.itemImageView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(com.example.myapplication.R.layout.item_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListItem item = items.get(position);
        holder.itemTextView.setText(item.getText());
        holder.itemImageView.setImageResource(item.getImageResId());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}