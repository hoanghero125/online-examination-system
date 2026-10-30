package com.group2.oes;

import com.group2.oes.tools.SqlScript;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptTest {

    @Test
    void táchTheoDấuChấmPhẩy() {
        assertEquals(List.of("CREATE TABLE a (id INT)", "INSERT INTO a VALUES (1)"),
                SqlScript.split("CREATE TABLE a (id INT);\nINSERT INTO a VALUES (1);\n"));
    }

    @Test
    void khôngCắtỞDấuChấmPhẩyTrongChuỗiHoặcChúThích() {
        List<String> parts = SqlScript.split("""
                -- chú thích; không phải câu lệnh
                INSERT INTO t VALUES ('a;b', "c;d"); # chú thích cuối dòng; vẫn bỏ
                /* khối chú thích; nhiều dòng
                   vẫn bỏ; */ SELECT 1;
                """);
        assertEquals(List.of("INSERT INTO t VALUES ('a;b', \"c;d\")", "SELECT 1"), parts);
    }

    @Test
    void đổiDấuKếtThúcKhiGặpDelimiter() {
        List<String> parts = SqlScript.split("""
                DROP TRIGGER IF EXISTS trg;
                DELIMITER //
                CREATE TRIGGER trg BEFORE INSERT ON t FOR EACH ROW
                BEGIN
                    SET NEW.x = 1;
                    SET NEW.y = 2;
                END //
                DELIMITER ;
                SELECT 1;
                """);
        assertEquals(3, parts.size());
        assertTrue(parts.get(1).startsWith("CREATE TRIGGER") && parts.get(1).endsWith("END"), "thân trigger giữ nguyên");
        assertTrue(parts.get(1).contains("SET NEW.y = 2;"), "dấu ; bên trong thân trigger không bị cắt");
    }

    @Test
    void đọcĐúngHaiTệpSqlCủaDựÁn() throws IOException {
        List<String> schema = SqlScript.split(Files.readString(Path.of("db/schema.sql")));
        assertEquals(12, schema.stream().filter(s -> s.startsWith("CREATE TABLE")).count(), "đủ 12 bảng");

        List<String> security = SqlScript.split(Files.readString(Path.of("db/security.sql")));
        assertEquals(4, security.stream().filter(s -> s.startsWith("CREATE TRIGGER")).count(), "đủ 4 trigger");
        assertEquals(1, security.stream().filter(s -> s.startsWith("CREATE PROCEDURE")).count());
        assertTrue(security.stream().filter(s -> s.startsWith("CREATE TRIGGER")).allMatch(s -> s.endsWith("END")),
                "thân trigger không bị cắt giữa chừng");
    }
}
