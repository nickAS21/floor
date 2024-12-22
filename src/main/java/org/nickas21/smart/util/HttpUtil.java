package org.nickas21.smart.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;

@Slf4j
public class HttpUtil {

    public static final Integer tempCurrentKuhnyMin = 2;
    public static final Integer tempCurrentKuhny5 = 5;
    public static final String offOnKey = "switch";
    public static final String tempSetKey = "temp_set";
    public static final String tempCurrentKey = "temp_current";
    public static final String bmsSocKey = "BMS_SOC";
    public static final String totalSolarPowerKey = "S_P_T";
    public static final String totalEnergySellKey = "E_S_TO";
    public static final String dailyEnergySellKey = "E_S_D";
    public static final String totalEnergyBuyKey = "E_B_TO";
    public static final String dailyEnergyBuyKey = "E_B_D";
    public static final String totalConsumptionPowerKey = "E_Puse_t1";
    public static final String totalGridPowerKey = "PG_Pt1";
    public static final String gridRelayStatusKey = "GRID_RELAY_ST1";
    public static final String gridStatusKey = "ST_PG1";
    // battery
    public static final String batteryStatusKey = "B_ST1";
    public static final String batteryPowerKey = "B_P1";           // W
    public static final String batteryCurrentKey = "B_C1";         // A
    public static final String batteryVoltageKey = "B_V1";         // V
    public static final String batterySocKey = "B_left_cap1";            // %
    public static final String batteryDailyChargeKey = "Etdy_cg1";       // kWh
    public static final String batteryDailyDischargeKey = "Etdy_dcg1";   // kWh
    public static final String productionTotalSolarPowerKey = "S_P_T";   // kWh
    public static final String consumptionTotalPowerKey = "E_Puse_t1";   // kWh

    public static final Locale locale = Locale.forLanguageTag("en-US");
    public static final TimeZone timeZone = TimeZone.getTimeZone("Europe/Kyiv");
    public static final int timeLocalNightTariffStart = 23;
    public static final int timeLocalMinutesNightTariffStart_1 = 10;
    public static final int timeLocalMinutesNightTariffStart_2 = 15;
    public static final int timeLocalNightTariffFinish = 7;
    public static final int timeLocalMinutesNightTariffFinish = 50;


    public static String toLocaleTimeString(Long milliSec) {
        return toLocaleTimeString(Instant.ofEpochMilli(milliSec));
    }
    public static String toLocaleTimeString(Instant curInst) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(timeZone.toZoneId());
        return formatter.format(curInst);
    }
    public static String toLocaleDateString(Long milliSec) {
        return toLocaleDateString(Instant.ofEpochMilli(milliSec));
    }
    public static String toLocaleDateString(Instant curInst) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.FULL)
                .withLocale(locale)
                .withZone(timeZone.toZoneId());
        return formatter.format(curInst);
    }
    public static String toLocaleDateTimeString(Instant curInst) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(timeZone.toZoneId());
        return formatter.format(curInst);
    }
    public static int toLocaleDateTimeHour() {
        Instant curInst = Instant.now();
        ZonedDateTime localDateTime = curInst.atZone(ZoneId.systemDefault());
        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");
        String hour = localDateTime.format(hourFormatter);
        return Integer.parseInt(hour);
    }

    public static int toLocaleDateTimeMinutes() {
        Instant curInst = Instant.now();
        ZonedDateTime localDateTime = curInst.atZone(ZoneId.systemDefault());
        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("mm");
        String min = localDateTime.format(hourFormatter);
        return Integer.parseInt(min);
    }

    public static String toLocaleDateTimeStringToTelegram(Long milliSec) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.forLanguageTag("uk-UA"))
                .withZone(timeZone.toZoneId());
        return formatter.format(Instant.ofEpochMilli(milliSec));
    }

    public static String toUsDateTimeStringToTelegram(Long milliSec) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
                .withLocale(Locale.US)
                .withZone(timeZone.toZoneId());
        return formatter.format(Instant.ofEpochMilli(milliSec));
    }

    public static String creatHttpPathWithQueries(String path, Map<String, Object> queries) {
        String pathWithQueries = path;
        if (queries != null) {
            pathWithQueries += "?" + queries.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                    .collect(Collectors.joining("&"));
        }
        return pathWithQueries;
    }

    @SneakyThrows
    public static String getBodyHash(String body) {
        if (StringUtils.isBlank(body)) {
            return EMPTY_HASH;
        } else {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(messageDigest.digest());
        }
    }

    public static Long[] getSunRiseSunset(double locationLat, double locationLng) {
        Long[] result = new Long[2];
        log.info("GetSunRiseSunset Calendar dateTime: [{}]", toLocaleDateTimeString(Calendar.getInstance().toInstant()));
        Calendar date = Calendar.getInstance(timeZone);
        int localOffsetHrs = ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochMilli(date.getTimeInMillis())).getTotalSeconds()/60/60;

        if (date.get(Calendar.HOUR_OF_DAY) <= localOffsetHrs) {
            date.set(Calendar.HOUR_OF_DAY, localOffsetHrs);
        }
        Calendar[] calendars = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(date, locationLat, locationLng);
        result[0] = calendars[0].getTimeInMillis();
        result[1] = calendars[1].getTimeInMillis();
        log.info("Sunrise at: [{}]", toLocaleTimeString(result[0]));
        log.info("Sunset  at: [{}]", toLocaleTimeString(result[1]));
        return result;
    }
}
