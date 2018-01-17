package com.example.kkostov.chat.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.kkostov.chat.data.ClientsContract.ClientsEntry;

/**
 * Created by kkostov on 26-May-17.
 */

public class ClientsDataProvider {

    private static final String TAG = ClientsDataProvider.class.getSimpleName();
    private ClientsDbHelper clientsDbHelper;

    public ClientsDataProvider(Context context) {
        //  Note: Because they can be long-running, be sure that you call getWritableDatabase() or getReadableDatabase() in a background thread, such as with AsyncTask or IntentService.
        clientsDbHelper = new ClientsDbHelper(context);
    }

    public void insert(ContentValues values) {
        final SQLiteDatabase db = clientsDbHelper.getWritableDatabase();
        // If exists update not insert again
        long foundRow = findClientId(values.getAsString(ClientsEntry.COLUMN_EMAIL));
        if (foundRow <= 0) {
            db.insert(ClientsEntry.TABLE_NAME, null, values);
        }else {
            Log.d(TAG, "will update row in db: " +foundRow);
            //  Update
            db.update(ClientsEntry.TABLE_NAME, values, ClientsEntry.COLUMN_CLIENT_ID +  " = ?", new String[]{String.valueOf(foundRow)});
        }
    }

    public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = clientsDbHelper.getWritableDatabase();
        return db.query(
                ClientsEntry.TABLE_NAME,                  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
    }

    public long findClientId(String email) {
        long id = -1; // not found

        String selection = ClientsEntry.COLUMN_EMAIL + " = ? ";
        String selectionArgs[] = {email};

        SQLiteDatabase db = clientsDbHelper.getWritableDatabase();
        Cursor cursor = db.query(ClientsEntry.TABLE_NAME,           // The table to query
                null,                                               // The columns to return
                selection,                                          // The columns for the WHERE clause
                selectionArgs,                                      // The values for the WHERE clause
                null,                                               // don't group the rows
                null,                                               // don't filter by row groups
                null                                                // The sort order
        );

        if (cursor.moveToFirst()) {
            id =  cursor.getLong(cursor.getColumnIndexOrThrow(ClientsEntry.COLUMN_CLIENT_ID));
        }

        cursor.close();
        return id;
    }

    public int delete(String selection, String[] selectionArgs) {
        final SQLiteDatabase db = clientsDbHelper.getWritableDatabase();
        int rows = db.delete(ClientsEntry.TABLE_NAME, selection, selectionArgs);
        return rows;
    }

//    public ClientForm findClient(long senderId) {
//        String selection = ClientsEntry.COLUMN_EMAIL + " = ?";
//        String selectionArgs[] = {email};
//        Log.d("DB", "findClientInfo: " + email);
//
//        SQLiteDatabase db = clientsDbHelper.getWritableDatabase();
//        Cursor cursor = db.query(ClientsEntry.TABLE_NAME,           // The table to query
//                null,                                               // The columns to return
//                selection,                                          // The columns for the WHERE clause
//                selectionArgs,                                      // The values for the WHERE clause
//                null,                                               // don't group the rows
//                null,                                               // don't filter by row groups
//                null                                                // The sort order
//        );
//
//        if (cursor.moveToFirst()) {
//            return cursor.getLong(cursor.getColumnIndexOrThrow(ClientsEntry.COLUMN_CLIENT_ID));
//        }
//
//        cursor.close();
//        return -1;
//    }
}
