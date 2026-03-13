package com.example.military.server;

import com.example.military.model.*;
import com.example.military.shared.Protocol;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection connection;
    private final String dbFileName;

    public DatabaseManager() {
        this.dbFileName = Protocol.DB_FILE_NAME;
        initDatabase();
    }

    public DatabaseManager(String dbFileName) {
        this.dbFileName = dbFileName;
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
            createTables();
            System.out.println("✅ База данных инициализирована: " + dbFileName);
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Драйвер SQLite не найден: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Ошибка подключения к БД: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String createPersonnelTable =
                "CREATE TABLE IF NOT EXISTS personnel (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type TEXT NOT NULL," +
                        "last_name TEXT NOT NULL," +
                        "company TEXT," +
                        "rank TEXT," +
                        "birth_date TEXT," +
                        "enlistment_date TEXT," +
                        "unit TEXT," +
                        "salary REAL," +
                        "military_district TEXT," +
                        "position TEXT," +
                        "years_of_service INTEGER," +
                        "command_allowance REAL," +
                        "contract_period TEXT," +
                        "contract_date TEXT," +
                        "protocol_number TEXT," +
                        "award_name TEXT," +
                        "prize REAL," +
                        "awarded_allowance REAL" +
                        ");";

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAuditLogTable = """
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    details TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createAuditLogTable);

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                String hash1 = BCrypt.hashpw("kD9#mP2$rT5@", BCrypt.gensalt());
                String hash2 = BCrypt.hashpw("xL7&nR4$wQ9*", BCrypt.gensalt());

                String insertUsers = "INSERT INTO users (username, password_hash, full_name) VALUES " +
                        "('user001@arm.ru', '" + hash1 + "', 'Сотрудник №1'), " +
                        "('user002@arm.ru', '" + hash2 + "', 'Сотрудник №2')";
                stmt.execute(insertUsers);
            }
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPersonnelTable);

            try {
                stmt.execute("ALTER TABLE personnel ADD COLUMN locked_by INTEGER DEFAULT NULL");
            } catch (SQLException ignored) {}

            try {
                stmt.execute("ALTER TABLE personnel ADD COLUMN locked_at TIMESTAMP DEFAULT NULL");
            } catch (SQLException ignored) {}
        }
    }

    public boolean lockRecord(int recordId, int userId) throws SQLException {
        String sql = "UPDATE personnel SET locked_by = ?, locked_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND (locked_by IS NULL OR locked_by = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, recordId);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean unlockRecord(int recordId, int userId) throws SQLException {
        String sql = "UPDATE personnel SET locked_by = NULL, locked_at = NULL WHERE id = ? AND locked_by = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, recordId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public Integer checkLockOwner(int recordId) throws SQLException {
        String sql = "SELECT locked_by FROM personnel WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, recordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int owner = rs.getInt("locked_by");
                if (owner == 0) return null;
                return owner;
            }
        }
        return null;
    }

    public User findUserById(int userId) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("password_hash")
                );
            }
        }
        return null;
    }

    public User findUserByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("password_hash")
                );
            }
        }
        return null;
    }

    public int savePerson(MilitaryPerson person) throws SQLException {
        String sql =
                "INSERT INTO personnel (" +
                        "type, last_name, company, rank, birth_date, enlistment_date, unit, salary, " +
                        "military_district, position, years_of_service, command_allowance, " +
                        "contract_period, contract_date, protocol_number, " +
                        "award_name, prize, awarded_allowance" +
                        ") VALUES (" +
                        "?, ?, ?, ?, ?, ?, ?, ?, " +
                        "?, ?, ?, ?, " +
                        "?, ?, ?, " +
                        "?, ?, ?" +
                        ")";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String type = getPersonType(person);
            pstmt.setString(1, type);
            pstmt.setString(2, person.getLastName());
            pstmt.setString(3, person.getCompany());
            pstmt.setString(4, person.getRank());
            pstmt.setString(5, person.getBirthDate() != null ? person.getBirthDate().toString() : null);
            pstmt.setString(6, person.getEnlistmentDate() != null ? person.getEnlistmentDate().toString() : null);
            pstmt.setString(7, person.getUnit());
            pstmt.setDouble(8, person.getSalary());

            if (person instanceof MilitaryCommand) {
                MilitaryCommand cmd = (MilitaryCommand) person;
                pstmt.setString(9, cmd.getMilitaryDistrict());
                pstmt.setString(10, cmd.getPosition());
                pstmt.setInt(11, cmd.getYearsOfService());
                pstmt.setDouble(12, cmd.getAllowance());
                pstmt.setString(13, null);
                pstmt.setString(14, null);
                pstmt.setString(15, null);
                pstmt.setString(16, null);
                pstmt.setDouble(17, 0);
                pstmt.setDouble(18, 0);
            } else if (person instanceof MilitaryContract) {
                MilitaryContract contract = (MilitaryContract) person;
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setNull(11, Types.INTEGER);
                pstmt.setNull(12, Types.DOUBLE);
                pstmt.setString(13, contract.getContractPeriod());
                pstmt.setString(14, contract.getContractDate() != null ? contract.getContractDate().toString() : null);
                pstmt.setString(15, contract.getProtocolNumber());
                pstmt.setString(16, null);
                pstmt.setNull(17, Types.DOUBLE);
                pstmt.setNull(18, Types.DOUBLE);
            } else if (person instanceof MilitaryAwarded) {
                MilitaryAwarded awarded = (MilitaryAwarded) person;
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setNull(11, Types.INTEGER);
                pstmt.setNull(12, Types.DOUBLE);
                pstmt.setString(13, null);
                pstmt.setString(14, null);
                pstmt.setString(15, null);
                pstmt.setString(16, awarded.getAwardName());
                pstmt.setDouble(17, awarded.getPrize());
                pstmt.setDouble(18, awarded.getAllowance());
            } else {
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setNull(11, Types.INTEGER);
                pstmt.setNull(12, Types.DOUBLE);
                pstmt.setString(13, null);
                pstmt.setString(14, null);
                pstmt.setString(15, null);
                pstmt.setString(16, null);
                pstmt.setNull(17, Types.DOUBLE);
                pstmt.setNull(18, Types.DOUBLE);
            }

            pstmt.executeUpdate();

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        }
    }

    public List<MilitaryPerson> loadAllPersons() throws SQLException {
        List<MilitaryPerson> result = new ArrayList<>();
        String sql = "SELECT * FROM personnel ORDER BY id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                MilitaryPerson person = createPersonFromResultSet(rs);
                person.setId(rs.getInt("id"));
                if (person != null) {
                    result.add(person);
                }
            }
        }
        return result;
    }

    public MilitaryPerson loadPersonById(int id) throws SQLException {
        String sql = "SELECT * FROM personnel WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                MilitaryPerson person = createPersonFromResultSet(rs);
                person.setId(rs.getInt("id"));
                return person;
            }
        }
        return null;
    }

    public boolean updatePerson(MilitaryPerson person) throws SQLException {
        String sql = "UPDATE personnel SET " +
                "type = ?, last_name = ?, company = ?, rank = ?, " +
                "birth_date = ?, enlistment_date = ?, unit = ?, salary = ?, " +
                "military_district = ?, position = ?, years_of_service = ?, command_allowance = ?, " +
                "contract_period = ?, contract_date = ?, protocol_number = ?, " +
                "award_name = ?, prize = ?, awarded_allowance = ? " +
                "WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String type = getPersonType(person);
            pstmt.setString(1, type);
            pstmt.setString(2, person.getLastName());
            pstmt.setString(3, person.getCompany());
            pstmt.setString(4, person.getRank());
            pstmt.setString(5, person.getBirthDate() != null ? person.getBirthDate().toString() : null);
            pstmt.setString(6, person.getEnlistmentDate() != null ? person.getEnlistmentDate().toString() : null);
            pstmt.setString(7, person.getUnit());
            pstmt.setDouble(8, person.getSalary());

            if (person instanceof MilitaryCommand) {
                MilitaryCommand cmd = (MilitaryCommand) person;
                pstmt.setString(9, cmd.getMilitaryDistrict());
                pstmt.setString(10, cmd.getPosition());
                pstmt.setInt(11, cmd.getYearsOfService());
                pstmt.setDouble(12, cmd.getAllowance());
            } else {
                pstmt.setNull(9, Types.VARCHAR);
                pstmt.setNull(10, Types.VARCHAR);
                pstmt.setNull(11, Types.INTEGER);
                pstmt.setNull(12, Types.DOUBLE);
            }

            if (person instanceof MilitaryContract) {
                MilitaryContract contract = (MilitaryContract) person;
                pstmt.setString(13, contract.getContractPeriod());
                pstmt.setString(14, contract.getContractDate() != null ? contract.getContractDate().toString() : null);
                pstmt.setString(15, contract.getProtocolNumber());
            } else {
                pstmt.setNull(13, Types.VARCHAR);
                pstmt.setNull(14, Types.VARCHAR);
                pstmt.setNull(15, Types.VARCHAR);
            }

            if (person instanceof MilitaryAwarded) {
                MilitaryAwarded awarded = (MilitaryAwarded) person;
                pstmt.setString(16, awarded.getAwardName());
                pstmt.setDouble(17, awarded.getPrize());
                pstmt.setDouble(18, awarded.getAllowance());
            } else {
                pstmt.setNull(16, Types.VARCHAR);
                pstmt.setNull(17, Types.DOUBLE);
                pstmt.setNull(18, Types.DOUBLE);
            }

            pstmt.setInt(19, person.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deletePerson(int id) throws SQLException {
        String sql = "DELETE FROM personnel WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public int getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM personnel";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }

    public void clearAll() throws SQLException {
        String sql = "DELETE FROM personnel";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private MilitaryPerson createPersonFromResultSet(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        String lastName = rs.getString("last_name");
        String company = rs.getString("company");
        String rank = rs.getString("rank");
        LocalDate birthDate = parseDate(rs.getString("birth_date"));
        LocalDate enlistmentDate = parseDate(rs.getString("enlistment_date"));
        String unit = rs.getString("unit");
        double salary = rs.getDouble("salary");

        switch (type) {
            case "COMMAND":
                return new MilitaryCommand(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        rs.getString("military_district"),
                        rs.getString("position"),
                        rs.getInt("years_of_service"),
                        rs.getDouble("command_allowance")
                );
            case "CONTRACT":
                return new MilitaryContract(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        rs.getString("contract_period"),
                        parseDate(rs.getString("contract_date")),
                        rs.getString("protocol_number")
                );
            case "AWARDED":
                return new MilitaryAwarded(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        rs.getString("award_name"),
                        rs.getDouble("prize"),
                        rs.getDouble("awarded_allowance")
                );
            case "BASE":
            default:
                return new MilitaryPerson(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary
                );
        }
    }

    private String getPersonType(MilitaryPerson person) {
        if (person instanceof MilitaryCommand) return "COMMAND";
        if (person instanceof MilitaryContract) return "CONTRACT";
        if (person instanceof MilitaryAwarded) return "AWARDED";
        return "BASE";
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("📦 Соединение с БД закрыто");
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при закрытии БД: " + e.getMessage());
        }
    }
}