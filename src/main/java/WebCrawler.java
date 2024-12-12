import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WebCrawler {
    public static void main(String[] args) {
        try {
            // Tải cấu hình
            ConfigLoader config = new ConfigLoader("config.properties");

            // Thu thập dữ liệu từ web
            Crawler crawler = new Crawler(config.getProperty("source.url"));
            Elements moviesElements = crawler.fetchMovies();

            // Kiểm tra xem có dữ liệu trả về hay không
            if (moviesElements == null || moviesElements.isEmpty()) {
                System.out.println("Không có dữ liệu nào được trả về từ trang web!");
                return;
            }

            // Phân tích dữ liệu và tạo danh sách các bộ phim
            List<Movie> movies = new ArrayList<>();
            for (Element movieElement : moviesElements) {
                try {
                    Movie movie = crawler.parseMovie(movieElement);
                    movies.add(movie);
                } catch (Exception ex) {
                    System.err.println("Lỗi khi phân tích phim: " + ex.getMessage());
                }
            }

            // Lưu dữ liệu vào tệp CSV
            DatabaseHandler dbHandler = new DatabaseHandler(
                    config.getProperty("db.url"),
                    config.getProperty("db.username"),
                    config.getProperty("db.password")
            );

            boolean saveSuccess = dbHandler.saveToCSV(movies, config.getProperty("source.file"));
            if (saveSuccess) {
                System.out.println("Dữ liệu đã được lưu thành công vào tệp " + config.getProperty("source.file"));
            } else {
                System.err.println("Lỗi khi lưu dữ liệu vào tệp CSV.");
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
