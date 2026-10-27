package com.group2.oes.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/** Nạp logging.properties trong classpath để nhật ký ghi ra tệp oes-audit.log thay vì lẫn vào màn hình. */
public final class LoggingSetup {

    private LoggingSetup() {
    }

    public static void configure() {
        try (InputStream in = LoggingSetup.class.getResourceAsStream("/logging.properties")) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
            }
        } catch (IOException e) {
            System.err.println("Không đọc được cấu hình nhật ký, dùng mặc định: " + e.getMessage());
        }
    }
}
