package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class IndieShuffleScraper {

    ArrayList<String> allVideoIds = new ArrayList<String>();
    
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
    public ArrayList<String> fetch(int songsToFetch)
                    throws FailingHttpStatusCodeException {

        Main.output("Fetching from Indie Shuffle");

        File output = new File(strDate + "-indie-shuffle-songs.txt"); // keep local txt file as well
        PrintWriter writer = null;

        String baseUrl = "https://www.indieshuffle.com";
        WebClient client = new WebClient();


        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);


        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        try {
            String searchUrl = baseUrl + "/popular/week/";
            HtmlPage mainPage = client.getPage(searchUrl);
            @SuppressWarnings("unchecked")

            // this is where you collect all the items
            List<HtmlElement> items = (List<HtmlElement>) mainPage
                            .getByXPath(".//div[@class='sortable-item']");
            if (items.isEmpty()) {
                Main.output("No items found");
            } else {
                int count = 0;
                for (HtmlElement htmlItem : items) {
                    // extract items
                    HtmlElement title = ((HtmlElement) htmlItem.getFirstByXPath(".//h5"));
                    String strTitle = title.asText();
                    String[] titleTokens = strTitle.split("-");
                    strTitle = titleTokens[1];
                    
                    HtmlElement artist = ((HtmlElement) htmlItem
                                    .getFirstByXPath(".//h5/strong"));
                    String strArtist = artist.asText();

                    HtmlElement albumArtContainer = ((HtmlElement) htmlItem.getFirstByXPath(
                                    ".//div[contains(@class, 'commontrack')]"));
                    String albumArtLink = albumArtContainer.getAttribute("data-artwork").toString();
                    
                    HtmlElement link = htmlItem.getFirstByXPath(
                                    ".//a[@class='pink ajaxlink']");
                    String strLink = baseUrl + link.getAttribute("href");

                    List<String> videoIds;

                    videoIds = YouTubeScraper.ySearch(strArtist, strTitle);
                    
                    if (videoIds == null) {
                        continue;
                    }

                    // generate links to videos and playlists
                    String strYouTubeLink = YouTubeScraper.generateLink(videoIds.get(0));
                    String strYouTubeEmbedLink = YouTubeScraper.generateEmbedLink(videoIds.get(0));
                    allVideoIds.add(videoIds.get(0));

                    // create song object
                    Song song = new Song();

                    song.setTitle(strTitle);
                    song.setArtist(strArtist);
                    song.setGenre("Indie");
                    song.setAlbumArtLink(albumArtLink);
                    song.setLink(strLink);
                    song.setYoutubeLink(strYouTubeLink);
                    song.setYoutubeEmbedLink(strYouTubeEmbedLink);

                    // print detailed information to console
                    Main.output(song.getGenre() + " - Position " + (count + 1) + ": "
                                    + song.getArtist() + " - " + song.getTitle());
                    writer.println("Song: " + (count + 1) + "\n" + song.toString() + "\n");

                    // Thread.sleep(1000); // be nice to website?

                    if (count == songsToFetch - 1) {
                        break;
                    }

                    count++;
                }
            }

            client.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        fetchedInfo = "Indie Shuffle (Top " + songsToFetch + " Songs)";
        fetchedInfo = "Indie Shuffle";
        return allVideoIds;

    }
    
    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }
}
