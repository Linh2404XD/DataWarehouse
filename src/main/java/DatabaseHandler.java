import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.sql.*;

public class DatabaseHandler {
    private Connection connection;

    public DatabaseHandler(String url, String username, String password) throws SQLException {
        // Kết nối tới cơ sở dữ liệu
        connection = DriverManager.getConnection(url, username, password);
    }

    public void insertOrUpdateStaging(String name, String director, String actor, String limitAge,
                                      String country, String brief, String image,
                                      String releaseDate, String endDate, int duration) throws SQLException {
        // Bước 3: Gọi stored procedure để lưu dữ liệu phim vào bảng staging
        String sql = "CALL insert_or_update_phimchieurapStaging(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, director);
            stmt.setString(3, actor);
            stmt.setString(4, limitAge);
            stmt.setString(5, country);
            stmt.setString(6, brief);
            stmt.setString(7, image);
            stmt.setString(8, releaseDate);
            stmt.setString(9, endDate);
            stmt.setInt(10, duration);
            stmt.executeUpdate();
        }
    }

    public boolean saveToCSV(String filePath) {
        // Bước 4: Lưu dữ liệu từ staging vào file CSV
        String sql = "SELECT * FROM phimchieurap_staging";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                writer.writeAll(rs, true);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void logError(String message) throws SQLException {
        // Ghi log lỗi vào bảng log
        String sql = "INSERT INTO log (status, date_create) VALUES (?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, message);
            stmt.executeUpdate();
        }
    }
}
