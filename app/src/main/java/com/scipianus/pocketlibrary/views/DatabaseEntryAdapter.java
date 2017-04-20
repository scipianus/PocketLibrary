package com.scipianus.pocketlibrary.views;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.scipianus.pocketlibrary.R;
import com.scipianus.pocketlibrary.utils.DatabaseEntry;
import com.scipianus.pocketlibrary.utils.HTTPUtils;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Created by scipianus on 10-Apr-17.
 */

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class DatabaseEntryAdapter extends RecyclerView.Adapter<DatabaseEntryAdapter.MyViewHolder> {

    private static final String COVER_API_PATH = "http://covers.openlibrary.org/b/olid/";
    private List<DatabaseEntry> databaseEntries;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView entryIdTextView;
        public ImageView bookCoverImageView;
        public TextView entryScoreTextView;
        public Button selectEntryButton;

        public MyViewHolder(View view) {
            super(view);
            entryIdTextView = (TextView) view.findViewById(R.id.entryIdTextView);
            bookCoverImageView = (ImageView) view.findViewById(R.id.bookCoverImageView);
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
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final DatabaseEntry databaseEntry = databaseEntries.get(position);
        holder.entryIdTextView.setText(String.format("%d.", position + 1));
        holder.entryScoreTextView.setText(String.format("%.2f", databaseEntry.getScore()));
        holder.selectEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), databaseEntry.getId().toString(), Toast.LENGTH_SHORT).show();
            }
        });

        if (databaseEntry.getCoverImage() != null) {
            holder.bookCoverImageView.setImageBitmap(databaseEntry.getCoverImage());
        } else {
            Thread thread = new Thread(new Runnable() {
                String imageUrl = String.format("%s%s-M.jpg", COVER_API_PATH, databaseEntry.getId());

                @Override
                public void run() {
                    final Bitmap bookCover = HTTPUtils.getImage(imageUrl);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            databaseEntry.setCoverImage(bookCover);
                            holder.bookCoverImageView.setImageBitmap(bookCover);
                        }
                    });
                }
            });
            thread.start();
        }
    }

    @Override
    public int getItemCount() {
        return databaseEntries.size();
    }
}