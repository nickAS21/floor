package org.nickas21.smart.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;

@Slf4j
public class HttpUtil {
    //    private static final RestTemplate httpClient = new RestTemplate();
    public static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public static final SimpleDateFormat formatter_D_M_Y = new SimpleDateFormat("dd/MM/yyyy");
    public static final String tempSetKey = "temp_set";
    public static final String tempCurrentKey = "temp_current";
    public static final String bmsSocKey = "BMS_SOC";
    public static final String totalSolarPowerKey = "S_P_T";
    public static final String totalEnergySellKey = "E_S_TO";
    public static final String totalConsumptionPowerKey = "E_Puse_t1";
    public static final String tuyaTokenInvalid = "token invalid";
    public static final int tuyaTokenInvalidCode = 1010;

    public static String creatHttpPathWithQueries(String path, Map<String, Object> queries) {
        String pathWithQueries = path;
        if (queries != null) {
            pathWithQueries += "?" + queries.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                    .collect(Collectors.joining("&"));
        }
        return pathWithQueries;
    }

    public static String getBodyHash(String body) throws Exception {
        if (StringUtils.isBlank(body)) {
            return EMPTY_HASH;
        } else {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(messageDigest.digest());
        }
    }

    public static String get_SHA_256_SecurePassword(String passwordToHash) {
        String generatedPassword = null;
        try {
            String salt = getSalt();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] bytes = md.digest(passwordToHash.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16)
                        .substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    private static String getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt.toString();
    }

    public static Date[] getSunRiseSunset(double locationLat, double locationLng) {
        Date[] result = new Date[2];
        log.info("GetSunRiseSunset Calendar dateTime: [{}]", Calendar.getInstance().getTime());
        Calendar[] calendars = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), locationLat, locationLng);
        result[0] = calendars[0].getTime();
        result[1] = calendars[1].getTime();
        log.info("Sunrise at: [{}]", result[0]);
        log.info("Sunset at: [{}]", result[1]);
        return result;
    }
}
