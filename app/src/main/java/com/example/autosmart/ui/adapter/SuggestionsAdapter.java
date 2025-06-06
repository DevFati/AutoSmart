package com.example.autosmart.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.model.MaintenanceSuggestion;

import java.util.ArrayList;
import java.util.List;

public class SuggestionsAdapter
        extends RecyclerView.Adapter<SuggestionsAdapter.VH> {

    private final List<MaintenanceSuggestion> items = new ArrayList<>();

    public void submitList(List<MaintenanceSuggestion> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View vew = LayoutInflater.from(p.getContext())
                .inflate(android.R.layout.simple_list_item_2, p, false);
        return new VH(vew);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MaintenanceSuggestion s = items.get(pos);
        h.tv1.setText(s.task);
        h.tv2.setText("Frecuencia: "+s.frequency);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        VH(View iv) {
            super(iv);
            tv1 = iv.findViewById(android.R.id.text1);
            tv2 = iv.findViewById(android.R.id.text2);
        }
    }
}
