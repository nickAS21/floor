package org.nickas21.smart.usr.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class UsrTcpWiFiLogReader implements Closeable {

    private final BufferedReader reader;

    public UsrTcpWiFiLogReader(File file) throws IOException {
        this.reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
        );
    }

    public UsrTcpWiFiPacketRecord readNext() throws IOException {
        String line = reader.readLine();
        if (line == null) return null;

        // *** ОПТИМІЗАЦІЯ: ВИКЛИК СТАТИЧНОГО МЕТОДУ ДЛЯ УНИКНЕННЯ ДУБЛЮВАННЯ ***
        return UsrTcpWiFiPacketRecord.fromLine(line);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}