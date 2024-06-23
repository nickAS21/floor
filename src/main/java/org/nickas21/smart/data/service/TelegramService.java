package org.nickas21.smart.data.service;

import org.nickas21.smart.data.entity.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;
import java.util.Map.Entry;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateTimeStringToTelegram;
import static org.nickas21.smart.util.HttpUtil.toUsDateTimeStringToTelegram;

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

    public String sendFirstMsgToTelegram(Entry<Long, Boolean> gridStateOnLine) {
        String msg = null;
        if (telegramBot.isStateStart()){
            String timeUpdateStr = toLocaleDateTimeStringToTelegram(gridStateOnLine.getKey());
            String timeUpdateStrEng = toUsDateTimeStringToTelegram(gridStateOnLine.getKey());
            String msgGridStatus = gridStateOnLine.getValue() ? "підключена" : "відключена";
            String msgGridStatusEng = gridStateOnLine.getValue() ? "turned on" : "turned off";
            msg = "Перезавантаження програми.\nПочаток відстеження стану мережи з:\n - " + timeUpdateStr + ",\n - стан мережи: [" +  msgGridStatus + "].";
            this.sendNotification(msg);
            msg = "Restarting the program.\nStart tracking Grid status with:\n - " + timeUpdateStrEng + ",\n - Grid state: [" +  msgGridStatusEng + "].";
        }
        return msg;
    }
    public String sendMsgToTelegram(Long lastUpdateTimeGridStatusInfo, Entry<Long, Boolean> gridStateOnLine) {
        String msg = null;
        if (telegramBot.isStateStart()){
            String timeBeforeStr = toLocaleDateTimeStringToTelegram(lastUpdateTimeGridStatusInfo);
            String timeBeforeStrEng = toUsDateTimeStringToTelegram(lastUpdateTimeGridStatusInfo);
            String timeUpdateStr = toLocaleDateTimeStringToTelegram(gridStateOnLine.getKey());
            String timeUpdateStrEng = toUsDateTimeStringToTelegram(gridStateOnLine.getKey());
            String msgGrid = gridStateOnLine.getValue() ? "Мережа була відсутня" : "Mережа була підключена";
            String msgGridEng = gridStateOnLine.getValue() ? "Grid was no connected" : "Grid was connected";
            String msgGridStatus = gridStateOnLine.getValue() ? "Мережа підключена." : "Mережа відключена.";
            String msgGridStatusEng = gridStateOnLine.getValue() ? "Grid is turned on." : "Grid was turned off.";
            long duration = gridStateOnLine.getKey() - lastUpdateTimeGridStatusInfo;
            long durationMin = duration/1000/60;
            long durationHour = durationMin/60;
            long duration24 = durationHour/24;
            durationHour = durationHour - duration24*24;
            durationMin = durationMin - duration24*24*60 -  durationHour*60;
            String durationStr = duration24 > 0 ?
                    duration24 + " d," + durationHour + " h, " + durationMin + " min." :
                    durationHour > 0 ?
                            durationHour + " h, " + durationMin + " min." :
                            durationMin + " min.";
            msg = "Станом на: [" + timeUpdateStr + "]\n - " +  msgGridStatus + "\n" +
                    msgGrid + " з [" + timeBeforeStr + "] по [" + timeUpdateStr + "],\n- тривалість: [" + durationStr + "].";
            this.sendNotification(msg);
            msg = "As of: [" + timeUpdateStrEng + "]\n - " +  msgGridStatusEng + "\n" +
                    msgGridEng + " with [" + timeBeforeStrEng + "] to [" + timeUpdateStrEng + "],\n- duration: [" + durationStr + "].";
        }
        return msg;
    }
}

