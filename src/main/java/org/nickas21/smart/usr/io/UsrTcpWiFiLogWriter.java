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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class UsrTcpWiFiLogWriter implements Closeable {

    private BufferedWriter writerLast;
    private BufferedWriter writerError;

    @Value("${usr.tcp.file-cur:usrTcpWiFiCur.log}")
    private String fileCur;

    @Value("${usr.tcp.logs-dir:}")
    String logsDir;

    @Value("${usr.tcp.file-last:}")
    String fileLast;

    @Value("${usr.tcp.file-error:}")
    String fileError;

    @PostConstruct
    public void init() throws IOException {
        try {
            initInternal();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void initInternal() throws IOException {
        if (logsDir == null || logsDir.isBlank()) {
            logsDir = "/tmp/usr-bms";   // fallback for Kubernetes
        }

        Path dir = Paths.get(logsDir);
        Files.createDirectories(dir);

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
        writerLast.write(rec.toLine());
        writerLast.flush();
    }

    // ---- ЗАПИС В ERROR ----
    public synchronized void writeError(UsrTcpWiFiPacketRecord rec) throws IOException {
        writerError.write(rec.toLine());
        writerError.flush();
    }

    @Override
    public void close() throws IOException {
        writerLast.close();
        writerError.close();
    }

}
