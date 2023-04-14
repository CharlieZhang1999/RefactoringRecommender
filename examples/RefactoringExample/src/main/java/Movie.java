/**
 * Movie represents the notion of a film. A video store might have several tapes in stock of the same movie
 */
public class Movie {
    private String name = "no name yet";
    public static final int CHILDREN = 2;
    public static final int  REGULAR = 0;
    public static final int  NEW_RELEASE = 1;

    private int priceCode;

    public Movie(String name, int priceCode) {
        this.name = name;
        this.priceCode = priceCode;
    }

    public int priceCode() {
        return priceCode;
    }

    public void persist() {
        Registrar.addMovie(name, this);
    }

    public  Movie get() {
        return Registrar.getMovie(name);
    }

    public String getName() {
        return name;
    }
}

