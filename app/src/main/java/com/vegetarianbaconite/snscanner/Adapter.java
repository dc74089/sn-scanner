package com.vegetarianbaconite.snscanner;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

    List<String> serials = new ArrayList<>();

    public void insert(String str) {
        if (!serials.contains(str)) {
            serials.add(str);
            Log.d("Adapter", "Added " + str);

            notifyDataSetChanged();
        }
    }

    public boolean contains(String str) {
        return serials.contains(str);
    }

    public String getSerialsAsText() {
        StringBuilder builder = new StringBuilder();

        for (String str : serials) {
            builder.append(str);
            builder.append("\n");
        }

        return builder.toString();
    }

    public void clear() {
        serials.clear();
        notifyDataSetChanged();
    }

    public void undo() {
        if (serials.size() > 0) {
            serials.remove(serials.size() - 1);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);

        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(serials.get(position));
    }

    @Override
    public int getItemCount() {
        return serials.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView tv;

        public Holder(@NonNull View itemView) {
            super(itemView);
            this.tv = itemView.findViewById(android.R.id.text1);
        }

        public void bind(String s) {
            this.tv.setText(s);
        }
    }
}
