package org.nickas21.smart.data.service;

import org.nickas21.smart.data.entity.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;
import java.util.Map.Entry;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateTimeStringToTelegram;

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
            String msgGridStatus = gridStateOnLine.getValue() ? "підключена" : "відключена";
            msg = "Перезавантаження програми.\nПочаток відстеження стану мережи з:\n - " + timeUpdateStr + ",\n - стан мережи: [" +  msgGridStatus + "].";
            this.sendNotification(msg);
        }
        return msg;
    }
    public String sendMsgToTelegram(Long lastUpdateTimeGridStatusInfo, Entry<Long, Boolean> gridStateOnLine) {
        String msg = null;
        if (telegramBot.isStateStart()){
            String timeBeforeStr = toLocaleDateTimeStringToTelegram(lastUpdateTimeGridStatusInfo);
            String timeUpdateStr = toLocaleDateTimeStringToTelegram(gridStateOnLine.getKey());
            String msgGrid = gridStateOnLine.getValue() ? "Мережа була відсутня" : "Mережа була підключена";
            String msgGridStatus = gridStateOnLine.getValue() ? "Мережа підключена." : "Mережа відключена.";
            long duration = gridStateOnLine.getKey() - lastUpdateTimeGridStatusInfo;
            long durationMin = duration/1000/60;
            long durationHour = durationMin/60;
            durationMin = durationMin -  durationHour*60;
            String durationStr = durationHour >= 24 ? "більше " + durationHour/24 + " діб/доби" :
                    durationHour > 0 ? durationHour + " годин(и/а), " + durationMin + " хв." :
                            durationMin + " хв.";
           msg = "Станом на: [" + timeUpdateStr + "]\n - " +  msgGridStatus + "\n" +
                    msgGrid + " з [" + timeBeforeStr + "] по [" + timeUpdateStr + "],\n- тривалість: [" + durationStr + "].";
            this.sendNotification(msg);
        }
        return msg;
    }
}

