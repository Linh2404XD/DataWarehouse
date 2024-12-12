import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CSVToStaging {
    public static void main(String[] args) {
        try {
            // Tải cấu hình
            ConfigLoader config = new ConfigLoader("config.properties");

            // Khởi tạo database handler
            DatabaseHandler dbHandler = new DatabaseHandler(
                    config.getProperty("db.url"),
                    config.getProperty("db.username"),
                    config.getProperty("db.password")
            );

            // Tải các bộ phim từ tệp CSV
            List<Movie> movies = dbHandler.loadFromCSV(config.getProperty("source.file"));

            // Chèn dữ liệu vào bảng staging
            for (Movie movie : movies) {
                try {
                    dbHandler.insertOrUpdateStaging(
                            movie.getId(),
                            movie.getName(),
                            movie.getDirector(),
                            movie.getActor(),
                            movie.getLimitAge(),
                            movie.getCountry(),
                            movie.getBrief(),
                            movie.getImage(),
                            movie.getReleaseDate(),
                            movie.getEndDate(),
                            movie.getDuration()

                    );
                    System.out.println("Dữ liệu đã được chèn thành công vào bảng staging.");
                    // 7.1.Ghi log và cập nhật status của table config= SUCCESS
                    dbHandler.logSuccess("Dữ liệu đã được chèn thành công cho phim: " + movie.getName());
                } catch (SQLException ex) {
                    // 7.2 Ghi log
                    dbHandler.logError("Lỗi khi chèn dữ liệu vào bảng staging: " + ex.getMessage());
                }
            }


        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
