package org.nickas21.smart.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.entity.DefaultBotSessionFloor;
import org.nickas21.smart.data.entity.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

@Slf4j
@Configuration
public class TelegramBotConfig {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private TelegramBotsApi botsApi;
    private TelegramBot bot;
    private BotSession botSession;

    @Bean
    public TelegramBot telegramBot() {
        bot = new TelegramBot();
        bot.setBotUsername(botUsername);
        bot.setBotToken(botToken);
        return bot;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot bot) {
        try {
            botsApi = new TelegramBotsApi(DefaultBotSessionFloor.class);
            botSession =  botsApi.registerBot(bot);
            bot.setStateStart(true);
            log.info("TelegramBotsApi is started successful.");
            return botsApi;
        } catch (TelegramApiException e) {
            log.error("TelegramBotsApi is null. BotUsername: [{}] BotToken: [{}] error: [{}]", bot.getBotUsername(), bot.getBotToken(), e.getMessage());
            return null;
        }
    }

    public void preDestroy() {
        log.info("Telegram bot stateStart: [{}]", bot.isStateStart());
        if (botSession != null) {
            botSession.stop();
        }
        if (bot.isStateStart()) {
            try {
                bot.onClosing();
                botsApi = null;
                log.info("Telegram bot unregistered successfully.");
            } catch (Exception e) {
                log.error("Failed to unregister bot: {}", e.getMessage());
            }
        }
    }
}
