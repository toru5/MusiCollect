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
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class lastFmScraper {

    ArrayList<String> allVideoIds = new ArrayList<String>();
    ArrayList<String> genreLinks = new ArrayList<String>();
    ArrayList<String> genres = new ArrayList<String>();
    HashMap<String, Genre> genreMap;

    // used for playlist name
    String fetchedInfo = "";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);
    String userName;
    String password;

    WebClient client = new WebClient();
    HtmlPage mainPage;


    public void setUserLogin(String uName, String password) {
        this.userName = uName;
        this.password = password;
    }


    public static void main(String[] args) {
        lastFmScraper l = new lastFmScraper();
        l.fetch(3);
    }

    /**
     * THE METHOD verifyUsernamePassword() MUST BE CALLED BEFORE CALLING THIS METHOD Main method
     * that fetches an arbitrary number of songs (up to 100) from each given genre. Genres are
     * passed via ArrayList and must be a particular string (associated with Beatport's Genre
     * conventions)
     * 
     * @param songsToFetch The amount of songs (from each genre) to fetch
     * @param userGenres The genres that the user would like to fetch from
     * @return Returns a playlist name including information about the genres collected and date
     * @throws FailingHttpStatusCodeException
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> fetch(int songsToFetch) throws FailingHttpStatusCodeException {

        Main.output("Fetching from Last.fm suggested tracks: ");

        File output = new File(strDate + "-last-fm.txt"); // keep local txt file as well
        PrintWriter writer = null;

        YouTubeScraper y = new YouTubeScraper();
        List<String> videoIds = null;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        Song song = null;

        List<HtmlElement> items = null;

        // this is where you collect all the items
        try {
            items = (List<HtmlElement>) mainPage
                            .getByXPath(".//div[@class='recs-feed-inner-wrap']");
        } catch (Exception e) {
            Main.output("ERROR: Invalid username or password");
            client.close();
            return null;
        }

        if (items.size() == 0) {
            Main.output(
                            "Error fetching from last.fm -- username or password may be typed incorrectly.");
        }
        int songPos = 1;
        for (HtmlElement htmlItem : items) {

            HtmlElement title = (HtmlElement) htmlItem
                            .getFirstByXPath(".//a[@class='link-block-target']");
            String strTitle = title.asText();

            if (strTitle.charAt(strTitle.length() - 1) == ')') {
                // trim song length from the end
                int i = strTitle.length() - 1;
                char[] chTitle = strTitle.toCharArray();
                while (chTitle[i] != '(') {
                    chTitle[i] = ' ';
                    i--;
                }
                chTitle[i] = ' ';

                strTitle = new String(chTitle);
            }
            
            strTitle = strTitle.trim();
            HtmlElement artist = (HtmlElement) htmlItem
                            .getFirstByXPath(".//p[@class='recs-feed-description']/a");
            String strArtist = artist.asText();

            // searches for the song on youtube, returns the first video, if nothing
            // is
            // found, the program narrows down the search by shedding the "artist"
            // parameter
            try {
                videoIds = y.search(strArtist, strTitle, 1); // fetch 1 video
            } catch (NoSuchElementException e) {
                Main.output("No videos found for: " + strArtist + " " + strTitle
                                + "\nSearching for: " + strTitle);
                try {
                    videoIds = y.search("", strTitle, 1); // simplify parameters
                } catch (NoSuchElementException ee) {
                    Main.output("Error finding song: " + strTitle
                                    + "\nSkipping to next track...");
                    break;
                }
            }

            // generate links to videos and playlists
            String strYouTubeLink = YouTubeScraper.generateLink(videoIds.get(0));
            String strYouTubeEmbedLink = YouTubeScraper.generateEmbedLink(videoIds.get(0));
            allVideoIds.add(videoIds.get(0));
            // create song object
            song = new Song();
            song.setTitle(strTitle);
            song.setArtist(strArtist);
            // get genre by clicking on song link and grabbings tags (will slow it down,tho)
            song.setYoutubeLink(strYouTubeLink);
            song.setYoutubeEmbedLink(strYouTubeEmbedLink);

            // print detailed information to console
            Main.output(
                            "Song " + songPos + ": " + song.getArtist() + " - " + song.getTitle());
            writer.println("Song: " + songPos + "\n" + song.toString() + "\n");

            // Thread.sleep(1000); // be nice to website?
            if (songPos == songsToFetch) {
                break;
            } else {
                songPos++;
            }
        }

        client.close();
        writer.close();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        // fetchedInfo = "Billboard Top 100 - Genres: " + prettyGenreList + " (Top " + songsToFetch
        // + " Songs)";
        fetchedInfo = "Last.fm";
        return allVideoIds;

    }

    @SuppressWarnings("unchecked")
    public boolean verifyUserNamePassword() {
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        try {
            String baseUrl = "https://www.last.fm/";
            HtmlPage loginPage = client.getPage("https://secure.last.fm/login");
            mainPage = null;
            HtmlInput userNameInput = loginPage.getFirstByXPath(".//input[@id='id_username']");
            HtmlInput passwordInput = loginPage.getFirstByXPath(".//input[@id='id_password']");
            String searchUrl = baseUrl + "home/tracks";

            userNameInput.setValueAttribute(userName);
            passwordInput.setValueAttribute(password);

            // passwordInput.setTextContent(password);
            HtmlButton submitBtn =
                            (HtmlButton) loginPage.getFirstByXPath(".//button[@name='submit']");
            mainPage = submitBtn.click();
            mainPage = client.getPage(searchUrl);
            List<HtmlElement> items = null;

            // this is where you collect all the items
            try {
                items = (List<HtmlElement>) mainPage
                                .getByXPath(".//div[@class='recs-feed-inner-wrap']");
            } catch (Exception e) {
                Main.output("ERROR: Invalid username or password");
                client.close();
                return false;
            }

            if (items.size() == 0) {
                Main.output(
                                "Error fetching from last.fm -- username or password may be typed incorrectly.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
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

