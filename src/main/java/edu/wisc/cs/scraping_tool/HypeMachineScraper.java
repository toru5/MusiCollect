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

public class HypeMachineScraper {

    ArrayList<Song> allSongs = new ArrayList<Song>();

    // used for playlist name
    String fetchedInfo = "";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);

//    public static void main(String[] args) {
//        // TODO Auto-generated method stub
//        HypeMachineScraper h = new HypeMachineScraper();
//        h.fetch(500);
//
//    }

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
        Main.printLine("\nFetching from Hype Machine");

        String baseUrl = "https://hypem.com";
        WebClient client = new WebClient();

        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        int pageNumber = 1;
        int songCount = 0;

        while (songCount < songsToFetch - 1) {

            try {
                String searchUrl = baseUrl + "/latest/all/" + pageNumber;
                HtmlPage mainPage = client.getPage(searchUrl);

                // this is where you collect all the items
                @SuppressWarnings("unchecked")
                List<HtmlElement> items = (List<HtmlElement>) mainPage
                                .getByXPath(".//h3[@class='track_name']");
                if (items.isEmpty()) {
                    Main.printLine("No more items found.  Ending search");
                    songCount = songsToFetch - 1; // break out of the while loop as well
                    break;
                } else {
                    for (HtmlElement htmlItem : items) {
                        // extract items
                        String trackInfo = htmlItem.asText();
                        String[] trackTokens = trackInfo.split("-");
                        String title = trackTokens[1].trim();
                        String artist = trackTokens[0].trim();
                        
                        // create song object
                        Song song = new Song();

                        song.setTitle(title);
                        song.setArtist(artist);

                        allSongs.add(song);

                         // print detailed information to console
                         Main.printLine("Position " + (songCount + 1) + ": "
                         + song.getArtist() + " - " + song.getTitle());

                        TimeUnit.MILLISECONDS.sleep(50);
                        
                        if (songCount == songsToFetch - 1) {
                            break;
                        }
                        
                        songCount++;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            
            pageNumber++;
        }

        client.close();
        fetchedInfo = "Hype Machine";
        return allSongs;
    }
    
    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }

}
