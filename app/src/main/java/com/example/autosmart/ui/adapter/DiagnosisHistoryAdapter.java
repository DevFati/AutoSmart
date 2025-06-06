package com.example.autosmart.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.R;
import com.example.autosmart.ui.fragment.DiagnosisFragment;

import java.util.List;

public class DiagnosisHistoryAdapter extends RecyclerView.Adapter<DiagnosisHistoryAdapter.HistoryViewHolder> {

    private final List<DiagnosisFragment.DiagnosisResult> history;

    public DiagnosisHistoryAdapter(List<DiagnosisFragment.DiagnosisResult> history) {
        this.history = history;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diagnosis_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        DiagnosisFragment.DiagnosisResult item = history.get(position);
        holder.tvCode.setText("Código: " + item.getCode());
        holder.tvDefinition.setText(item.getDefinition());
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvDefinition;
        HistoryViewHolder(View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvHistoryCode);
            tvDefinition = itemView.findViewById(R.id.tvHistoryDefinition);
        }
    }
} 