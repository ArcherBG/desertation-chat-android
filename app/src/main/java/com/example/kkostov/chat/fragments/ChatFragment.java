package com.example.kkostov.chat.fragments;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.kkostov.chat.R;
import com.example.kkostov.chat.adapters.HistoryCursorAdapter;
import com.example.kkostov.chat.data.ClientsDataProvider;
import com.example.kkostov.chat.data.HistoryContract.HistoryEntry;
import com.example.kkostov.chat.models.ClientForm;
import com.example.kkostov.chat.models.ClientListItemContent;
import com.example.kkostov.chat.models.MessageForm;
import com.example.kkostov.chat.sharedpreferences.SessionManager;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChatFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private static final String RECEIVER_ID_BUNDLE_KEY = "receiverid";
    private final long MY_ID = SessionManager.getInstance((Context) getActivity()).getClientId();

    private HistoryCursorAdapter cursorAdapter;

    // Views
    private ListView listView;
    private EditText etUserInput;
    private Button btnSendMessage;

    private long receiverId = 0;


    private static final String[] HISTORY_COLUMNS = {
            HistoryEntry.TABLE_NAME + "." + HistoryEntry._ID,
            HistoryEntry.COLUMN_RECEIVER_ID,
            HistoryEntry.COLUMN_MESSAGE
    };

    public static ChatFragment newInstance(long receiverId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putLong(RECEIVER_ID_BUNDLE_KEY, receiverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the id from bundle
        receiverId = getArguments().getLong(RECEIVER_ID_BUNDLE_KEY, 0);

        // If finding receiver id was not successful  exit fragment
        if (receiverId <= 0) {
            Toast.makeText((Context) getActivity(), "Error getting receiver info", Toast.LENGTH_LONG).show();
            getActivity().onBackPressed();
        }

        // Prepare the loader.  Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(0, null, this);

    }

    private void getReceiverIdFromDb(final String email) {
        // Query the db on worker thread
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ClientsDataProvider clientsDataProvider = new ClientsDataProvider((Context) getActivity());
                receiverId = clientsDataProvider.findClientId(email);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        // initialize the adapter
        cursorAdapter = new HistoryCursorAdapter(getActivity(), null, true);

        // Get a reference to the ListView, and attach this adapter to it.
        listView = (ListView) rootView.findViewById(R.id.lvChat);
        listView.setDivider(null);
        listView.setAdapter(cursorAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

           Cursor cursor  = (Cursor) listView.getItemAtPosition(position);
                // Get data
                int isReceived=  cursor.getInt(cursor.getColumnIndex(HistoryEntry.COLUMN_IS_MESSAGE_RECEIVED));
                int isOutgoing = cursor.getInt(cursor.getColumnIndex(HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING));
                String message = cursor.getString(cursor.getColumnIndex(HistoryEntry.COLUMN_MESSAGE));
                long receiverId = cursor.getLong(cursor.getColumnIndex(HistoryEntry.COLUMN_RECEIVER_ID));
                int messageId = cursor.getInt(cursor.getColumnIndex(HistoryEntry._ID));

                if(isReceived  == 0 && isOutgoing == 1) {
                    mListener.sendMessageAgain(message, receiverId, messageId);
                }
                return false;
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUserInput = (EditText) getActivity().findViewById(R.id.etUserInput);
        btnSendMessage = (Button) getActivity().findViewById(R.id.btnSendMessage);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do not send empty messages
                if (etUserInput.getText().toString().trim().length() == 0) {
                    return;
                }

                String text = etUserInput.getText().toString().trim();
                Log.d(TAG, "onClick send btn: " + text);
                mListener.sendMessageButtonClicked(text, receiverId);
                etUserInput.setText(null);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener.markMessagesWithClientAsRead(receiverId);
    }

    /**
     * Implementation below for Loader callback methods
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.

        //  WHERE (senderId = 5 and  receiverId = 1)  OR    ( senderId = 1 and receiverId = 5)
        // One time the user of this app is the sender and on the receiver.
        String selection = "( " + HistoryEntry.COLUMN_SENDER_ID + " = ? " + " AND " +
                HistoryEntry.COLUMN_RECEIVER_ID + " = ? " +
                " ) "
                + " OR ( " +
                HistoryEntry.COLUMN_SENDER_ID + " = ? " + " AND " +
                HistoryEntry.COLUMN_RECEIVER_ID + " = ? " +
                " )";
        String selectionArgs[] = {String.valueOf(MY_ID), String.valueOf(receiverId), String.valueOf(receiverId), String.valueOf(MY_ID)};
        String sortOrder = HistoryEntry._ID + " ASC";

        return new CursorLoader(getActivity(),
                HistoryEntry.CONTENT_URI,
                null,   // return all columns
                selection,
                selectionArgs,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished: rows count:" + cursor.getCount() + " poss: " + cursor.getPosition());
        cursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }

    public interface MainFragmentListener {

        void sendMessageButtonClicked(String text, final long receiverId);

        void markMessagesWithClientAsRead(long receiverId);

        void sendMessageAgain(String message, long receiverId, int messageId)     ;

    }
}
