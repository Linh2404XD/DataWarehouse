import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Bước 1: Tải dữ liệu từ file config.properties
            ConfigLoader config = new ConfigLoader("config.properties");

            // Thiết lập kết nối cơ sở dữ liệu
            DatabaseHandler dbHandler = new DatabaseHandler(
                    config.getProperty("db.url"),
                    config.getProperty("db.username"),
                    config.getProperty("db.password")
            );

            // Gọi hàm lấy dữ liệu cần crawl (dựa theo config)
            Crawler crawler = new Crawler(config.getProperty("source.url"));
            Elements moviesElements = crawler.fetchMovies();

            // Bước 2: Kiểm tra dữ liệu trả về
            if (moviesElements == null || moviesElements.isEmpty()) {
                System.out.println("Không có dữ liệu nào được trả về từ trang web!");
                dbHandler.logError("Dữ liệu trả về = null");
                return;
            }

            // Bước 3: Phân tích dữ liệu và tạo danh sách các bộ phim
            List<Movie> movies = new ArrayList<>();
            for (Element movieElement : moviesElements) {
                try {
                    // Phân tích dữ liệu từng bộ phim
                    Movie movie = crawler.parseMovie(movieElement);
                    movies.add(movie);
                } catch (Exception ex) {
                    // Log lỗi nếu quá trình crawl phim gặp vấn đề
                    dbHandler.logError("Lỗi khi crawl phim: " + ex.getMessage());
                }
            }

            // Bước 4: Lưu dữ liệu vào file CSV
            boolean saveSuccess = dbHandler.saveToCSV(movies, config.getProperty("source.file"));
            if (saveSuccess) {
                System.out.println("Dữ liệu đã được lưu vào file " + config.getProperty("source.file") + " thành công.");
            } else {
                System.out.println("Lỗi khi lưu dữ liệu vào file cinestar.csv.");
                dbHandler.logError("Lỗi khi lưu file cinestar.csv.");
            }

            // Bước 5: Lưu vào bảng staging
            for (Movie movie : movies) {
                try {
                    dbHandler.insertOrUpdateStaging(
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
// Ghi log sau khi thêm dữ liệu thành công vào bảng staging
                    dbHandler.logSuccess("Thêm dữ liệu thành công cho phim: " + movie.getName());
                } catch (SQLException ex) {
                    dbHandler.logError("Lỗi khi chèn dữ liệu vào bảng staging: " + ex.getMessage());
                }

            }
            // 8.Ghi log và cập nhật status của table config= SUCCESS
            System.out.println("Thêm dữ liệu thành công vào bảng Staging thành công");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
