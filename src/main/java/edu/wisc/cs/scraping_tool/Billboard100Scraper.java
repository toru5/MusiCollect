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
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Billboard100Scraper {

    ArrayList<String> allVideoIds = new ArrayList<String>();
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
    public ArrayList<String> fetch(int songsToFetch, ArrayList<String> userGenres,
                    boolean randomSongs) throws FailingHttpStatusCodeException {


        collectGenres(userGenres);
        String prettyGenreList = createGenreList(genres);
        System.out.println("Fetching from Billboard: " + prettyGenreList);

        String baseUrl = "https://www.billboard.com/charts/";
        WebClient client = new WebClient();


        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        File output = new File(strDate + "-billboard-100-songs.txt"); // keep local txt file as well
        PrintWriter writer = null;

        YouTubeScraper y = new YouTubeScraper();
        List<String> videoIds = null;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

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
            }else if (s.contains("contemporary")) {
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


                @SuppressWarnings("unchecked")
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
                        if (i != 1 || randomSongs) {
                            // extract items
                            HtmlElement htmlItem = (HtmlElement) details.getFirstByXPath(
                                            ".//div[contains(@data-rank, '" + songPos + "')]");
                            String strTitle = htmlItem.getAttribute("data-title");

                            String strArtist = htmlItem.getAttribute("data-artist");

                            // searches for the song on youtube, returns the first video, if nothing
                            // is
                            // found, the program narrows down the search by shedding the "artist"
                            // parameter
                            try {
                                videoIds = y.search(strArtist, strTitle, 1); // fetch 1 video
                            } catch (NoSuchElementException e) {
                                System.out.println("No videos found for: " + strArtist + " "
                                                + strTitle + "\nSearching for: " + strTitle);
                                try {
                                    videoIds = y.search("", strTitle, 1); // simplify parameters
                                } catch (NoSuchElementException ee) {
                                    System.out.println("Error finding song: " + strTitle
                                                    + "\nSkipping to next track...");
                                    break;
                                }
                            }

                            // generate links to videos and playlists
                            String strYouTubeLink = YouTubeScraper.generateLink(videoIds.get(0));
                            String strYouTubeEmbedLink =
                                            YouTubeScraper.generateEmbedLink(videoIds.get(0));
                            allVideoIds.add(videoIds.get(0));
                            // create song object
                            song = new Song();
                            song.setTitle(strTitle);
                            song.setArtist(strArtist);
                            song.setGenre(genres.get(genreCount));
                            song.setYoutubeLink(strYouTubeLink);
                            song.setYoutubeEmbedLink(strYouTubeEmbedLink);
                        } else {
                            // searches for the song on youtube, returns the first video, if nothing
                            // is
                            // found, the program narrows down the search by shedding the "artist"
                            // parameter
                            try {
                                videoIds = y.search(strOneArtist, strOneTitle, 1); // fetch 1 video
                            } catch (NoSuchElementException e) {
                                System.out.println("No videos found for: " + strOneArtist + " "
                                                + strOneTitle + "\nSearching for: " + strOneTitle);
                                try {
                                    videoIds = y.search("", strOneTitle, 1); // simplify parameters
                                } catch (NoSuchElementException ee) {
                                    System.out.println("Error finding song: " + strOneTitle
                                                    + "\nSkipping to next track...");
                                    break;
                                }
                            }

                            allVideoIds.add(videoIds.get(0));
                            // create song object
                            song = new Song();
                            song.setTitle(strOneTitle);
                            song.setArtist(strOneArtist);
                            song.setGenre(genres.get(genreCount));
                            // generate links to videos and playlists
                            String strYouTubeLink = YouTubeScraper.generateLink(videoIds.get(0));
                            String strYouTubeEmbedLink =
                                            YouTubeScraper.generateEmbedLink(videoIds.get(0));
                            song.setYoutubeLink(strYouTubeLink);
                            song.setYoutubeEmbedLink(strYouTubeEmbedLink);
                        }
                        // print detailed information to console
                        System.out.println(song.getGenre() + " - Position " + songPos + ": "
                                        + song.getArtist() + " - " + song.getTitle());
                        writer.println("Song: " + songPos + "\n" + song.toString() + "\n");

                        // Thread.sleep(1000); // be nice to website?



                    }
                }

                client.close();
                writer.close();
            } catch (

            Exception e) {
                e.printStackTrace();
            }
            genreCount++;
        }

        // fetchedInfo = "Billboard Top 100 - Genres: " + prettyGenreList + " (Top " + songsToFetch
        // + " Songs)";
        fetchedInfo = "Billboard";
        return allVideoIds;

    }

    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }

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
     * @param genres
     * @return
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

    public HashMap<String, Genre> getGenreMap() {
        return genreMap;
    }

    public void setGenreMap(HashMap<String, Genre> genreMap) {
        this.genreMap = genreMap;
    }
}
