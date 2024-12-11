import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Crawler {

    private String url;

    public Crawler(String url) {
        this.url = url;
    }

    public Elements fetchMovies() throws IOException {
        // Bước 2: Crawl toàn bộ danh sách phim từ trang web
        Document doc = Jsoup.connect(url).get();
        return doc.select(".web-movie-box"); // Sử dụng selector chính xác cho phim
    }

    public Movie parseMovie(Element movieElement) throws IOException {
        // Phân tích thông tin chi tiết từng bộ phim
        String name = movieElement.select(".name").first().text(); // Lấy phần tử đầu tiên nếu có nhiều phần tử trùng

        // Lấy đường dẫn đến trang chi tiết phim
        String movieDetailUrl = movieElement.select("a").attr("href");
        if (!movieDetailUrl.startsWith("http")) {
            movieDetailUrl = "https://www.cinestar.com.vn" + movieDetailUrl; // Thêm domain vào nếu đường dẫn là relative
        }

        // Gọi trang chi tiết phim để lấy thêm thông tin
        Document movieDetailDoc = Jsoup.connect(movieDetailUrl).get();
// Lấy thông tin từ trang chi tiết
        String director = "";
        String actor = "";
        String releaseDate = "";
        String brief = "";

        // Lấy thông tin trong phần MÔ TẢ
        Element descriptionElement = movieDetailDoc.select(".detail-ct-bd").first();
        if (descriptionElement != null) {
            Elements infoItems = descriptionElement.select("ul li");

            // Lấy đạo diễn
            if (infoItems.size() > 0) {
                director = infoItems.get(0).text().replace("Đạo diễn: ", "").trim();
            }

            // Lấy diễn viên
            if (infoItems.size() > 1) {
                actor = infoItems.get(1).text().replace("Diễn viên: ", "").trim();
            }

            // Lấy ngày phát hành (kiểm tra nếu có phần tử thứ 3)
            if (infoItems.size() > 2) {
                releaseDate = infoItems.get(2).text().replace("Khởi chiếu: ", "").trim();
            }
        }
        brief = movieDetailDoc.select("p.txt.line-clamp-6").text();


        String limitAge = movieElement.select(".age .num").text() + " " + movieElement.select(".age .txt").text(); // Kết hợp số và loại độ tuổi
        String country = movieElement.select(".info-item i.fa-earth-americas + span.txt").text();
        String image = movieElement.select(".image img").attr("src"); // Đường dẫn hình ảnh
        String endDate = movieDetailDoc.select(".end-date-selector").text(); // Ngày kết thúc
        int duration = 0;
        String durationText = movieElement.select(".info-item .txt").stream()
                .map(Element::text)
                .filter(e -> e.contains("'")) // Kiểm tra chuỗi chứa thời lượng (ví dụ 80')
                .findFirst()
                .orElse("");
        if (!durationText.isEmpty()) {
            duration = Integer.parseInt(durationText.replace("'", "").trim()); // Xử lý thời lượng
        }

        return new Movie(name, director, actor, limitAge, country, brief, image, releaseDate, endDate, duration);
    }

    public static void main(String[] args) throws IOException {
        // Thay đổi URL theo website mà bạn muốn crawl
        String url = "https://cinestar.com.vn/";
        Crawler crawler = new Crawler(url);

        // Lấy danh sách phim
        Elements movies = crawler.fetchMovies();

        // Duyệt qua từng phim và in ra thông tin
        for (Element movieElement : movies) {
            Movie movie = crawler.parseMovie(movieElement);
            System.out.println("Tên phim: " + movie.getName());
            System.out.println("Đạo diễn: " + movie.getDirector());
            System.out.println("Diễn viên: " + movie.getActor());
            System.out.println("Giới hạn tuổi: " + movie.getLimitAge());
            System.out.println("Quốc gia: " + movie.getCountry());
            System.out.println("Mô tả: " + movie.getBrief());
            System.out.println("Hình ảnh: " + movie.getImage());
            System.out.println("Ngày phát hành" + movie.getReleaseDate());
            System.out.println("Ngày kết thúc: " + movie.getEndDate());
            System.out.println("Thời lượng: " + movie.getDuration() + " phút");
            System.out.println("--------------------------------------------------");
        }
    }
}
