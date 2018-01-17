package com.example.kkostov.chat.models;

/**
 * Created by kkostov on 26-May-17.
 */
public class ClientListItemContent {

    private String pathClientAvatar;
    private long clientId;
    private String email;
    private int newMessagesCount;
    private String lastMessageTimestamp;

    public ClientListItemContent()
    {}

    public ClientListItemContent(long clientId, String clientEmail) {
        this.clientId = clientId;
        this.email = clientEmail;
    }


    public String getPathClientAvatar() {
        return pathClientAvatar;
    }

    public void setPathClientAvatar(String pathClientAvatar) {
        this.pathClientAvatar = pathClientAvatar;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getNewMessagesCount() {
        return newMessagesCount;
    }

    public void setNewMessagesCount(int newMessagesCount) {
        this.newMessagesCount = newMessagesCount;
    }

    public String getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(String lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }


}
