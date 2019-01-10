package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class BeatportScraper {

    // initialize lists
    ArrayList<String> allVideoIds = new ArrayList<String>();
    ArrayList<String> genreLinks = new ArrayList<String>();
    ArrayList<String> genres = new ArrayList<String>();
    HashMap<String, Genre> genreMap;
    String fetchedInfo = "";

    // used for playlist name
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
                    boolean randomSongs, boolean getExtraInfo) {

        collectGenres(userGenres);
        String prettyGenreList = createGenreList(genres);
        Main.output("Fetching from Beatport: " + prettyGenreList);

        String baseUrl = "https://www.beatport.com/genre/";
        WebClient client = new WebClient();


        int genreCount = 0; // potentially unneeded now that we use a hashmap

        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        File output = new File(strDate + "-beatport-songs.txt"); // keep local txt file as well
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        for (String genreLink : genreLinks) {
            String searchUrl = baseUrl + genreLink;
            HtmlPage mainPage = null;
            int maxSongs = 100;
            int count = 0;

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

            try {
                mainPage = client.getPage(searchUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            @SuppressWarnings("unchecked")
            List<HtmlElement> items = (List<HtmlElement>) mainPage
                            .getByXPath(".//li[@class='bucket-item ec-item track']");
            if (items.isEmpty()) {
                Main.output("No items found");
                break;
            } else {
                for (HtmlElement htmlItem : items) {

                    // create song object
                    Song song = new Song();
                    // extract items
                    if (randomSongs && !randoms.contains(count)) {

                    } else {

                        HtmlElement title = ((HtmlElement) htmlItem.getFirstByXPath(
                                        ".//span[@class='buk-track-primary-title']"));
                        HtmlElement mix = ((HtmlElement) htmlItem
                                        .getFirstByXPath(".//span[@class='buk-track-remixed']"));
                        String strTitle = title.asText() + " (" + mix.asText() + ")";


                        HtmlElement artist = ((HtmlElement) htmlItem
                                        .getFirstByXPath(".//p[@class='buk-track-artists']"));
                        String strArtist = artist.asText();


                        HtmlElement remixer = ((HtmlElement) htmlItem
                                        .getFirstByXPath(".//p[@class='buk-track-remixers']"));
                        String strRemixer = remixer == null ? "" : remixer.asText();

                        // get a bunch of extra stuff that will add a costly penalty to the runtime
                        // of the application
                        if (getExtraInfo) {
                            HtmlAnchor releaseBtn = ((HtmlAnchor) htmlItem
                                            .getFirstByXPath(".//p[@class='buk-track-title']/a"));
                            HtmlPage releasePage = null;

                            try {
                                releasePage = releaseBtn.click();
                            } catch (IOException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                                continue;
                            }


                            HtmlElement albumContainer = releasePage.getFirstByXPath(
                                            ".//li[@class='interior-track-releases-artwork-container ec-item']");
                            String strAlbum = albumContainer.getAttribute("data-ec-name");



                            HtmlElement releaseContainer = releasePage.getFirstByXPath(
                                            ".//li[@class='interior-track-content-item interior-track-released']");
                            HtmlElement releaseDate = releaseContainer
                                            .getFirstByXPath(".//span[@class='value']");
                            String strReleaseDate = releaseDate.asText();

                            HtmlElement recordLabel = ((HtmlElement) htmlItem
                                            .getFirstByXPath(".//p[@class='buk-track-labels']"));
                            String strRecordLabel = recordLabel.asText();

                            HtmlElement albumArtContainer =
                                            ((HtmlElement) releasePage.getFirstByXPath(
                                                            ".//img[@class='interior-track-release-artwork']"));
                            String albumArtLink = albumArtContainer.getAttribute("src");


                            HtmlElement link = releasePage.getFirstByXPath(
                                            ".//input[@class='share-embed-drop-copy-text']");
                            String strLink = link.getAttribute("value");


                            HtmlElement embedLink = releasePage.getFirstByXPath(
                                            ".//input[@class='share-embed-drop-copy-text'][2]");
                            String strEmbedLink = embedLink.getAttribute("value");

                            song.setReleaseDate(strReleaseDate);
                            song.setRecordLabel(strRecordLabel);
                            song.setAlbumArtLink(albumArtLink);
                            song.setLink(strLink);
                            song.setEmbedLink(strEmbedLink);
                            song.setAlbum(strAlbum);
                        }

                        String yTitle;

                        if (!strTitle.contains("Original Mix")) {
                            yTitle = strTitle;
                        } else {
                            yTitle = title.asText();

                        }

                        List<String> videoIds;

                        videoIds = YouTubeScraper.ySearch(strArtist, yTitle);
                        if (videoIds == null) {
                            videoIds = YouTubeScraper.ySearch(strArtist, title.asText());

                            if (videoIds == null) {
                                continue;
                            }
                        }


                        // generate links to videos and playlists
                        String strYouTubeLink = YouTubeScraper.generateLink(videoIds.get(0));
                        String strYouTubeEmbedLink =
                                        YouTubeScraper.generateEmbedLink(videoIds.get(0));
                        allVideoIds.add(videoIds.get(0));

                        song.setTitle(strTitle);
                        song.setArtist(strArtist);
                        song.setRemixer(strRemixer);
                        song.setGenre(genres.get(genreCount));

                        song.setYoutubeLink(strYouTubeLink);
                        song.setYoutubeEmbedLink(strYouTubeEmbedLink);

                        // print detailed information to console
                        Main.output(song.getGenre() + " - Position " + (count + 1) + ": "
                                        + song.getArtist() + " - " + song.getTitle());
                        writer.println("Song: " + (count + 1) + "\n" + song.toString() + "\n");

                        // Thread.sleep(1000); // be nice to website?
                    }

                    if (!randomSongs && count == songsToFetch - 1) {
                        break;
                    } else if (count == 99) {
                        break;
                    }
                    count++;
                }
            }
            genreCount++;

        }

        // // create playlist -- this should eventually be in the main method??
        // playlistId = YouTubeScraper.createPlaylist(allVideoIds,
        // strDate + "-Beatport-Results (Genres: " + prettyGenreList);
        // fetchedInfo = "Beatport (Genres: " + prettyGenreList + ")";
        fetchedInfo = "Beatport";

        client.close();
        writer.close();

        return allVideoIds;

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

    /**
     * 
     * @param userGenres
     */
    private void collectGenres(ArrayList<String> userGenres) {

        for (String s : userGenres) {
            if (genreMap.get(s) != null) {
                genreLinks.add(genreMap.get(s).getPostLink());
                genres.add(s);
            }
        }

    }


    public HashMap<String, Genre> getGenreMap() {
        return genreMap;
    }


    public void setGenreMap(HashMap<String, Genre> genreMap) {
        this.genreMap = genreMap;
    }


    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }

}
