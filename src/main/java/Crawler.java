import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        String name = movieElement.select(".name").first().text();

        String movieDetailUrl = movieElement.select("a").attr("href");
        if (!movieDetailUrl.startsWith("http")) {
            movieDetailUrl = "https://www.cinestar.com.vn" + movieDetailUrl;
        }

        Document movieDetailDoc = Jsoup.connect(movieDetailUrl).get();

        String director = "";
        String actor = "";
        String releaseDate = "";
        String brief = "";

        // Phân tích phần MÔ TẢ
        Element descriptionElement = movieDetailDoc.select(".detail-ct-bd").first();
        if (descriptionElement != null) {
            Elements infoItems = descriptionElement.select("ul li"); // Chọn đúng danh sách li

            // Kiểm tra số lượng phần tử
            int size = infoItems.size();

            if (size == 1) {
                // Nếu chỉ có 1 phần tử, phần tử 0 là "Khởi chiếu"
                String text = infoItems.get(0).text().replaceAll("<!--.*?-->", "").trim(); // Loại bỏ chú thích <!-- -->
                releaseDate = text;

            } else if (size == 2) {
                // Nếu có 2 phần tử, phần tử 0 là "Đạo diễn", phần tử 1 là "Khởi chiếu"
                String directorText = infoItems.get(0).text().replaceAll("<!--.*?-->", "").trim();
                director = directorText;


                String releaseText = infoItems.get(1).text().replaceAll("<!--.*?-->", "").trim();
                releaseDate = releaseText;

            } else if (size == 3) {
                // Nếu có 3 phần tử, phần tử 0 là "Đạo diễn", phần tử 1 là "Diễn viên", phần tử 2 là "Khởi chiếu"
                String directorText = infoItems.get(0).text().replaceAll("<!--.*?-->", "").trim();
                director = directorText;


                String actorText = infoItems.get(1).text().replaceAll("<!--.*?-->", "").trim();
                actor = actorText;


                String releaseText = infoItems.get(2).text().replaceAll("<!--.*?-->", "").trim();
                releaseDate = releaseText;

            }
        }

        brief = movieDetailDoc.select("p.txt.line-clamp-6").text();

        String limitAge = movieElement.select(".age .num").text() + " " + movieElement.select(".age .txt").text();
        String country = movieElement.select(".info-item i.fa-earth-americas + span.txt").text();
        String image = movieElement.select(".image img").attr("src");
        String endDate = movieDetailDoc.select(".end-date-selector").text();

        int duration = 0;
        String durationText = movieElement.select(".info-item .txt").stream()
                .map(Element::text)
                .filter(e -> e.contains("'"))
                .findFirst()
                .orElse("");
        if (!durationText.isEmpty()) {
            try {
                duration = Integer.parseInt(durationText.replace("'", "").trim());
            } catch (NumberFormatException e) {
                System.err.println("Error parsing duration: " + durationText);
            }
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
            System.out.println("Ngày phát hành: " + movie.getReleaseDate());
            System.out.println("Ngày kết thúc: " + movie.getEndDate());
            System.out.println("Thời lượng: " + movie.getDuration() + " phút");
            System.out.println("--------------------------------------------------");
        }
    }
}
