package org.nickas21.smart.data.service.systemOut;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LineOutputStream extends OutputStream {

    private final RuntimeLogBuffer buffer;
    private final OutputStream originalStream;
    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(256);

    public LineOutputStream(RuntimeLogBuffer buffer, OutputStream originalStream) {
        this.buffer = buffer;
        this.originalStream = originalStream;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        originalStream.write(b);
        processByte(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        originalStream.write(b, off, len);
        for (int i = off; i < off + len; i++) {
            processByte(b[i] & 0xFF);
        }
    }

    private void processByte(int b) {
        if (b == '\n') {
            lineBuffer.write('\n');
            buffer.append(lineBuffer.toString(StandardCharsets.UTF_8));
            lineBuffer.reset();
        } else if (b == '\r') {
            if (lineBuffer.size() > 0) {
                lineBuffer.write('\r');
                buffer.append(lineBuffer.toString(StandardCharsets.UTF_8));
                lineBuffer.reset();
            }
        } else {
            lineBuffer.write(b);
        }
    }

    @Override
    public void flush() throws IOException {
        originalStream.flush();
    }

    @Override
    public void close() throws IOException {
        originalStream.close();
    }
}