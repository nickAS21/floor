package org.nickas21.smart.data.service.systemOut;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class StdOutInterceptor {

    private final RuntimeLogBuffer buffer;

    @PostConstruct
    public void hook() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(new LineOutputStream(buffer, originalOut), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LineOutputStream(buffer, originalErr), true, StandardCharsets.UTF_8));
    }
}