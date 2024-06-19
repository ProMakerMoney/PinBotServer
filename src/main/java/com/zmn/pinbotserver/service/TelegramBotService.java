package com.zmn.pinbotserver.service;

import com.zmn.pinbotserver.MyTelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
public class TelegramBotService {

    private final MyTelegramBot telegramBot;

    public TelegramBotService(@Value("${telegram.bot.token}") String botToken,
                              @Value("${telegram.bot.username}") String botUsername,
                              @Value("${telegram.chat.id}") String chatId) throws TelegramApiException {
        this.telegramBot = new MyTelegramBot(botToken, botUsername, chatId);

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBot);
    }

    public void sendMessageToTelegram(String text) {
        SendMessage message = new SendMessage();
        message.setChatId(telegramBot.getChatId());
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

