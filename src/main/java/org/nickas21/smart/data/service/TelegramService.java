package org.nickas21.smart.data.service;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.telegram.TelegramBot;
import org.nickas21.smart.data.telegram.TelegramBotSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import reactor.core.publisher.Mono;

import java.util.Map.Entry;

import static org.nickas21.smart.util.HttpUtil.toLocaleDateTimeStringToTelegram;
import static org.nickas21.smart.util.HttpUtil.toUsDateTimeStringToTelegram;

@Slf4j
@Service
public class TelegramService implements TelegramServiceImpl {


    @Value("${telegram.bot.dacha.username}")
    private String botUsernameDacha;

    @Value("${telegram.bot.dacha.token}")
    private String botTokenDacha;

    @Value("${telegram.bot.dacha.chat-id}")
    private String chatIdDacha;

    @Value("${telegram.bot.home.username}")
    private String botUsernameHome;

    @Value("${telegram.bot.home.token}")
    private String botTokenHome;

    @Value("${telegram.bot.home.chat-id}")
    private String chatIdHome;

    private TelegramBot telegramBotDacha;
    private TelegramBot telegramBotHome;
    private BotSession telegramBotSessionDacha;
    private BotSession telegramBotSessionHome;

    public void  init() {
        this.telegramBotDacha = new TelegramBot(botUsernameDacha, botTokenDacha, chatIdDacha, "Dacha");
        this.telegramBotHome = new TelegramBot(botUsernameHome, botTokenHome, chatIdHome, "Home");
        this.telegramBotSessionDacha = this.initApi(this.telegramBotDacha);
        this.telegramBotSessionHome = this.initApi(this.telegramBotHome);
    }

    public Mono<Boolean> sendNotification(TelegramBot bot, String message) {
        try {
            bot.sendMessage(bot.getChatId(), message);
            return Mono.just(true);
        } catch (TelegramApiException e) {
            log.error("TelegramApiException [{}]", bot.getHouseName(), e);
            return Mono.error(e);
        }
    }

    public String sendFirstMsgToTelegram(TelegramBot bot, Entry<Long, Boolean> gridStateOnLine) {
        String msg = null;
        if (bot.isStateStart()) {
            String timeUpdateStr = toLocaleDateTimeStringToTelegram(gridStateOnLine.getKey());
            String timeUpdateStrEng = toUsDateTimeStringToTelegram(gridStateOnLine.getKey());
            String msgGridStatus = gridStateOnLine.getValue() ? "підключена" : "відключена";
            String msgGridStatusEng = gridStateOnLine.getValue() ? "turned on" : "turned off";
            msg = "Перезавантаження програми.\nПочаток відстеження стану мережи з:\n - " + timeUpdateStr + ",\n - стан мережи: [" + msgGridStatus + "].";
            this.sendNotification(bot, msg);
            msg = "Restarting the program.\nStart tracking Grid status with:\n - " + timeUpdateStrEng + ",\n - Grid state: [" + msgGridStatusEng + "].";
        }
        return msg;
    }

    public String sendMsgToTelegram(TelegramBot telegramBot, Long lastUpdateTimeGridStatusInfo, Entry<Long, Boolean> gridStateOnLine) {
        String msg = null;
        if (telegramBot.isStateStart()) {
            String timeBeforeStr = toLocaleDateTimeStringToTelegram(lastUpdateTimeGridStatusInfo);
            String timeBeforeStrEng = toUsDateTimeStringToTelegram(lastUpdateTimeGridStatusInfo);
            String timeUpdateStr = toLocaleDateTimeStringToTelegram(gridStateOnLine.getKey());
            String timeUpdateStrEng = toUsDateTimeStringToTelegram(gridStateOnLine.getKey());
            String msgGrid = gridStateOnLine.getValue() ? "Мережа була відсутня" : "Mережа була підключена";
            String msgGridEng = gridStateOnLine.getValue() ? "Grid was no connected" : "Grid was connected";
            String msgGridStatus = gridStateOnLine.getValue() ? "Мережа підключена." : "Mережа відключена.";
            String msgGridStatusEng = gridStateOnLine.getValue() ? "Grid is turned on." : "Grid was turned off.";
            long duration = gridStateOnLine.getKey() - lastUpdateTimeGridStatusInfo;
            long durationMin = duration / 1000 / 60;
            long durationHour = durationMin / 60;
            long duration24 = durationHour / 24;
            durationHour = durationHour - duration24 * 24;
            durationMin = durationMin - duration24 * 24 * 60 - durationHour * 60;
            String durationStr = duration24 > 0 ?
                    duration24 + " d," + durationHour + " h, " + durationMin + " min." :
                    durationHour > 0 ?
                            durationHour + " h, " + durationMin + " min." :
                            durationMin + " min.";
            msg = "Станом на: [" + timeUpdateStr + "]\n - " + msgGridStatus + "\n" +
                    msgGrid + " з [" + timeBeforeStr + "] по [" + timeUpdateStr + "],\n- тривалість: [" + durationStr + "].";
            this.sendNotification(telegramBot, msg);
            msg = "As of: [" + timeUpdateStrEng + "]\n - " + msgGridStatusEng + "\n" +
                    msgGridEng + " with [" + timeBeforeStrEng + "] to [" + timeUpdateStrEng + "],\n- duration: [" + durationStr + "].";
        }
        return msg;
    }

    public TelegramBot getTelegramBotDacha() {
        return telegramBotDacha;
    }

    public TelegramBot getTelegramBotHome() {
        return telegramBotHome;
    }

    private TelegramBotSession initApi(TelegramBot bot) {
        if (bot != null) {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(TelegramBotSession.class);
                TelegramBotSession botSession = (TelegramBotSession) botsApi.registerBot(bot);
                bot.setStateStart(true);
                log.info("TelegramBotsApiDacha is started successful.");
                return botSession;
            } catch (TelegramApiException e) {
                log.error("TelegramBotsApiDacha is null. BotUsername: [{}] BotToken: [{}] error: [{}]", bot.getBotUsername(), bot.getBotToken(), e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public void preDestroy() {
        if (telegramBotSessionDacha != null) {
            log.info("Telegram botDacha stateStart: [{}]", telegramBotDacha.isStateStart());
            telegramBotSessionDacha.stop();
        }
        if (telegramBotDacha.isStateStart()) {
            try {
                telegramBotDacha.onClosing();
                log.info("Telegram botDacha unregistered successfully.");
            } catch (Exception e) {
                log.error("Failed to unregister botDacha: {}", e.getMessage());
            }
        }
        if (telegramBotSessionHome != null) {
            log.info("Telegram botHome stateStart: [{}]", telegramBotHome.isStateStart());
            telegramBotSessionHome.stop();
        }
        if (telegramBotHome.isStateStart()) {
            try {
                telegramBotHome.onClosing();
                log.info("Telegram botHome unregistered successfully.");
            } catch (Exception e) {
                log.error("Failed to unregister botHome: {}", e.getMessage());
            }
        }
    }
}
