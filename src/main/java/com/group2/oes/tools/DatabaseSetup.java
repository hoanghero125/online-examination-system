package com.group2.oes.tools;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.LoggingSetup;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.ExamAppException;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dựng CSDL cho máy cá nhân hoặc máy demo bằng một lệnh (scripts/setup.sh và scripts/setup.bat gọi lớp này):
 * <ol>
 *   <li>nối MySQL bằng tài khoản root;</li>
 *   <li>tạo CSDL (mặc định oes), chạy db/schema.sql rồi db/security.sql;</li>
 *   <li>đặt mật khẩu cho oes_staff, oes_client khớp với tệp cấu hình sắp ghi;</li>
 *   <li>ghi db-staff.properties, db-client.properties;</li>
 *   <li>tạo dữ liệu mẫu bằng SeedTool (bỏ qua khi có --no-seed).</li>
 * </ol>
 * Chạy SQL qua JDBC nên máy không cần cài trình mysql dòng lệnh (trên Windows thường không có trong PATH).
 */
public final class DatabaseSetup {

    static final String DEFAULT_STAFF_PASSWORD = "doi_mat_khau_staff";
    static final String DEFAULT_CLIENT_PASSWORD = "doi_mat_khau_client";

    // Một bộ đọc dùng chung: tạo bộ đọc mới mỗi lần sẽ làm mất các dòng đã nằm sẵn trong bộ đệm của bộ đọc cũ.
    private static final BufferedReader STDIN = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private static final Set<String> FLAGS = Set.of("reset", "no-seed", "yes");
    private static final Set<String> OPTIONS = Set.of("host", "port", "root-user", "root-password", "database",
            "staff-password", "client-password", "staff-config", "client-config", "sql-dir");

    /** Các lựa chọn dòng lệnh, đã gán giá trị mặc định. */
    record Options(String host, int port, String rootUser, String rootPassword, String database,
                   String staffPassword, String clientPassword, Path staffConfig, Path clientConfig, Path sqlDir,
                   boolean reset, boolean seed, boolean assumeYes) {

        static Options parse(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String arg : args) {
                if (!arg.startsWith("--")) {
                    throw new IllegalArgumentException("Tham số không hợp lệ: " + arg);
                }
                String body = arg.substring(2);
                int eq = body.indexOf('=');
                String key = eq < 0 ? body : body.substring(0, eq);
                if (eq < 0 ? !FLAGS.contains(key) : !OPTIONS.contains(key)) {
                    throw new IllegalArgumentException("Không có tùy chọn " + arg);
                }
                values.put(key, eq < 0 ? "true" : body.substring(eq + 1));
            }
            String database = values.getOrDefault("database", "oes");
            // Tên CSDL phải nối thẳng vào câu CREATE DATABASE (tên không truyền qua tham số ? được),
            // nên chỉ nhận chữ, số, gạch dưới để không thể chèn thêm lệnh SQL.
            if (!database.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("Tên CSDL chỉ gồm chữ, số và dấu gạch dưới: " + database);
            }
            int port;
            try {
                port = Integer.parseInt(values.getOrDefault("port", "3306"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cổng phải là số: " + values.get("port"));
            }
            return new Options(values.getOrDefault("host", "localhost"), port,
                    values.getOrDefault("root-user", "root"), values.get("root-password"), database,
                    values.getOrDefault("staff-password", DEFAULT_STAFF_PASSWORD),
                    values.getOrDefault("client-password", DEFAULT_CLIENT_PASSWORD),
                    Path.of(values.getOrDefault("staff-config", "db-staff.properties")),
                    Path.of(values.getOrDefault("client-config", "db-client.properties")),
                    Path.of(values.getOrDefault("sql-dir", "db")),
                    values.containsKey("reset"), !values.containsKey("no-seed"), values.containsKey("yes"));
        }

        String serverUrl() {
            // allowPublicKeyRetrieval: MySQL 8 không bật TLS (ví dụ cài từ gói zip) vẫn đăng nhập được bằng mật khẩu
            return "jdbc:mysql://" + host + ":" + port + "/?allowPublicKeyRetrieval=true&connectTimeout=5000";
        }

        String databaseUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true";
        }
    }

    private DatabaseSetup() {
    }

    public static void main(String[] args) {
        LoggingSetup.configure();
        if (List.of(args).contains("--help")) {
            printUsage();
            return;
        }
        Options o;
        try {
            o = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
            return;
        }
        try {
            run(o);
        } catch (SQLException e) {
            System.err.println();
            System.err.println("Lỗi MySQL: " + e.getMessage());
            System.err.println(hint(e));
            System.exit(1);
        } catch (ExamAppException e) {
            System.err.println(e.getUserMessage() + " (" + e.getMessage() + ")");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Lỗi đọc/ghi tệp: " + e.getMessage());
            System.exit(1);
        }
    }

    static void run(Options o) throws SQLException, IOException {
        String rootPassword = o.rootPassword() != null ? o.rootPassword() : askRootPassword(o);
        List<String> schema = SqlScript.split(Files.readString(o.sqlDir().resolve("schema.sql")));
        List<String> security = SqlScript.split(Files.readString(o.sqlDir().resolve("security.sql")));

        step("Nối MySQL tại " + o.host() + ":" + o.port() + " bằng tài khoản " + o.rootUser());
        try (Connection root = DriverManager.getConnection(o.serverUrl(), o.rootUser(), rootPassword)) {
            if (databaseExists(root, o.database())) {
                if (!o.reset() && !confirm(o, "CSDL '" + o.database() + "' đã có. Xóa và tạo lại từ đầu (mất hết dữ liệu)?")) {
                    System.out.println("Giữ nguyên CSDL cũ, không làm gì thêm. Muốn tạo lại: thêm --reset.");
                    return;
                }
                step("Xóa CSDL cũ " + o.database());
                execute(root, "DROP DATABASE `" + o.database() + "`");
            }
            step("Tạo CSDL " + o.database());
            // Không ghi COLLATE: CSDL, các bảng (DEFAULT CHARSET=utf8mb4) và tham số của thủ tục lưu trữ phải cùng
            // collation mặc định của MySQL, nếu không phép so sánh chuỗi trong sp_student_change_password báo lỗi.
            execute(root, "CREATE DATABASE `" + o.database() + "` CHARACTER SET utf8mb4");
        }

        try (Connection db = DriverManager.getConnection(o.databaseUrl(), o.rootUser(), rootPassword)) {
            step("Chạy " + o.sqlDir().resolve("schema.sql") + " (" + schema.size() + " câu lệnh)");
            for (String sql : schema) {
                execute(db, sql);
            }
            step("Chạy " + o.sqlDir().resolve("security.sql") + " (" + security.size() + " câu lệnh)");
            for (String sql : security) {
                execute(db, sql);
            }
            // security.sql tạo tài khoản bằng mật khẩu mẫu và không đổi mật khẩu của tài khoản đã có;
            // đặt lại ở đây để chắc chắn khớp với tệp cấu hình ghi ở bước sau.
            step("Đặt mật khẩu cho oes_staff, oes_client");
            setPassword(db, "oes_staff", o.staffPassword());
            setPassword(db, "oes_client", o.clientPassword());
        }

        step("Ghi " + o.staffConfig().toAbsolutePath());
        writeConfig(o.staffConfig(), "StaffApp, GradingDaemon, SeedTool", o.databaseUrl(), "oes_staff", o.staffPassword());
        step("Ghi " + o.clientConfig().toAbsolutePath());
        writeConfig(o.clientConfig(), "ExamClientApp", o.databaseUrl(), "oes_client", o.clientPassword());

        if (o.seed()) {
            step("Tạo dữ liệu mẫu");
            SeedTool.seed(AppContext.jdbc(ConnectionManager.fromFile(o.staffConfig())));
        }
        System.out.println();
        System.out.println("Đã dựng xong CSDL '" + o.database() + "'.");
    }

    private static boolean databaseExists(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Câu lệnh lấy từ tệp .sql của dự án hoặc đã kiểm tra tên CSDL ở trên – không chứa giá trị người dùng nhập. */
    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static void setPassword(Connection c, String user, String password) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("ALTER USER '" + user + "'@'%' IDENTIFIED BY ?")) {
            ps.setString(1, password);
            ps.execute();
        }
    }

    /** Ghi tệp cấu hình theo đúng định dạng java.util.Properties nhưng giữ dễ đọc để người dùng tự sửa được. */
    static void writeConfig(Path file, String usedBy, String url, String user, String password) throws IOException {
        String content = "# Cấu hình kết nối CSDL cho " + usedBy + ", tạo bởi scripts/setup.\n"
                + "# Đổi máy chủ: sửa địa chỉ trong db.url. Tệp này chứa mật khẩu, không đưa lên Git.\n"
                + "db.url=" + url + "\n"
                + "db.user=" + user + "\n"
                + "db.password=" + password.replace("\\", "\\\\") + "\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String askRootPassword(Options o) {
        String envPassword = System.getenv("OES_ROOT_PASSWORD");
        if (envPassword != null) {
            return envPassword;
        }
        String prompt = "Mật khẩu MySQL của " + o.rootUser() + " (để trống nếu không có): ";
        Console console = System.console();
        if (console != null) {
            char[] typed = console.readPassword(prompt);
            return typed == null ? "" : new String(typed);
        }
        System.out.print(prompt);
        return readLine();
    }

    private static boolean confirm(Options o, String question) {
        if (o.assumeYes()) {
            return true;
        }
        System.out.print(question + " [c/K]: ");
        String answer = readLine().strip().toLowerCase();
        return answer.equals("c") || answer.equals("co") || answer.equals("có") || answer.equals("y") || answer.equals("yes");
    }

    private static String readLine() {
        try {
            String line = STDIN.readLine();
            return line == null ? "" : line;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void step(String text) {
        System.out.println("- " + text);
    }

    /** Gợi ý cách sửa cho các lỗi hay gặp khi dựng CSDL lần đầu. */
    static String hint(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            return "MySQL chưa chạy hoặc sai địa chỉ/cổng. Windows: services.msc -> MySQL80 -> Start. "
                    + "Linux: sudo systemctl start mysql. Hoặc dùng scripts/setup-docker để chạy MySQL trong Docker.";
        }
        if (e.getErrorCode() == 1045) {
            return "Sai mật khẩu tài khoản root.";
        }
        if (e.getErrorCode() == 1819) {
            return "MySQL đang bật chính sách mật khẩu mạnh: chạy lại với --staff-password=... --client-password=... "
                    + "(mật khẩu dài, có chữ hoa, chữ thường, số, ký tự đặc biệt).";
        }
        return "Xem docs/SETUP.md, mục Lỗi thường gặp.";
    }

    private static void printUsage() {
        System.err.println("""
                Cách dùng: DatabaseSetup [tùy chọn]
                  --host=localhost          địa chỉ MySQL
                  --port=3306               cổng MySQL
                  --root-user=root          tài khoản quản trị MySQL
                  --root-password=...       mật khẩu (không ghi thì hỏi, hoặc lấy từ biến môi trường OES_ROOT_PASSWORD)
                  --database=oes            tên CSDL
                  --staff-password=...      mật khẩu đặt cho oes_staff (mặc định doi_mat_khau_staff)
                  --client-password=...     mật khẩu đặt cho oes_client (mặc định doi_mat_khau_client)
                  --staff-config=db-staff.properties    nơi ghi cấu hình ứng dụng quản lý
                  --client-config=db-client.properties  nơi ghi cấu hình ứng dụng thi
                  --sql-dir=db              thư mục chứa schema.sql, security.sql
                  --reset                   CSDL đã có thì xóa và tạo lại, không hỏi
                  --no-seed                 không tạo dữ liệu mẫu
                  --yes                     trả lời "có" cho mọi câu hỏi""");
    }
}
