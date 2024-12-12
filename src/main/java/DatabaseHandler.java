import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
public class DatabaseHandler {
    private Connection connection;

    public DatabaseHandler(String url, String username, String password) throws SQLException {
        // Kết nối tới cơ sở dữ liệu
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
            formattedReleaseDate = null; // Hoặc bạn có thể gán giá trị mặc định
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
            stmt.setString(8, formattedReleaseDate);  // Dùng releaseDate đã được định dạng lại
            stmt.setString(9, formattedEndDate);      // Dùng endDate đã được định dạng lại
            stmt.setInt(10, duration);
            stmt.executeUpdate();
        }
    }
    // Bước 4: Lưu dữ liệu từ file CSV vào staging
    public boolean saveToCSV(List<Movie> movies, String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // Ghi tiêu đề cột
            String[] header = {"Name", "Director", "Actor", "LimitAge", "Country", "Brief", "Image", "ReleaseDate", "EndDate", "Duration"};
            writer.writeNext(header);

            // Ghi dữ liệu các bộ phim
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

            for (Movie movie : movies) {
                String releaseDateFormatted = movie.getReleaseDate();
                String endDateFormatted = movie.getEndDate();

                // Chuyển đổi ngày nếu có
                if (releaseDateFormatted != null && !releaseDateFormatted.isEmpty()) {
                    try {
                        Date releaseDate = (Date) inputFormat.parse(releaseDateFormatted);
                        releaseDateFormatted = outputFormat.format(releaseDate);
                    } catch (Exception e) {
                        // Log lỗi nếu không thể chuyển đổi ngày
                        System.out.println("Lỗi khi chuyển đổi releaseDate: " + releaseDateFormatted);
                    }
                }

                if (endDateFormatted != null && !endDateFormatted.isEmpty()) {
                    try {
                        Date endDate = (Date) inputFormat.parse(endDateFormatted);
                        endDateFormatted = outputFormat.format(endDate);
                    } catch (Exception e) {
                        // Log lỗi nếu không thể chuyển đổi ngày
                        System.out.println("Lỗi khi chuyển đổi endDate: " + endDateFormatted);
                    }
                }

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
}
