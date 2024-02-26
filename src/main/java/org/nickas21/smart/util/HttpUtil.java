package org.nickas21.smart.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;

@Slf4j
public class HttpUtil {
    public static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public static final SimpleDateFormat formatter_D_M_Y = new SimpleDateFormat("dd/MM/yyyy");

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

    public static Date[] getSunRiseSunset(double locationLat, double locationLng) {
        Date[] result = new Date[2];
        log.info("GetSunRiseSunset Calendar dateTime: [{}]", Calendar.getInstance().getTime());
        Calendar date = Calendar.getInstance(TimeZone.getDefault());
        if (date.get(Calendar.HOUR_OF_DAY) <= 2) {
            date.set(Calendar.HOUR_OF_DAY, 2);
        }
        Calendar[] calendars = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(date, locationLat, locationLng);
        result[0] = calendars[0].getTime();
        result[1] = calendars[1].getTime();
        log.info("Sunrise at: [{}]", result[0]);
        log.info("Sunset at: [{}]", result[1]);
        return result;
    }
}
