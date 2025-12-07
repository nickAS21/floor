package org.nickas21.smart.usr.io;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Slf4j
@Component
public class UsrTcpWiFiLogWriter implements Closeable {

    private BufferedWriter writerLast;
    private BufferedWriter writerError;

    private String  logDir;

    public void init(String  logDir, UsrTcpWiFiProperties usrTcpWiFiProperties) throws IOException {
        try {
            log.info("UsrTcpWiFiLogWriter init... logDir: {}", logDir);
            this.logDir = logDir;
            this.writerLast = openWriter(usrTcpWiFiProperties.getFileLast());
            this.writerError = openWriter(usrTcpWiFiProperties.getFileError());
        } catch (Exception e) {
            log.error("Failed start ", e);
            throw e;
        }
    }

    private BufferedWriter openWriter(String name) throws IOException {
        File file = Paths.get(this.logDir, name).toFile();
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
