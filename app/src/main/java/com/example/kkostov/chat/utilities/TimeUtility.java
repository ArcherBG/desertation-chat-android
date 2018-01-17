package com.example.kkostov.chat.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kkostov on 25-May-17.
 */

public class TimeUtility {

    /**
     * @return yyyy-MM-dd HH:mm:ss format date as string
     */
    public static String getCurrentTimeStamp() {
        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");  // old "yyyy-MM-dd HH:mm:ss"
            String currentDateTime = dateFormat.format(new Date()); // Find todays date

            return currentDateTime;
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }
}
