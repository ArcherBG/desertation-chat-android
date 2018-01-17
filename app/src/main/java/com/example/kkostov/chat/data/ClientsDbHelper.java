package com.example.kkostov.chat.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.kkostov.chat.data.ClientsContract.ClientsEntry;

/**
 * Created by kkostov on 26-May-17.
 */

public class ClientsDbHelper extends SQLiteOpenHelper {

    // Important: If you change the database scheme, increment the version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "clients.db";

    public ClientsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

         String SQL_CREATE_TABLE = " CREATE TABLE " + ClientsEntry.TABLE_NAME + " ( " +
                 ClientsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                 ClientsEntry.COLUMN_CLIENT_ID + " INT8, " +
                 ClientsEntry.COLUMN_EMAIL + " TEXT " +
                 " );";

        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Deletes current table and creates a new one.
        db.execSQL("DROP TABLE IF EXISTS " + ClientsEntry.TABLE_NAME);
        onCreate(db);
    }

    public void deleteTable(){
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ClientsEntry.TABLE_NAME);
    }
}
