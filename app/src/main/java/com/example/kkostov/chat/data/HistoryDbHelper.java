package com.example.kkostov.chat.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.kkostov.chat.data.HistoryContract.HistoryEntry;

/**
 * Created by kkostov on 18-May-17.
 */
public class HistoryDbHelper extends SQLiteOpenHelper{

    // Important: If you change the database scheme, increment the version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "history.db";

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CRATE_TABLE = "CREATE TABLE " + HistoryEntry.TABLE_NAME  + " ( " +
                HistoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HistoryEntry.COLUMN_SENDER_ID + " INT8 NOT NULL, " +
                HistoryEntry.COLUMN_RECEIVER_ID + " INT8 NOT NULL, " +
                HistoryEntry.COLUMN_MESSAGE  + " TEXT NOT NULL, " +
                HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING + " INTEGER DEFAULT 0, " +
                HistoryEntry.COLUMN_IS_MESSAGE_RECEIVED + " INTEGER DEFAULT 0, " +
                HistoryEntry.COLUMN_IS_MESSAGE_READ_BY_ME + " INTEGER DEFAULT 0," +
                HistoryEntry.COLUMN_TIMESTAMP + " TEXT "+
                " );";

        db.execSQL(SQL_CRATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Deletes current table and creates a new one.
        db.execSQL("DROP TABLE IF EXISTS " + HistoryEntry.TABLE_NAME);
        onCreate(db);
    }

    public void deleteTable(){
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + HistoryEntry.TABLE_NAME);
    }
}
