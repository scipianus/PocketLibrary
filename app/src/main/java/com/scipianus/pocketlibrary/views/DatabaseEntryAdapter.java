package com.scipianus.pocketlibrary.views;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.scipianus.pocketlibrary.R;
import com.scipianus.pocketlibrary.utils.DatabaseEntry;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Created by scipianus on 10-Apr-17.
 */

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class DatabaseEntryAdapter extends RecyclerView.Adapter<DatabaseEntryAdapter.MyViewHolder> {

    private List<DatabaseEntry> databaseEntries;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView entryIdTextView;
        public TextView entryScoreTextView;
        public Button selectEntryButton;

        public MyViewHolder(View view) {
            super(view);
            entryIdTextView = (TextView) view.findViewById(R.id.entryIdTextView);
            entryScoreTextView = (TextView) view.findViewById(R.id.entryScoreTextView);
            selectEntryButton = (Button) view.findViewById(R.id.selectEntryButton);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.database_entry_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final DatabaseEntry databaseEntry = databaseEntries.get(position);
        holder.entryIdTextView.setText(String.format("%d.", position + 1));
        holder.entryScoreTextView.setText(String.format("%.2f", databaseEntry.getScore()));
        holder.selectEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), databaseEntry.getId().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return databaseEntries.size();
    }
}