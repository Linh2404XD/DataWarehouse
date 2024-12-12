public class Movie {
    private String id;
    private String name;
    private String director;
    private String actor;
    private String limitAge;
    private String country;
    private String brief;
    private String image;
    private String releaseDate;
    private String endDate;
    private int duration;

    // Constructor, getters, and setters
    public Movie(String id, String name, String director, String actor, String limitAge, String country,
                 String brief, String image, String releaseDate, String endDate, int duration) {
        this.id = id;
        this.name = name;
        this.director = director;
        this.actor = actor;
        this.limitAge = limitAge;
        this.country = country;
        this.brief = brief;
        this.image = image;
        this.releaseDate = releaseDate;
        this.endDate = endDate;
        this.duration = duration;
    }


    // Constructor mặc định
    public Movie() {}

    // Getter và Setter cho id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Các getter còn lại
    public String getName() { return name; }
    public String getDirector() { return director; }
    public String getActor() { return actor; }
    public String getLimitAge() { return limitAge; }
    public String getCountry() { return country; }
    public String getBrief() { return brief; }
    public String getImage() { return image; }
    public String getReleaseDate() { return releaseDate; }
    public String getEndDate() { return endDate; }
    public int getDuration() { return duration; }

    // Các setter còn lại
    public void setName(String name) { this.name = name; }
    public void setDirector(String director) { this.director = director; }
    public void setActor(String actor) { this.actor = actor; }
    public void setLimitAge(String limitAge) { this.limitAge = limitAge; }
    public void setCountry(String country) { this.country = country; }
    public void setBrief(String brief) { this.brief = brief; }
    public void setImage(String image) { this.image = image; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setDuration(int duration) { this.duration = duration; }
}
