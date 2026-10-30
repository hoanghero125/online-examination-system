package com.group2.oes.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Tách một tệp .sql thành từng câu lệnh để chạy qua JDBC.
 *
 * JDBC chỉ chạy được một câu mỗi lần và không hiểu lệnh DELIMITER (lệnh riêng của trình mysql dòng lệnh, dùng khi
 * viết trigger/thủ tục có dấu ";" bên trong). Lớp này làm đúng việc trình mysql làm: đọc tới dấu kết thúc câu đang
 * dùng, đổi dấu kết thúc khi gặp dòng DELIMITER, và không cắt nhầm ở dấu ";" nằm trong chuỗi hoặc chú thích.
 */
public final class SqlScript {

    private SqlScript() {
    }

    public static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String delimiter = ";";
        boolean inBlockComment = false;

        for (String line : script.split("\\R", -1)) {
            String trimmed = line.strip();
            if (!inBlockComment && current.toString().isBlank() && trimmed.toUpperCase().startsWith("DELIMITER ")) {
                delimiter = trimmed.substring("DELIMITER ".length()).strip();
                continue;
            }
            char quote = 0;   // ' " hoặc ` khi đang ở trong chuỗi/tên
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (inBlockComment) {
                    if (line.startsWith("*/", i)) {
                        inBlockComment = false;
                        i++;
                    }
                    continue;
                }
                if (quote != 0) {
                    current.append(ch);
                    if (ch == '\\' && i + 1 < line.length()) {
                        current.append(line.charAt(++i));   // ký tự được thoát, ví dụ \'
                    } else if (ch == quote) {
                        quote = 0;
                    }
                    continue;
                }
                if (ch == '\'' || ch == '"' || ch == '`') {
                    quote = ch;
                    current.append(ch);
                } else if (line.startsWith("--", i) && (i + 2 == line.length() || Character.isWhitespace(line.charAt(i + 2)))
                        || ch == '#') {
                    break;   // chú thích tới hết dòng: bỏ, kể cả dấu ";" bên trong
                } else if (line.startsWith("/*", i)) {
                    inBlockComment = true;
                    i++;
                } else if (line.startsWith(delimiter, i)) {
                    addIfNotBlank(statements, current);
                    i += delimiter.length() - 1;
                } else {
                    current.append(ch);
                }
            }
            current.append('\n');
        }
        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder current) {
        String sql = current.toString().strip();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
        current.setLength(0);
    }
}
