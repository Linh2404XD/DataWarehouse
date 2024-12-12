import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;

public class DatabaseHandler {
    private Connection connection;

    public DatabaseHandler(String url, String username, String password) throws SQLException {
        // 2. Kết nối database
        connection = DriverManager.getConnection(url, username, password);
    }

    // Bước 3: Gọi stored procedure để lưu dữ liệu phim vào bảng staging
    public void insertOrUpdateStaging(String name, String director, String actor, String limitAge,
                                      String country, String brief, String image,
                                      String releaseDate, String endDate, int duration) throws SQLException {
        // Loại bỏ phần tên ngày trong tuần nếu có (ví dụ: "Thứ Hai, ", "Thứ Bảy, ").
        String cleanedReleaseDate = releaseDate.replaceAll("^.*?\\s*,\\s*", "").trim();  // Loại bỏ tên ngày như "Thứ Hai, "
        String cleanedEndDate = endDate.replaceAll("^.*?\\s*,\\s*", "").trim();  // Loại bỏ tên ngày nếu có

        // Chuyển đổi releaseDate và endDate sang định dạng chuẩn (yyyy-MM-dd)
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

        String formattedReleaseDate = null;
        String formattedEndDate = null;

        // Chuyển đổi releaseDate
        try {
            if (cleanedReleaseDate != null && !cleanedReleaseDate.isEmpty()) {
                Date parsedReleaseDate = inputFormat.parse(cleanedReleaseDate); // Đảm bảo cleanedReleaseDate có định dạng đúng
                formattedReleaseDate = outputFormat.format(parsedReleaseDate);
            }
        } catch (Exception e) {
            // Log lỗi nếu không thể chuyển đổi releaseDate
            System.out.println("Lỗi khi chuyển đổi releaseDate: " + cleanedReleaseDate);
            formattedReleaseDate = null; // Hoặc có thể gán giá trị mặc định
        }

        // Chuyển đổi endDate
        try {
            if (cleanedEndDate != null && !cleanedEndDate.isEmpty()) {
                Date parsedEndDate = inputFormat.parse(cleanedEndDate); // Đảm bảo cleanedEndDate có định dạng đúng
                formattedEndDate = outputFormat.format(parsedEndDate);
            }
        } catch (Exception e) {
            // Log lỗi nếu không thể chuyển đổi endDate
            System.out.println("Lỗi khi chuyển đổi endDate: " + cleanedEndDate);
            formattedEndDate = null; // Hoặc bạn có thể gán giá trị mặc định
        }

        // In nội dung trước khi kiểm tra và chèn
        System.out.println("Chèn dữ liệu: " + name + ", " + director + ", " + actor + ", " + limitAge + ", " + country + ", " + brief + ", " + image + ", " + formattedReleaseDate + ", " + formattedEndDate + ", " + duration);

        // Kiểm tra xem dữ liệu đã tồn tại hay chưa
        String checkSql = "SELECT phimchieurap_staging.name FROM phimchieurap_staging WHERE name = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                // Nếu đã tồn tại, cập nhật ngày updated_at
                System.out.println("Dữ liệu đã tồn tại: " + name);
                String updateSql = "UPDATE phimchieurap_staging SET updated_at = NOW() WHERE name = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setString(1, name);
                    updateStmt.executeUpdate();
                }
            } else {
                // Nếu chưa tồn tại, thực hiện insert
                System.out.println("Chèn mới dữ liệu: " + name);
                String insertSql = "CALL insert_or_update_phimchieurapStaging(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, name);
                    insertStmt.setString(2, director);
                    insertStmt.setString(3, actor);
                    insertStmt.setString(4, limitAge);
                    insertStmt.setString(5, country);
                    insertStmt.setString(6, brief);
                    insertStmt.setString(7, image);
                    insertStmt.setString(8, formattedReleaseDate);
                    insertStmt.setString(9, formattedEndDate);
                    insertStmt.setInt(10, duration);
                    insertStmt.executeUpdate();
                }
            }
        }
    }


    // Bước 4: Lưu dữ liệu từ file CSV vào staging
    public boolean saveToCSV(List<Movie> movies, String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // Ghi tiêu đề cột
            String[] header = {"Name", "Director", "Actor", "LimitAge", "Country", "Brief", "Image", "ReleaseDate", "EndDate", "Duration"};
            writer.writeNext(header);

            // Ghi dữ liệu các bộ phim
            for (Movie movie : movies) {
                String releaseDateFormatted = movie.getReleaseDate();
                String endDateFormatted = movie.getEndDate();
                String[] row = {
                        movie.getName(),
                        movie.getDirector(),
                        movie.getActor(),
                        movie.getLimitAge(),
                        movie.getCountry(),
                        movie.getBrief(),
                        movie.getImage(),
                        releaseDateFormatted,
                        endDateFormatted,
                        String.valueOf(movie.getDuration())
                };
                writer.writeNext(row);
            }

            return true; // Thêm dữ liệu thành công

        } catch (IOException e) {
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

    public void logSuccess(String message) throws SQLException {
        String sql = "INSERT INTO log (status, date_create) VALUES (?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, message);
            stmt.executeUpdate();
        }
    }
    public static List<Movie> loadFromCSV(String filePath) {
        List<Movie> movies = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            reader.readNext(); // Bỏ qua dòng tiêu đề

            while ((line = reader.readNext()) != null) {
                System.out.println("Đọc dòng: " + String.join(",", line));
                if (line.length < 10) {
                    System.err.println("Dòng không đủ dữ liệu: " + String.join(",", line));
                    continue;
                }

                try {
                    Movie movie = new Movie();
                    movie.setName(line[0].trim());
                    System.out.println("Tên phim: " + movie.getName());
                    movie.setDirector(line[1].trim());
                    movie.setActor(line[2].trim());
                    movie.setLimitAge(line[3].trim());
                    movie.setCountry(line[4].trim());
                    movie.setBrief(line[5].trim());
                    movie.setImage(line[6].trim());
                    movie.setReleaseDate(line[7].trim());
                    movie.setEndDate(line[8].trim());

                    try {
                        movie.setDuration(Integer.parseInt(line[9].trim()));
                    } catch (NumberFormatException e) {
                        System.err.println("Lỗi chuyển đổi 'Duration' thành số: " + line[9]);
                        movie.setDuration(0); // Giá trị mặc định nếu không thể chuyển đổi
                    }

                    movies.add(movie);
                    System.out.println("Phim đã thêm: " + movie.getName());

                } catch (Exception ex) {
                    System.err.println("Lỗi khi xử lý dòng: " + String.join(",", line));
                    ex.printStackTrace();
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Lỗi khi đọc file CSV: " + filePath);
            e.printStackTrace();
        }

        System.out.println("Tổng số phim đã tải: " + movies.size());
        return movies;
    }

    public static void main(String[] args) {
        loadFromCSV("cinestar.csv");
    }
}



