package org.nickas21.smart.data.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;

    private final String chatId;
    private final String houseName;
    private boolean stateStart;

    public TelegramBot(String botUsername, String botToken, String chatId, String houseName) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.chatId = chatId;
        this.houseName = houseName;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
    public String getChatId() {
        return chatId;
    }
    public String getHouseName() {
        return houseName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Here you can handle incoming messages if necessary
    }

    public boolean isStateStart() {
        return this.stateStart;
    }

    public void setStateStart(boolean stateStart) {
        this.stateStart = stateStart;
    }

    public void sendMessage(String chatId, String message) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        execute(sendMessage);
    }
}
