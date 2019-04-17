package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Class used to fetch music (by scraping HTML content) from billboard.com
 * 
 * @author Zach Kremer
 *
 */
public class Billboard100Scraper {

    ArrayList<Song> allSongs = new ArrayList<Song>();
    ArrayList<String> genreLinks = new ArrayList<String>();
    ArrayList<String> genres = new ArrayList<String>();
    HashMap<String, Genre> genreMap;

    // used for playlist name
    String fetchedInfo = "";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);

    /**
     * Main method that fetches an arbitrary number of songs (up to 100) from each given genre.
     * Genres are passed via ArrayList and must be a particular string (associated with Beatport's
     * Genre conventions)
     * 
     * @param songsToFetch The amount of songs (from each genre) to fetch
     * @param userGenres The genres that the user would like to fetch from
     * @return Returns a playlist name including information about the genres collected and date
     * @throws FailingHttpStatusCodeException
     */
    public ArrayList<Song> fetch(int songsToFetch, ArrayList<String> userGenres,
                    boolean randomSongs) throws FailingHttpStatusCodeException {


        collectGenres(userGenres);
        String prettyGenreList = createGenreList(genres);
        Main.output("Fetching from Billboard: " + prettyGenreList);

        String baseUrl = "https://www.billboard.com/charts/";
        WebClient client = new WebClient();

        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        int genreCount = 0;

        // r-b-hip
        for (String s : genreLinks) {
            int maxSongs = 0;

            if (s.contains("200")) {
                if (songsToFetch > 200) {
                    songsToFetch = 200;
                }
                maxSongs = 200;
            } else if (s.contains("100")) {
                if (songsToFetch > 100) {
                    songsToFetch = 100;
                }
                maxSongs = 100;
            } else if (s.contains("r-b-hip") || s.contains("country")) {
                if (songsToFetch > 50) {
                    songsToFetch = 50;
                }
                maxSongs = 50;
            } else if (s.contains("contemporary")) {
                if (songsToFetch > 30) {
                    songsToFetch = 30;
                }
                maxSongs = 30;
            } else {
                if (songsToFetch > 40) {
                    songsToFetch = 40;
                }
                maxSongs = 40;
            }

            ArrayList<Integer> randoms = new ArrayList<Integer>();
            if (randomSongs) {
                for (int i = 0; i < songsToFetch; i++) {
                    Integer test = (int) (Math.random() * maxSongs);
                    while (randoms.contains(test)) {
                        test = (int) (Math.random() * maxSongs);
                    }

                    randoms.add(test);
                }
            }

            Song song = null;

            try {

                String searchUrl = baseUrl + s;
                HtmlPage mainPage = client.getPage(searchUrl);

                HtmlElement oneTitle = (HtmlElement) mainPage
                                .getFirstByXPath(".//div[@class='chart-number-one__title']");
                String strOneTitle = oneTitle.asText();
                HtmlElement oneArtist = (HtmlElement) mainPage
                                .getFirstByXPath(".//div[@class='chart-number-one__artist']");
                String strOneArtist = oneArtist.asText();

                // this is where you collect all the items
                // List<HtmlElement> items = (List<HtmlElement>) mainPage
                // .getByXPath(".//div[@class='chart-details ']");
                HtmlElement details = (HtmlElement) mainPage
                                .getFirstByXPath(".//div[@class='chart-details ']");

                if (details == null) {
                    System.out.println("No items found for " + searchUrl);
                } else {
                    for (int i = 1; i <= songsToFetch; i++) {
                        int songPos = 0;
                        if (randomSongs) {
                            songPos = randoms.get(i - 1);
                        } else {
                            songPos = i;
                        }

                        song = new Song();

                        if (i != 1 || randomSongs) {
                            // extract items
                            HtmlElement htmlItem = (HtmlElement) details.getFirstByXPath(
                                            ".//div[contains(@data-rank, '" + songPos + "')]");
                            String strTitle = htmlItem.getAttribute("data-title");

                            String strArtist = htmlItem.getAttribute("data-artist");

                            song.setTitle(strTitle);
                            song.setArtist(strArtist);
                            song.setGenre(genres.get(genreCount));
                        } else {
                            song.setTitle(strOneTitle);
                            song.setArtist(strOneArtist);
                            song.setGenre(genres.get(genreCount));
                        }

                        allSongs.add(song);

                        // print detailed information to console
                        Main.output(song.getGenre() + " - Position " + songPos + ": "
                                        + song.getArtist() + " - " + song.getTitle());

                        TimeUnit.MILLISECONDS.sleep(50); // be nice



                    }
                }

                client.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            genreCount++;
        }

        fetchedInfo = "Billboard";
        return allSongs;
    }

    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }

    /**
     * Method that iterates through a list of genres given by the user and adds their hashmap value
     * (billboard link) to an array of genre links
     * 
     * @param userGenres the list of String values corresponding to valid keys in the HashMap
     */
    private void collectGenres(ArrayList<String> userGenres) {
        for (String s : userGenres) {
            if (genreMap.get(s) != null) {
                genreLinks.add(genreMap.get(s).getPostLink());
                genres.add(s);
            }
        }
    }

    /**
     * Creates a backslash separated list of the genres to be fetched from
     * 
     * @param genres genres to be added toa large string
     * @return String identifier of genres that will be fetched
     */
    private String createGenreList(ArrayList<String> genres) {
        String fullTag = "";
        for (int i = 0; i < genres.size(); i++) {
            if (i == genres.size() - 1) {
                fullTag += genres.get(i);
            } else {
                fullTag += genres.get(i) + " / ";
            }
        }

        return fullTag;
    }

    /**
     * simple getter method
     * 
     * @return genreMap
     */
    public HashMap<String, Genre> getGenreMap() {
        return genreMap;
    }

    /**
     * simple setter method
     * 
     * @param genreMap
     */
    public void setGenreMap(HashMap<String, Genre> genreMap) {
        this.genreMap = genreMap;
    }
}
