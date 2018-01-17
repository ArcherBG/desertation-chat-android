package com.example.kkostov.chat.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.kkostov.chat.interfaces.ErrorOccurredCallback;

/**
 * Created by kkostov on 09-Jun-17.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private ErrorOccurredCallback listener;
    private boolean isShowing = false;

    public void setListener(Context context) {
        listener = (ErrorOccurredCallback) context;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);


        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        Log.d("NetworkChangeReceiver ", "isConnected:" + isConnected);

        if (!isConnected) {
            if (listener != null) {
                isShowing = false;
                listener.hasConnection(false);
            }
        } else {
            if (listener != null) {
                if(!isShowing) {
                    listener.hasConnection(true);
                }
                isShowing = true;
            } else {
                Log.d("NetworkChangeReceiver", "listener is null ");
            }

        }
    }
}