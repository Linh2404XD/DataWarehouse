import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class DatabaseHandler {
    private Connection connection;

    public DatabaseHandler(String url, String username, String password) throws SQLException {
        // 2. Kết nối database
        connection = DriverManager.getConnection(url, username, password);
    }


    // 4. Thực hiện insert vào staging
    public void insertOrUpdateStaging(String id, String name, String director, String actor, String limitAge,
                                      String country, String brief, String image,
                                      String releaseDate, String endDate, int duration) throws SQLException {
        // Loại bỏ phần tên ngày trong tuần nếu có
        String cleanedReleaseDate = releaseDate.replaceAll("^.*?\\s*,\\s*", "").trim();
        String cleanedEndDate = endDate.replaceAll("^.*?\\s*,\\s*", "").trim();

        // Chuyển đổi định dạng ngày tháng
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

        String formattedReleaseDate = null;
        String formattedEndDate = null;

        try {
            if (!cleanedReleaseDate.isEmpty()) {
                Date parsedReleaseDate = inputFormat.parse(cleanedReleaseDate);
                formattedReleaseDate = outputFormat.format(parsedReleaseDate);
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi chuyển đổi releaseDate: " + cleanedReleaseDate);
        }

        try {
            if (!cleanedEndDate.isEmpty()) {
                Date parsedEndDate = inputFormat.parse(cleanedEndDate);
                formattedEndDate = outputFormat.format(parsedEndDate);
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi chuyển đổi endDate: " + cleanedEndDate);
        }

        // Gọi stored procedure
        String sql = "CALL insert_or_update_phimchieurapStaging(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, director);
            stmt.setString(4, actor);
            stmt.setString(5, limitAge);
            stmt.setString(6, country);
            stmt.setString(7, brief);
            stmt.setString(8, image);
            stmt.setString(9, formattedReleaseDate);
            stmt.setString(10, formattedEndDate);
            stmt.setInt(11, duration);
            stmt.executeUpdate();
        }
    }

    // Bước 4: Lưu dữ liệu từ file CSV vào staging
    public boolean saveToCSV(List<Movie> movies, String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // Ghi tiêu đề cột
            String[] header = {"ID", "Name", "Director", "Actor", "LimitAge", "Country", "Brief", "Image", "ReleaseDate", "EndDate", "Duration"};
            writer.writeNext(header);

            // Ghi dữ liệu các bộ phim
            for (Movie movie : movies) {
                String releaseDateFormatted = movie.getReleaseDate();
                String endDateFormatted = movie.getEndDate();
                String[] row = {
                        movie.getId(),
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

    // 3. Đọc file csv
    public static List<Movie> loadFromCSV(String filePath) {
        List<Movie> movies = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            reader.readNext(); // Bỏ qua dòng tiêu đề

            while ((line = reader.readNext()) != null) {
                System.out.println("Đọc dòng: " + String.join(",", line));
                if (line.length < 11) {
                    System.err.println("Dòng không đủ dữ liệu: " + String.join(",", line));
                    continue;
                }

                try {
                    Movie movie = new Movie();
                    movie.setId(line[0].trim());
                    movie.setName(line[1].trim());
                    System.out.println("Tên phim: " + movie.getName());
                    movie.setDirector(line[2].trim());
                    movie.setActor(line[3].trim());
                    movie.setLimitAge(line[4].trim());
                    movie.setCountry(line[5].trim());
                    movie.setBrief(line[6].trim());
                    movie.setImage(line[7].trim());
                    movie.setReleaseDate(line[8].trim());
                    movie.setEndDate(line[9].trim());

                    try {
                        movie.setDuration(Integer.parseInt(line[10].trim()));
                    } catch (NumberFormatException e) {
                        System.err.println("Lỗi chuyển đổi 'Duration' thành số: " + line[10]);
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



