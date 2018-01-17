package com.example.kkostov.chat.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.kkostov.chat.data.ClientsContract.ClientsEntry;
import com.example.kkostov.chat.data.ClientsDataProvider;
import com.example.kkostov.chat.interfaces.ErrorOccurredCallback;
import com.example.kkostov.chat.interfaces.LoginHelperCallback;
import com.example.kkostov.chat.interfaces.MainActivityHelperCallback;
import com.example.kkostov.chat.models.ClientForm;
import com.example.kkostov.chat.models.FindForm;
import com.example.kkostov.chat.models.MessageForm;
import com.example.kkostov.chat.sharedpreferences.SessionManager;
import com.example.kkostov.chat.utilities.TimeUtility;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static com.example.kkostov.chat.data.HistoryContract.HistoryEntry;

/**
 * Created by kkostov on 19-Apr-17.
 */
public class SocketService extends Service {
    private static final String TAG = SocketService.class.getSimpleName();
    private static final String SERVER_IP = "192.168.200.101";
    private static final int SERVER_PORT = 3456;
    private static final int MAX_RESTART_ATTEMPT = 3;

    private static final String COMMAND_FIND = "findclient";
    private static final String COMMAND_FIND_RESPONSE = "findclientresponse";
    private static final String COMMAND_LOGIN = "login";
    private static final String COMMAND_LOGIN_RESPONSE = "loginresponse";

    private static final String COMMAND = "command";
    private static final String WHISPER_COMMAND = "whisper";
    private static final String REGISTER_COMMAND = "register";
    private static final String COMMAND_QUIT = "quit";
    private static final String SYSTEM_MESSAGE_COMMAND = "system";
    private static final String INFO_COMMAND = "ack";
    private static final String PING_COMMAND = "ping";
    private static final String SUCCESS = "success";
    private static final String ADMIN_MODE = "adminmode";
    private static final String MESSAGE_INVALID_PARAMETERS = "invalid parameters";
    private static final String MESSAGE_LOGIN_ERROR = "login error";

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    private static final String[] projection = {
            ClientsEntry._ID,
            ClientsEntry.COLUMN_CLIENT_ID,
            ClientsEntry.COLUMN_EMAIL
    };


    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private LinkedBlockingQueue<String> messagesToSend = new LinkedBlockingQueue<>();

    // Callback listeners
    private SessionManager sessionManager;
    private LoginHelperCallback loginActivityCallback;
    private MainActivityHelperCallback mainActivityCallback;
    private ErrorOccurredCallback errorOccuredCallback;


    // Flags
    private boolean closeSocket = false;
    private boolean writing = true;
    private boolean listening = true;
    private int restartCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        sessionManager = SessionManager.getInstance(getApplicationContext());
        SocketService.this.startService();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public SocketService getService() {
            // Return this instance of SocketService so clients can call public methods
            return SocketService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void restartSocket() {
        closeSocket();

        // Start the socket again
        listening = true;
        writing = true;
        closeSocket = false;

        SocketService.this.startService();
    }

    /**
     * Restart the socket but do not send any unsend messages.
     */
    public void forceRestartSocket() {
        // Stop the socket
        listening = false;
        writing = false;
        closeSocket = true;

        // Start the socket again
        listening = true;
        writing = true;
        closeSocket = false;

        SocketService.this.startService();
    }

    public void closeSocket() {
        sendQuitMessageToServer();

        //Wait to send all msg then close the socket
        while (out!= null && messagesToSend.size() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Stop the socket
        listening = false;
        writing = false;
        closeSocket = true;
        Log.d(TAG, "closeSocket: socket is closed");
    }

    public void forceCloseSocket() {

        // Stop the socket
        listening = false;
        writing = false;
        closeSocket = true;
    }


    public void startService() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: Service started");

                while (!closeSocket) {
                    try {
                        // Open socket
                        socket = new Socket(SERVER_IP, SERVER_PORT);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Log.d(TAG, "run: Service is running");

                        runOutInWorkerThread();

                        // Remove the no service message if it has been shown.
                        if (errorOccuredCallback != null) {
                            errorOccuredCallback.hasConnection(true);
                        }

                        String msgFromServer;
                        Gson gson = new Gson();
                        while (listening) {
                            if ((msgFromServer = in.readLine()) != null) {
                                Log.d(TAG, "Message from server: " + msgFromServer);

                                // Cast to appropriate form
                                if (msgFromServer.contains(COMMAND_FIND_RESPONSE)) {
                                    FindForm findForm = gson.fromJson(msgFromServer.trim(), FindForm.class);

                                    handleFindClientResponse(findForm);
                                } else {
                                    // MessageForm is generic enough to handle the other types of commands.
                                    MessageForm messageForm = gson.fromJson(msgFromServer.trim(), MessageForm.class);

                                    // Check what type of command the server  send
                                    handleCommand(messageForm);
                                }
                            }
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();

                        Log.d(TAG, "run: in catch");

                        // Remove the sign in user to manually log in later because the server expects that
                        SessionManager.getInstance(getApplicationContext()).removeAllData();

                        // Inform the user there is no connection to the server
                        if (errorOccuredCallback != null) {
                            errorOccuredCallback.hasConnection(false);
                            if (loginActivityCallback != null) {
                                loginActivityCallback.loginResponse(false);
                            }
                        } else {
                            Log.d(TAG, "run: errorOccurredCallback is null");
                        }

//                        if( restartCount < MAX_RESTART_ATTEMPT) {
//                            restartCount++;
//                            // Wait 1 second before trying to connect
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//
//                            forceRestartSocket();
//                        }

                    } finally {

                        if (out != null) {
                            out.close();

                        }
                        try {
                            in.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // clear messages to send
                        messagesToSend.clear();

                        Log.d(TAG, "in finally run: socket closed");
                    }
                }
            }
        });
        thread.start();
    }

    private void runOutInWorkerThread() {
        Thread outThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // Send message to server
                while (writing) {
                    try {
                        // String obj = json.toJson(messagesToSend.take());
                        String msg = messagesToSend.take().trim();
                        out.println(msg);
                        Log.d(TAG, "Msg send to server: " + msg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        outThread.start();
    }

    private void handleCommand(MessageForm receivedForm) {

        // If the command is a private message  received
        if (receivedForm.getCommand().equalsIgnoreCase(WHISPER_COMMAND)) {

            if (isInputValid(WHISPER_COMMAND, receivedForm)) {

                handleReceivedWhisperMessage(receivedForm);
            }
        } else if (receivedForm.getCommand().equalsIgnoreCase(INFO_COMMAND)) {
            // Validate form
            if (receivedForm.getMessageId() < 0 && !(receivedForm.message.contains(SUCCESS))) {
                Log.d(TAG, "handleCommand " + INFO_COMMAND + " received invalid form: " + receivedForm);
                return;
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_RECEIVED, 1); // set to true

            // update row in db that the message was received
            getContentResolver().update(HistoryEntry.CONTENT_URI, contentValues,
                    HistoryEntry._ID + "= ?",
                    new String[]{Long.toString(receivedForm.getMessageId())});

        } else if (receivedForm.getCommand().equalsIgnoreCase(SYSTEM_MESSAGE_COMMAND)) {
            Log.d(TAG, "handleCommand: " + SYSTEM_MESSAGE_COMMAND + " " + receivedForm.getMessage());


        } else if (receivedForm.getCommand().equalsIgnoreCase(COMMAND_LOGIN_RESPONSE)) {

            handleLoginResponse(receivedForm);
        } else {
            Log.d(TAG, "handleCommand: Invalid command");
        }
    }

    private void handleReceivedWhisperMessage(MessageForm receivedForm) {

        // Prepare data for saving
        ContentValues contentValues = new ContentValues();
        contentValues.put(HistoryEntry.COLUMN_SENDER_ID, receivedForm.getSenderId());
        contentValues.put(HistoryEntry.COLUMN_RECEIVER_ID, receivedForm.getReceiverId());
        contentValues.put(HistoryEntry.COLUMN_MESSAGE, receivedForm.getMessage());
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING, 0); // We set to false because someone is sending the message to us.
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_RECEIVED, 1); // This is always 1 (true) because we have already received it from the server.
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_READ_BY_ME, 0); // We set to false bacause the user has not read the message yet.
        contentValues.put(HistoryEntry.COLUMN_TIMESTAMP, TimeUtility.getCurrentTimeStamp());  // Time we received the message

        // Save data in db
        getContentResolver().insert(HistoryEntry.CONTENT_URI, contentValues);

        //Check if we have the receiver stored in clients db
        ClientsDataProvider clientsDataProvider = new ClientsDataProvider(getApplicationContext());

        String selection = ClientsEntry.COLUMN_CLIENT_ID + " = ?";
        // We want the info of the client who send the message
        String selectionArgs[] = {String.valueOf(receivedForm.getSenderId())};
        Cursor cursor = clientsDataProvider.query(
                projection,                   // null = return all column
                selection,                    // WHERE clause
                selectionArgs,                // WHERE arguments
                null);

        cursor.moveToFirst();

        // If there is no record in local db, ask the server to give the client info
        if (!cursor.isFirst()) {
            findClientInfo(receivedForm.getSenderId(), null, null);
        } else {
            Log.d(TAG, "handleReceivedWhisperMessage will not insert ");
        }

        cursor.close();

        // We want to update the unread messages count in ClientsFragment
        if (mainActivityCallback != null) {
            mainActivityCallback.updateClientsFragmentUI();
        }
    }

    private void handleLoginResponse(MessageForm receivedForm) {

        if ((receivedForm.message != null) &&
                (receivedForm.message.toLowerCase().contains(MESSAGE_INVALID_PARAMETERS))) {
            if (loginActivityCallback != null) {
                loginActivityCallback.loginResponse(false);
                return;
            }

            //   forceRestartSocket();
        }

        // Old implementation
//        if (receivedForm.message.toLowerCase().contains(ADMIN_MODE)) {
//            // Check if client is admin
//            if (receivedForm.message.toLowerCase().contains("true")) {
//                sessionManager.setAdminMode(true);
//            }
//        }

        if (receivedForm.getIsAdmin() == 1) {
            sessionManager.setAdminMode(true);
        } else {
            sessionManager.setAdminMode(false);
        }

        if (receivedForm.getId() > 0) {
            sessionManager.setClientId(receivedForm.getId());
        }

        if (receivedForm.getAccessToken() != null && receivedForm.getAccessToken().trim().length() != 0) {
            sessionManager.setAccessToken(receivedForm.getAccessToken());
        }

        if (loginActivityCallback != null) {
            loginActivityCallback.loginResponse(true);
        }
    }


    private boolean isInputValid(String messageType, MessageForm form) {

        if (messageType.equals(WHISPER_COMMAND)) {
            if (form.senderId == 0) {
                Log.d(TAG, "isInputValid: SenderId is null or 0");
                return false;
            }
            if (form.message == null || form.message.trim().length() == 0) {
                Log.d(TAG, "isInputValid: MessageDto is null or empty");
                return false;
            }
            return true;
        }

        return false;
    }

    private void sendSystemMessageToServer(String errorMessage) {

        MessageForm errorForm = new MessageForm();
        errorForm.setCommand(SYSTEM_MESSAGE_COMMAND);
        errorForm.setMessage(errorMessage);

        Gson gson = new Gson();
        String sendMessage = gson.toJson(errorForm, MessageForm.class);
        messagesToSend.add(sendMessage);
    }

    public void sendWhisperMessageToServer(String message, final long receiverId, final long messageId) {

        MessageForm form = new MessageForm();
        form.setCommand(WHISPER_COMMAND);
        form.setReceiverId(receiverId);
        form.setMessageId(messageId);
        form.setMessage(message);

        Gson gson = new Gson();
        String sendMessage = gson.toJson(form, MessageForm.class);
        messagesToSend.add(sendMessage);
    }

    public void sendLoginMessageToServer(String email, String password, String accessToken, LoginHelperCallback loginActivityCallback) {

        ClientForm form = new ClientForm();
        form.setCommand(COMMAND_LOGIN);
        form.setId(sessionManager.getClientId());  // Tell the server who we are
        form.setEmail(email);
        form.setPassword(password);
        form.setAccessToken(accessToken);

        Gson gson = new Gson();
        String sendMessage = gson.toJson(form, ClientForm.class);
        messagesToSend.add(sendMessage);

        this.loginActivityCallback = loginActivityCallback;
    }

    public void findClientInfo(final long id, final String email, MainActivityHelperCallback callback) {
        FindForm form = new FindForm();
        form.setCommand(COMMAND_FIND);
        form.setClientId(id);
        form.setEmail(email);

        Gson gson = new Gson();
        String sendMessage = gson.toJson(form, FindForm.class);
        messagesToSend.add(sendMessage);

        if (callback != null) {
            this.mainActivityCallback = callback;
        }
    }

    private void handleFindClientResponse(FindForm findForm) {
        Log.d(TAG, "handleCommand: " + COMMAND_FIND_RESPONSE + "client :  " + findForm.getClientId() + " --- " + findForm.getEmail());

        // If response data is valid
        if (findForm.getClientId() > 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ClientsEntry.COLUMN_CLIENT_ID, findForm.getClientId());
            contentValues.put(ClientsEntry.COLUMN_EMAIL, findForm.getEmail());

            ClientsDataProvider clientsDataProvider = new ClientsDataProvider(getApplicationContext());
            clientsDataProvider.insert(contentValues);
        }

        if (mainActivityCallback != null) {
            mainActivityCallback.findClientResponse(findForm.getClientId());
        }
    }

    private void sendQuitMessageToServer() {
        MessageForm form = new MessageForm();
        form.setCommand(COMMAND_QUIT);

        Gson gson = new Gson();
        String sendMessage = gson.toJson(form, MessageForm.class);
        messagesToSend.add(sendMessage);
    }

    public void setMainActivityCallback(MainActivityHelperCallback callback) {
        this.mainActivityCallback = callback;
    }

    public void setErrorOccuredCallback(ErrorOccurredCallback callback) {
        this.errorOccuredCallback = callback;
    }

}
