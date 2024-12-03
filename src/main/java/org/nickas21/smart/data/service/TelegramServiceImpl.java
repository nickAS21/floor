package org.nickas21.smart.data.service;

import org.nickas21.smart.data.telegram.TelegramBot;
import reactor.core.publisher.Mono;

import java.util.Map.Entry;

public interface TelegramServiceImpl {
    Mono<Boolean> sendNotification(TelegramBot telegramBot, String message);
    String sendFirstMsgToTelegram(TelegramBot telegramBot, Entry<Long, Boolean> gridStateOnLine);
    String sendMsgToTelegram(TelegramBot telegramBot, Long lastUpdateTimeGridStatusInfo, Entry<Long, Boolean> gridStateOnLine);
}
