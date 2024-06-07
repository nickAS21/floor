package org.nickas21.smart.data.service;

import org.nickas21.smart.data.entity.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;

@Service
public class TelegramService {

    @Value("${telegram.chat-id}")
    private String chatId;

    @Autowired
    private TelegramBot telegramBot;

    public Mono<Boolean> sendNotification(String message) {
        try {
            telegramBot.sendMessage(chatId, message);
            return Mono.just(true);
        } catch (TelegramApiException e) {
            return Mono.error(e);
        }
    }
}

