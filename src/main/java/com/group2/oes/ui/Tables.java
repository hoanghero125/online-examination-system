package com.group2.oes.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

import java.util.function.Function;

/** Rút gọn việc khai báo cột bảng và ô chọn, để controller đọc dễ hơn. */
public final class Tables {

    private Tables() {
    }

    /** Cột hiển thị một giá trị tính từ dòng; sắp xếp theo giá trị đó (số thì sắp theo số). */
    public static <T, V> TableColumn<T, V> column(String title, double prefWidth, Function<T, V> value) {
        TableColumn<T, V> c = new TableColumn<>(title);
        c.setPrefWidth(prefWidth);
        c.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return c;
    }

    /** Ô chọn hiện tên tiếng Việt thay cho tên hằng; giá trị null hiện thành {@code nullText} (ví dụ "Tất cả"). */
    public static <T> void labels(ComboBox<T> box, Function<T, String> name, String nullText) {
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? nullText : name.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;   // ô chọn không cho gõ tay
            }
        });
    }
}
