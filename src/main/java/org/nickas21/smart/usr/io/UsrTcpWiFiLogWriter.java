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

import static org.nickas21.smart.util.StringUtils.bytesToHex;

@Component
public class UsrTcpWiFiLogWriter implements Closeable {

    private BufferedWriter writerLast;
    private BufferedWriter writerError;


    @Value("${usr.tcp.logs-dir:}")
    String logsDir;

    @Value("${usr.tcp.file-last:}")
    String fileLast;

    @Value("${usr.tcp.file-error:}")
    String fileError;

    @PostConstruct
    public void init() throws IOException {
        this.writerLast = openWriter(fileLast);
        this.writerError = openWriter(fileError);
    }

    private BufferedWriter openWriter(String name) throws IOException {
        File file = Paths.get(logsDir, name).toFile();
        return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
    }

    // ---- ЗАПИС В LAST ----
    public synchronized void writeLast(UsrTcpWiFiPacketRecord rec) throws IOException {
        writerLast.write(getLine(rec));
        writerLast.flush();
    }

    // ---- ЗАПИС В ERROR ----
    public synchronized void writeError(UsrTcpWiFiPacketRecord rec) throws IOException {
        writerError.write(getLine(rec));
        writerError.flush();
    }


    private String getLine(UsrTcpWiFiPacketRecord rec) throws IOException {
        String hexPayload = bytesToHex(rec.payload());
        return
                rec.timestamp() + ";" +
                        rec.port() + ";" +
                        rec.type() + ";" +
                        rec.payloadLength() + ";" +
                        hexPayload + "\n";
    }

    @Override
    public void close() throws IOException {
        writerLast.close();
        writerError.close();
    }

}
