package com.example.kkostov.chat.adapters;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kkostov.chat.R;
import com.example.kkostov.chat.data.HistoryContract.HistoryEntry;

/**
 * Created by kkostov on 18-May-17.
 */
public class HistoryCursorAdapter extends CursorAdapter {

    private static final String TAG = HistoryCursorAdapter.class.getSimpleName();

    public HistoryCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_chat_box, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Get data to show on UI
        String time = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.COLUMN_TIMESTAMP));
        String message = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.COLUMN_MESSAGE));
        // Meta data below
        int isMessageOutgoing = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING));
        final int isMessageReceivedByServer = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryEntry.COLUMN_IS_MESSAGE_RECEIVED));

        // Set data
        viewHolder.tvTime.setText(time);

        // Am I the creator of the message
        if (isMessageOutgoing > 0) {
            viewHolder.tvMyMessage.setText(message);

            // Format the UI
            viewHolder.tvTime.setGravity(Gravity.END);
            viewHolder.tvTime.setGravity(Gravity.RIGHT);
            viewHolder.ivReceiverAvatar.setVisibility(View.GONE);
            viewHolder.tvReceiversMessage.setVisibility(View.GONE);
            viewHolder.tvMyMessage.setVisibility(View.VISIBLE);
            if (isMessageReceivedByServer == 1) {
               // viewHolder.tvMyMessage.setBackgroundColor(context.getResources().getColor(R.color.send));
                viewHolder.tvMyMessage.setBackground(context.getResources().getDrawable(R.drawable.rounded_corners_without_right_send_color));
            } else {
               // viewHolder.tvMyMessage.setBackgroundColor(context.getResources().getColor(R.color.unsend));
                viewHolder.tvMyMessage.setBackground(context.getResources().getDrawable(R.drawable.rounded_corners_without_right_unsend_color));
            }
        } else {
            viewHolder.tvReceiversMessage.setText(message);

            // Format the UI
            viewHolder.tvTime.setGravity(Gravity.NO_GRAVITY);
            viewHolder.ivReceiverAvatar.setVisibility(View.VISIBLE);
            viewHolder.tvMyMessage.setVisibility(View.GONE);
            viewHolder.tvReceiversMessage.setVisibility(View.VISIBLE);
            viewHolder.tvReceiversMessage.setBackground(context.getResources().getDrawable(R.drawable.rounded_corners_without_left_send_color));
           // viewHolder.tvReceiversMessage.setBackgroundColor(context.getResources().getColor(R.color.send));
        }
    }


    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final TextView tvTime;
        public final ImageView ivReceiverAvatar;
        public final TextView tvReceiversMessage;
        public final TextView tvMyMessage;


        public ViewHolder(View view) {
            tvTime = (TextView) view.findViewById(R.id.tvTime);
            ivReceiverAvatar = (ImageView) view.findViewById(R.id.receiverIcon);
            tvReceiversMessage = (TextView) view.findViewById(R.id.tvReceiversMessage);
            tvMyMessage = (TextView) view.findViewById(R.id.tvMyMessage);
        }
    }
}
