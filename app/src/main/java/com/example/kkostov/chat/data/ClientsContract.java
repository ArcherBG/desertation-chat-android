package com.example.kkostov.chat.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by kkostov on 26-May-17.
 */

public class ClientsContract  {

    public static final String CONTENT_AUTHORITY = "com.example.kkostov.chat";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_HISTORY = "clients";


    public  static class ClientsEntry  implements BaseColumns{

        // In order not to instantiate the class
        private ClientsEntry() {}

        public static final String TABLE_NAME = "clients";
        public static final String COLUMN_CLIENT_ID = "client_id";
        public static final String COLUMN_EMAIL = "email";

    }
}
