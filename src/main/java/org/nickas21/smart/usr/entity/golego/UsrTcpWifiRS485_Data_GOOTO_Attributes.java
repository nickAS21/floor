package org.nickas21.smart.usr.entity.golego;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiRS485_Data_GOOTO_Attributes {

    // CID2 Constants
    public static final int CMD_CID2_4F = 0x4F;
    public static final int CMD_CID2_51 = 0x51;
    public static final int CMD_CID2_60 = 0x60;
    public static final int CMD_CID2_93 = 0x93;

    // --- Firmware, Protocol & Model Info ---
    private String protocolVersion;       // 0x4F
    private String manufacturerInfo;      // 0x51
    private String systemName;            // 0x60 (Device/HW Model)
    private String softwareVersion;        // 0x60 (FW Version)
    private Integer moduleCount;          // 0x60

    // --- Serial Numbers ---
    private String serialNumber;          // 0x93 (Pack SN)

    // --- Initial FETs & Alarms ---
    private Boolean chargeFetStatus;      // 0x44
    private Boolean dischargeFetStatus;   // 0x44
    private String systemAlarmSummary;    // 0x44
    private Integer rawAlarmFlags;        // 0x44

    // Unified Metadata Timestamp
    private Instant timestamp;

    private byte[] payloadBytesCur;
    private byte[] payloadBytesLastSaved;

    // -------------------------------------------------------------
    //   PARSERS
    // -------------------------------------------------------------

    public void parseAndUpdate44(String asciiFrame) {
        try {
            if (asciiFrame == null || asciiFrame.isEmpty()) {
                return;
            }

            this.payloadBytesCur = asciiFrame.getBytes(StandardCharsets.US_ASCII);


            String[] frames = asciiFrame.split("(?=~)");


//            for (String frame : frames) {
//                frame = frame.trim();
//
//                if (!frame.startsWith("~") || frame.length() < 19) {
//                    continue;
//                }
//
//                String dataHex = frame.substring(13, frame.length() - 4);
//
//                if (dataHex.length() != 2) {
//                    continue;
//                }
//
//                int alarmCode = Integer.parseInt(dataHex, 16);
//
//                this.rawAlarmFlags = alarmCode;
//                this.systemAlarmSummary = String.format("0x%02X", alarmCode);
//
//                log.info("RS485 0x44: frame [{}], alarmCode [{}]",
//                        frame, String.format("0x%02X", alarmCode));
//            }

        } catch (Exception e) {
            log.error("Failed to parse42 RS485 0x44 frame: {}", asciiFrame, e);
        }
    }

    public void parseAndUpdate4F(String asciiFrame, Instant requestTimestamp) {
        this.timestamp = requestTimestamp;
        this.protocolVersion = hexAsciiToString(asciiFrame);
    }

    public void parseAndUpdate51(String asciiFrame) {
        this.manufacturerInfo = hexAsciiToString(asciiFrame);
    }

    public void parseAndUpdate60(String asciiFrame) {
        try {
            String fullInfo = hexAsciiToString(asciiFrame);
            if (fullInfo != null && !fullInfo.isEmpty()) {
                this.systemName = fullInfo;
            }
        } catch (Exception e) {
            log.error("Failed to parse42 RS485 0x60 frame: {}", asciiFrame, e);
        }
    }

    public void parseAndUpdate93(String asciiFrame) {
        this.serialNumber = hexAsciiToString(asciiFrame);
    }

    private String hexAsciiToString(String asciiFrame) {
        if (asciiFrame == null || asciiFrame.isEmpty()) {
            return "N/A";
        }

        StringBuilder output = new StringBuilder();

        String[] frames = asciiFrame.split("(?=~)");

        for (String frame : frames) {
            frame = frame.trim();

            if (!frame.startsWith("~") || frame.length() <= 18) {
                continue;
            }

            String infoHex = frame.substring(13, frame.length() - 4);

            for (int i = 0; i + 2 <= infoHex.length(); i += 2) {
                int decimal = Integer.parseInt(infoHex.substring(i, i + 2), 16);

                if (decimal >= 32 && decimal <= 126) {
                    output.append((char) decimal);
                }
            }
        }

        return output.toString().trim();
    }

    public UsrTcpWiFiPacketRecord getInfoForRecords(int port) {
        if (this.payloadBytesCur == null || Arrays.equals(this.payloadBytesCur, this.payloadBytesLastSaved)) {
            return null;
        } else {
            this.payloadBytesLastSaved = this.payloadBytesCur;
            return new UsrTcpWiFiPacketRecord(
                    this.timestamp != null ? this.timestamp.toEpochMilli() : System.currentTimeMillis(),
                    port,
                    "RS485_META",
                    this.payloadBytesCur.length,
                    this.payloadBytesCur);
        }
    }
}