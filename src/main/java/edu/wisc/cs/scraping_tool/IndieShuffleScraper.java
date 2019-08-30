package edu.wisc.cs.scraping_tool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Class used to fetch music (by scraping HTML content) from indieshuffle.com.
 * 
 * @author Zach Kremer
 *
 */
public class IndieShuffleScraper {

    ArrayList<Song> allSongs = new ArrayList<Song>();

    // used for playlist name
    String fetchedInfo = "";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);

    /**
     * Main method that fetches an arbitrary number of songs (up to 15) from each given genre.
     * Genres are passed via ArrayList and must be a particular string (associated with Beatport's
     * Genre conventions)
     * 
     * @param songsToFetch The amount of songs (from each genre) to fetch
     * @param userGenres The genres that the user would like to fetch from
     * @return Returns a playlist name including information about the genres collected and date
     * @throws FailingHttpStatusCodeException
     */
    public ArrayList<Song> fetch(int songsToFetch) throws FailingHttpStatusCodeException {

        if (songsToFetch > 15) {
            Main.output("Maximum amount of songs IndieShuffle can send is 15\nSetting "
                            + "that number to 15 now.");
            songsToFetch = 15;
        }

        Main.output("Fetching from Indie Shuffle");

        String baseUrl = "https://www.indieshuffle.com";
        WebClient client = new WebClient();

        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        try {
            String searchUrl = baseUrl + "/popular/month/";
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

                    HtmlElement artist = ((HtmlElement) htmlItem.getFirstByXPath(".//h5/strong"));
                    String strArtist = artist.asText();

                    HtmlElement albumArtContainer = ((HtmlElement) htmlItem
                                    .getFirstByXPath(".//div[contains(@class, 'commontrack')]"));
                    String albumArtLink = albumArtContainer.getAttribute("data-artwork").toString();

                    HtmlElement link = htmlItem.getFirstByXPath(".//a[@class='pink ajaxlink']");
                    String strLink = baseUrl + link.getAttribute("href");

                    // create song object
                    Song song = new Song();

                    song.setTitle(strTitle);
                    song.setArtist(strArtist);
                    song.setGenre("Indie");
                    song.setAlbumArtLink(albumArtLink);
                    song.setLink(strLink);

                    allSongs.add(song);

                    // print detailed information to console
                    Main.output("Position " + (++count) + ": " + song.getArtist() + " - "
                                    + song.getTitle() + " [" + song.getGenre() + "]");

                    TimeUnit.MILLISECONDS.sleep(50);

                    if (count == songsToFetch) {
                        break;
                    }
                }
            }

            client.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        fetchedInfo = "Indie Shuffle";
        return allSongs;
    }

    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }
}
