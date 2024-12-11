import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;

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
            Elements movies = crawler.fetchMovies();

            // Bước 2: Kiểm tra dữ liệu trả về
            if (movies == null || movies.isEmpty()) {
                System.out.println("Không có dữ liệu nào được trả về từ trang web!");
                dbHandler.logError("Dữ liệu trả về = null");
                return;
            }

            // Bước 3: Thực hiện crawl từng bộ phim trong danh sách
            for (Element movieElement : movies) {
                try {
                    // Phân tích dữ liệu từng bộ phim
                    Movie movie = crawler.parseMovie(movieElement);

                    // Lưu vào bảng staging
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
                } catch (Exception ex) {
                    // Log lỗi nếu quá trình crawl phim gặp vấn đề
                    dbHandler.logError("Lỗi khi crawl phim: " + ex.getMessage());
                }
            }

            // Bước 4: Lưu kết quả vào file CSV
            boolean saveSuccess = dbHandler.saveToCSV(config.getProperty("source.file"));
            if (saveSuccess) {
                System.out.println("Dữ liệu đã được lưu vào file CSV thành công.");
            } else {
                System.out.println("Lỗi khi lưu dữ liệu vào file CSV.");
                dbHandler.logError("Lỗi khi lưu file CSV.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
