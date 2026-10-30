package com.group2.oes.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSetupTest {

    @Test
    void khôngGhiGìThìDùngGiáTrịMặcĐịnh() {
        DatabaseSetup.Options o = DatabaseSetup.Options.parse(new String[0]);
        assertEquals("localhost", o.host());
        assertEquals(3306, o.port());
        assertEquals("oes", o.database());
        assertEquals(DatabaseSetup.DEFAULT_STAFF_PASSWORD, o.staffPassword());
        assertTrue(o.seed());
        assertFalse(o.reset());
        assertEquals("jdbc:mysql://localhost:3306/oes?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true",
                o.databaseUrl());
    }

    @Test
    void đọcTùyChọnVàCờ() {
        DatabaseSetup.Options o = DatabaseSetup.Options.parse(new String[]{
                "--port=3307", "--database=oes_test", "--reset", "--no-seed", "--root-password="});
        assertEquals(3307, o.port());
        assertEquals("oes_test", o.database());
        assertTrue(o.reset());
        assertFalse(o.seed());
        assertEquals("", o.rootPassword(), "root không có mật khẩu");
    }

    @Test
    void tênCSDLLạHoặcTùyChọnSaiThìTừChối() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseSetup.Options.parse(new String[]{"--database=oes; DROP DATABASE mysql"}));
        assertThrows(IllegalArgumentException.class, () -> DatabaseSetup.Options.parse(new String[]{"--khong-co"}));
        assertThrows(IllegalArgumentException.class, () -> DatabaseSetup.Options.parse(new String[]{"--port=abc"}));
    }

    @Test
    void tệpCấuHìnhĐọcLạiĐúngKểCảMậtKhẩuCóKýTựĐặcBiệt(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("db-staff.properties");
        DatabaseSetup.writeConfig(file, "thử", "jdbc:mysql://localhost:3306/oes?a=1&b=2", "oes_staff", "Ab#1:=\\x");
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        }
        assertEquals("jdbc:mysql://localhost:3306/oes?a=1&b=2", p.getProperty("db.url"));
        assertEquals("oes_staff", p.getProperty("db.user"));
        assertEquals("Ab#1:=\\x", p.getProperty("db.password"));
    }
}
