package com.scipianus.pocketlibrary.views;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.scipianus.pocketlibrary.BookInfoActivity;
import com.scipianus.pocketlibrary.R;
import com.scipianus.pocketlibrary.models.BookEntry;
import com.scipianus.pocketlibrary.models.DatabaseBookEntry;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Created by scipianus on 30-Apr-17.
 */

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class BookEntryAdapter extends RecyclerView.Adapter<BookEntryAdapter.MyViewHolder> {

    private static final String BOOK_EXTRA = "book";
    private List<BookEntry> bookEntries;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView entryIdTextView;
        public ProgressBar progressBar;
        public ImageView bookCoverImageView;
        public TextView entryTitleTextView;
        public Button selectEntryButton;

        public MyViewHolder(View view) {
            super(view);
            entryIdTextView = (TextView) view.findViewById(R.id.entryIdTextView);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            bookCoverImageView = (ImageView) view.findViewById(R.id.bookCoverImageView);
            entryTitleTextView = (TextView) view.findViewById(R.id.entryTitleTextView);
            selectEntryButton = (Button) view.findViewById(R.id.selectEntryButton);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.book_entry_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final BookEntry bookEntry = bookEntries.get(position);
        holder.entryIdTextView.setText(String.format("%d.", position + 1));
        holder.selectEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), BookInfoActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(BOOK_EXTRA, bookEntry);
                intent.putExtras(bundle);
                v.getContext().startActivity(intent);
            }
        });

        if (bookEntry.getCoverImage() != null) {
            holder.progressBar.setVisibility(View.GONE);
            holder.entryTitleTextView.setText(bookEntry.getTitle());
            holder.bookCoverImageView.setImageBitmap(bookEntry.getCoverImage());
        } else {
            holder.selectEntryButton.setEnabled(false);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (bookEntry instanceof DatabaseBookEntry) {
                        ((DatabaseBookEntry) bookEntry).fetchJSONData();
                    }
                    bookEntry.fetchCoverImage();

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            holder.progressBar.setVisibility(View.GONE);
                            holder.entryTitleTextView.setText(bookEntry.getTitle());
                            holder.bookCoverImageView.setImageBitmap(bookEntry.getCoverImage());
                            holder.selectEntryButton.setEnabled(true);
                        }
                    });
                }
            });
            thread.start();
        }
    }

    @Override
    public int getItemCount() {
        return bookEntries.size();
    }
}
