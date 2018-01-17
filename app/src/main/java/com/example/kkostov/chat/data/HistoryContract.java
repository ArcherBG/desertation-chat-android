package com.example.kkostov.chat.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the questions database.
 */
public final class HistoryContract {

    public static final String CONTENT_AUTHORITY = "com.example.kkostov.chat";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_HISTORY = "history";


    // In order not to instantiate this class.
    private HistoryContract() {
    }

    /* Inner class where we define the columns and their index numbers */
    public static class HistoryEntry implements BaseColumns {

        // Content URI represents the base location for the table
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_HISTORY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_HISTORY;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_HISTORY;

        public static final String TABLE_NAME = "history";
        public static final String COLUMN_SENDER_ID = "sender_id";
        public static final String COLUMN_RECEIVER_ID = "receiver_id";
        public static final String COLUMN_MESSAGE = "message";
        public static final String COLUMN_IS_MESSAGE_OUTGOING = "is_msg_outgoing";
        public static final String COLUMN_IS_MESSAGE_RECEIVED = "is_received";  // received by server
        public static final String COLUMN_IS_MESSAGE_READ_BY_ME = "is_read" ; // is read by client ( receiver or sender)
        public static final String COLUMN_TIMESTAMP = "timestamp";


        // Important: If Columns change this must change too!
        // Indexes for Question Table
//        public static final int COl_ID = 0;
//        public static final int COL_CLIENT_ID_OF_CREATOR = 1;
//        public static final int COL_MESSAGE = 2;
//        public static final int COL_IS_MESSAGE_RECEIVED = 3;


        public static Uri buildHistoryUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
