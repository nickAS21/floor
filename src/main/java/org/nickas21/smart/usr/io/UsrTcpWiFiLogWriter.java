package org.nickas21.smart.usr.io;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Component
public class UsrTcpWiFiLogWriter implements Closeable {

    private BufferedWriter writer;


    @Value("${usr.tcp.logs-dir:}")
    String logsDir;

    @Value("${usr.tcp.file-last:}")
    String fileLast;

    @PostConstruct
    public void init() throws IOException {
        File file = Paths.get(logsDir, fileLast).toFile();
        this.writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
    }

    public synchronized void append(UsrTcpWiFiPacketRecord rec) throws IOException {
        String hexPayload = bytesToHex(rec.payload());
        writer.write(
                rec.timestamp() + ";" +
                        rec.port() + ";" +
                        rec.type() + ";" +
                        rec.payloadLength() + ";" +
                        hexPayload
        );
        writer.write("\n");
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
