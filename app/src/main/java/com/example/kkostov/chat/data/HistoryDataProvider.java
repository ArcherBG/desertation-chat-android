package com.example.kkostov.chat.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.kkostov.chat.data.HistoryContract.HistoryEntry;


/**
 * This class provides ability to read  from database.
 */
public class HistoryDataProvider extends ContentProvider {
    private static final String TAG = HistoryDataProvider.class.getSimpleName();

    // Creates a UriMatcher object.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static final int HISTORY = 100;
    static final int HISTORY_ID = 101;

    private HistoryDbHelper historyDbHelper;


    static {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final String authority = HistoryContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        sUriMatcher.addURI(authority, HistoryContract.PATH_HISTORY, HISTORY);
        sUriMatcher.addURI(authority, HistoryContract.PATH_HISTORY + "/*", HISTORY);
        sUriMatcher.addURI(authority, HistoryContract.PATH_HISTORY + "/#", HISTORY_ID);
    }

//    static {
//        /*
//         * The calls to addURI() go here, for all of the content URI patterns that the provider
//         * should recognize.
//         */
//
//        /*
//         * Sets the integer value for multiple rows in table to 1. Notice that no wildcard is used
//         * in the path
//         */
//        sUriMatcher.addURI(HistoryEntry.CONTENT_TYPE, HistoryEntry.TABLE_NAME, 1);
//
//        /*
//         * Sets the code for a single row to 2. In this case, the "#" wildcard is
//         * used. "content://com.example.app.provider/table3/3" matches, but
//         * "content://com.example.app.provider/table3 doesn't.
//         */
//        sUriMatcher.addURI(HistoryEntry.CONTENT_TYPE, HistoryEntry.TABLE_NAME + "/#", 2);
//    }

    public boolean onCreate() {
        //  Note: Because they can be long-running, be sure that you call getWritableDatabase() or getReadableDatabase() in a background thread, such as with AsyncTask or IntentService.
        historyDbHelper = new HistoryDbHelper(this.getContext());
        return true;
    }

//    /**
//     * Read data from DB by selecting  row.
//     */
//    public Cursor findRow(long senderId, long receiverId) {
//
//        SQLiteDatabase sqLiteDatabase = historyDbHelper.getReadableDatabase();
//
//            // Use a cursor to query the database.
//            Cursor cursor = sqLiteDatabase.query(
//                    HistoryEntry.TABLE_NAME,  // table name
//                    null, // leaving "columns" null just returns all the columns.
//                    HistoryEntry.COLUMN_ID + " =?",  // cols for "where" clause
//                    new String[]{Integer.toString(rowId)}, // values for "where" clause
//                    null, // columns to group by
//                    null, // columns to filter by row groups
//                    null  // sort order
//            );
//            return null;
//
//    }

//    public int getCount() {
//        if (!sqLiteDatabase.isOpen()) {
//            HistoryDbHelper dbHelper = new HistoryDbHelper(context);
//            sqLiteDatabase = dbHelper.getWritableDatabase();
//            return 0;
//        } else {
//            long rowCount = DatabaseUtils.queryNumEntries(sqLiteDatabase, HistoryEntry.TABLE_NAME);
//            return (int) rowCount;
//        }
//    }


//    public void closeConnectionToDb() {
//        if (sqLiteDatabase.isOpen()) {
//            sqLiteDatabase.close();
//        }
//    }

    // Implements ContentProvider.query()
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        final SQLiteDatabase db = historyDbHelper.getReadableDatabase();
        Cursor retCursor = null;
        switch (match) {
            case HISTORY:
                retCursor = db.query(
                        HistoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case HISTORY_ID:
                long _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        HistoryEntry.TABLE_NAME,
                        projection,
                        HistoryEntry._ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Set the notification URI for the cursor to the one passed into the function. This
        // causes the cursor to register a content observer to watch for changes that happen to
        // this URI and any of it's descendants. By descendants, we mean any URI that begins
        // with this path.
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = historyDbHelper.getWritableDatabase();
        long _id;
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case HISTORY:
                _id = db.insert(HistoryEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = HistoryEntry.buildHistoryUri(_id);
                } else {
                    throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Use this on the URI passed into the function to notify any observers that the uri has
        // changed.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = historyDbHelper.getWritableDatabase();
        int rows; // Number of rows effected

        switch (sUriMatcher.match(uri)) {
            case HISTORY:
                rows = db.delete(HistoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because null could delete all rows:
        if (selection == null || rows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }


    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = historyDbHelper.getWritableDatabase();
        int rows;

        switch (sUriMatcher.match(uri)) {
            case HISTORY:
                rows = db.update(HistoryEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case HISTORY:
                return HistoryEntry.CONTENT_TYPE;
            case HISTORY_ID:
                return HistoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

} // end class
