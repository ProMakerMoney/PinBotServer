package com.zmn.pinbotserver;

import lombok.Getter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MyTelegramBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;
    @Getter
    private final String chatId;

    public MyTelegramBot(String botToken, String botUsername, String chatId) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatId = chatId;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Этот метод должен быть реализован, но мы его не используем в данном случае
    }

    public void sendMessage(String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

