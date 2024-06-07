package org.nickas21.smart.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.entity.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
public class TelegramBotConfig {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Bean
    public TelegramBot telegramBot() {
        TelegramBot bot = new TelegramBot();
        bot.setBotUsername(botUsername);
        bot.setBotToken(botToken);
        return bot;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot bot)  {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            log.info("TelegramBotsApi is started successful.");
            return botsApi;
        } catch (TelegramApiException e) {
            log.error("TelegramBotsApi is null. BotUsername: [{}] BotToken: [{}] error: [{}]", bot.getBotUsername(), bot.getBotToken(), e.getMessage());
            return null;
        }

    }
}

