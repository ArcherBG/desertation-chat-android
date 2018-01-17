package com.example.kkostov.chat.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.kkostov.chat.R;
import com.example.kkostov.chat.broadcastreceivers.NetworkChangeReceiver;
import com.example.kkostov.chat.interfaces.ErrorOccurredCallback;

/**
 * Base class that handles showing UI message when there is problem
 * connecting to the server or has no internet connection.
 * <p>
 * Created by kkostov on 09-Jun-17.
 */

@Deprecated
public class BaseActivity extends AppCompatActivity implements ErrorOccurredCallback {

    private static final String TAG = "BaseActivity";
    protected TextView ivNoService;

    // public   abstract  void showNoServiceUI(boolean show){};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NetworkChangeReceiver receiver = new NetworkChangeReceiver();
        receiver.setListener(BaseActivity.this);
    }

    @Override
    public void hasConnection(boolean hasConnection) {

        Log.d(TAG, "hasConnection: " + hasConnection);

        if (ivNoService != null) {
            this.showNoServiceUI(!hasConnection);
        }
    }

    public void showNoServiceUI(boolean show) {
        Log.d(TAG, "showNoServiceUI: " + show);

        if (show) {
            ivNoService.setText(R.string.no_service);
            ivNoService.setVisibility(View.VISIBLE);
        } else {
            ivNoService.setVisibility(View.GONE);
        }
    }


}
