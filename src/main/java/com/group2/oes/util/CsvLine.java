package com.group2.oes.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tách một dòng CSV thành các trường, theo cùng quy ước ResultStatistics dùng khi xuất bảng điểm:
 * trường có dấu phẩy hoặc dấu nháy kép được bọc trong "...", dấu nháy kép bên trong viết thành "".
 * Đủ cho tệp danh sách sinh viên xuất từ Excel; không xử lý trường kéo dài nhiều dòng.
 */
public final class CsvLine {

    private CsvLine() {
    }

    public static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');   // "" bên trong trường có nháy là một dấu nháy
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                fields.add(cur.toString().strip());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString().strip());
        return fields;
    }
}
