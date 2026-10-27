package com.group2.oes.ui.controller;

import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Exam;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mở ca thi (FR-17, 18, 19): giờ mở/đóng, ba tùy chọn, ghi danh theo lớp hoặc chọn từng người.
 * Ghép ngày giờ và kiểm tra hợp lệ nằm ở Formats.combine, SessionWindow và ExamSessionService.
 */
public class SessionFormController {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label examLabel;
    @FXML
    private DatePicker openDate;
    @FXML
    private TextField openTime;
    @FXML
    private DatePicker closeDate;
    @FXML
    private TextField closeTime;
    @FXML
    private CheckBox shuffleQuestionsBox;
    @FXML
    private CheckBox shuffleOptionsBox;
    @FXML
    private CheckBox showAnswerBox;
    @FXML
    private ComboBox<CourseClass> classBox;
    @FXML
    private ListView<Student> studentList;
    @FXML
    private Label selectedLabel;
    @FXML
    private VBox page;

    private StaffWorkspace ws;
    private Exam exam;
    // thí sinh -> đã đánh dấu chưa; LinkedHashMap để giữ thứ tự theo mã sinh viên
    private final Map<Student, BooleanProperty> chosen = new LinkedHashMap<>();

    public void init(StaffWorkspace ws, Exam exam) {
        this.ws = ws;
        this.exam = exam;
        examLabel.setText(exam.getTitle() + " (phiên bản " + exam.getVersion() + ", " + exam.getItems().size() + " câu, "
                + Formats.score(exam.getTotalPoint()) + " điểm)");
        Tables.labels(classBox, c -> c.getCode() + (c.getSemester() == null ? "" : " – " + c.getSemester()),
                "Mọi thí sinh (chọn từng người)");
        studentList.setCellFactory(CheckBoxListCell.forListView(this::checkedOf, new StringConverter<>() {
            @Override
            public String toString(Student s) {
                return s.getStudentCode() + "  –  " + s.getFullName();
            }

            @Override
            public Student fromString(String s) {
                return null;
            }
        }));
        classBox.valueProperty().addListener((o, a, c) -> loadStudents(c));

        BackgroundTasks.run(page, () -> ws.app().sessionService.serverTime(), now -> {
            // mặc định: mở sau 5 phút, làm tròn tới 5 phút, kéo dài 60 phút
            LocalDateTime open = now.plusMinutes(5).withSecond(0).withNano(0);
            open = open.withMinute(open.getMinute() / 5 * 5);
            openDate.setValue(open.toLocalDate());
            openTime.setText(open.format(HH_MM));
            closeDate.setValue(open.plusMinutes(60).toLocalDate());
            closeTime.setText(open.plusMinutes(60).format(HH_MM));
        });
        BackgroundTasks.run(page, () -> ws.app().subjectService.listClasses(ws.user(), exam.getSubject()), classes -> {
            classBox.getItems().add(null);
            classBox.getItems().addAll(classes);
            classBox.setValue(classes.isEmpty() ? null : classes.get(0));
            if (classes.isEmpty()) {
                loadStudents(null);
            }
        });
    }

    private BooleanProperty checkedOf(Student s) {
        return chosen.get(s);
    }

    /** Theo lớp thì đánh dấu sẵn cả lớp; "mọi thí sinh" thì để trống cho giảng viên chọn từng người. */
    private void loadStudents(CourseClass cls) {
        BackgroundTasks.run(page, () -> cls == null
                ? ws.app().sessionService.allStudents(ws.user())
                : ws.app().sessionService.studentsOfClass(ws.user(), cls), students -> showStudents(students, cls != null));
    }

    private void showStudents(List<Student> students, boolean preselect) {
        chosen.clear();
        for (Student s : students) {
            BooleanProperty p = new SimpleBooleanProperty(preselect);
            p.addListener((o, a, b) -> updateCount());
            chosen.put(s, p);
        }
        studentList.getItems().setAll(students);
        updateCount();
    }

    private void updateCount() {
        long n = chosen.values().stream().filter(BooleanProperty::get).count();
        selectedLabel.setText("Đã chọn " + n + " / " + chosen.size() + " thí sinh");
    }

    @FXML
    private void onSelectAll() {
        chosen.values().forEach(p -> p.set(true));
    }

    @FXML
    private void onSelectNone() {
        chosen.values().forEach(p -> p.set(false));
    }

    @FXML
    private void onCreate() {
        SessionWindow window = new SessionWindow(Formats.combine(openDate.getValue(), openTime.getText(), "Giờ mở"),
                Formats.combine(closeDate.getValue(), closeTime.getText(), "Giờ đóng"));
        SessionPolicy policy = new SessionPolicy(shuffleQuestionsBox.isSelected(), shuffleOptionsBox.isSelected(),
                showAnswerBox.isSelected());
        List<Student> students = chosen.entrySet().stream().filter(e -> e.getValue().get()).map(Map.Entry::getKey).toList();
        BackgroundTasks.run(page, () -> ws.app().sessionService.createSession(ws.user(), exam, window, policy, students), s -> {
            ErrorDialogs.info("Mở ca thi", "Đã mở ca thi #" + s.getSessionId() + " cho " + students.size()
                    + " thí sinh, từ " + Formats.dateTime(window.openTime()) + " đến " + Formats.dateTime(window.closeTime())
                    + ". Đề \"" + exam.getTitle() + "\" đã bị khóa, muốn sửa phải tạo phiên bản mới.");
            ws.open("staff-sessions.fxml", (StaffSessionsController c) -> c.init(ws));
        });
    }

    @FXML
    private void onBack() {
        ws.open("exam-list.fxml", (ExamListController c) -> c.init(ws));
    }
}
