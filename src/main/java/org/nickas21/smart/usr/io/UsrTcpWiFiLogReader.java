package org.nickas21.smart.usr.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.nickas21.smart.util.StringUtils.hexToBytes;

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

        String[] arr = line.split(";");
        if (arr.length < 5) return null;

        long ts      = Long.parseLong(arr[0]);
        int port     = Integer.parseInt(arr[1]);
        String type  = arr[2];
        int len      = Integer.parseInt(arr[3]);
        byte[] data  = hexToBytes(arr[4]);

        return new UsrTcpWiFiPacketRecord(ts, port, type, len, data);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}