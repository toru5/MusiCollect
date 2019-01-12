package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class LastFmScraper {

    ArrayList<String> genreLinks = new ArrayList<String>();
    ArrayList<String> genres = new ArrayList<String>();
    HashMap<String, Genre> genreMap;

    final static String API_KEY = "3f0f17bc3fcf1b5fcda4cc9c776391d5";
    static int topSongs = 10;
    static int topArtists = 35;

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

    // public static void main(String[] args) {
    // LastFmScraper l = new LastFmScraper();
    // l.fetchSimilar("gregory alan isakov", 50);
    // }

    public ArrayList<String> fetchSimilar(String artistName, int songsToFetch)
                    throws FailingHttpStatusCodeException {
        Main.output("Fetching songs similar to " + artistName);
        File output = new File(strDate + "-similar.txt"); // keep local txt file as well
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        List<String> videoIds = null;
        ArrayList<String> allVideoIds = new ArrayList<String>();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = null;
        CloseableHttpResponse response = null;
        String jsonResponse = null;
        URI uri = null;
        JSONObject jsonObj = null;
        JSONParser jsonParse = new JSONParser();

        // create random parameters to go fetch songs
        ArrayList<Integer> randomArtists = new ArrayList<Integer>();
        ArrayList<Integer> randomSongs = new ArrayList<Integer>();
        ArrayList<String> uniqueChoices = new ArrayList<String>();

        for (int i = 0; i < songsToFetch; i++) {
            int artistTest = (int) (Math.random() * topArtists);
            int songTest = (int) (Math.random() * topSongs + 1);
            String choice = (String) (Integer.toString(artistTest) + "-"
                            + Integer.toString(songTest));

            while (uniqueChoices.contains(choice)) {
                // flip a coin
                int coin = (int) Math.random() * 2;
                if (coin == 0) {
                    songTest = (int) (Math.random() * topSongs + 1);
                } else {
                    artistTest = (int) (Math.random() * topArtists);
                }
                choice = (String) (Integer.toString(artistTest) + "-" + Integer.toString(songTest));
            }

            uniqueChoices.add(choice);
            randomArtists.add(artistTest);
            randomSongs.add(songTest);
        }

        try {
            uri = new URIBuilder().setScheme("http").setHost("ws.audioscrobbler.com/")
                            .setPath("2.0/").setParameter("method", "artist.getsimilar")
                            .setParameter("artist", artistName).setParameter("api_key", API_KEY)
                            .setParameter("format", "json").build();
            httpGet = new HttpGet(uri);
            response = client.execute(httpGet);
            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);

            // convert large piece of json to an array of artists
            jsonObj = (JSONObject) jsonObj.get("similarartists");
            if (jsonObj == null) {
                Main.output("ERROR: Could not find any artists similar to " + artistName
                                + "\nCheck spelling and try again.");
                return allVideoIds;
            }
            JSONArray artistArray = (JSONArray) jsonObj.get("artist");

            // check if last.fm returned nothing
            if (artistArray.size() == 0) {
                if (artistName.contains(" ")) {
                    artistName = artistName.replaceAll(" ", "_");
                    Main.output("Re-running with new parameters: " + artistName + ", "
                                    + songsToFetch);
                    return fetchSimilar(artistName, songsToFetch);
                }

                Main.output("ERROR: last.fm returned nothing with search: " + artistName
                                + "\nThis is probably a server-side error. Try searching again with"
                                + " a different artist that is similar to " + artistName
                                + " for comparable results.");
                return allVideoIds;
            }

            // loop through random artists and fetch their top songs
            for (int i = 0; i < songsToFetch; i++) {
                try {
                    JSONObject js = (JSONObject) artistArray.get(randomArtists.get(i));
                    String strArtist = js.get("name").toString();

                    uri = new URIBuilder().setScheme("http").setHost("ws.audioscrobbler.com/")
                                    .setPath("2.0/").setParameter("method", "artist.gettoptracks")
                                    .setParameter("artist", strArtist)
                                    .setParameter("api_key", API_KEY).setParameter("format", "json")
                                    .build();

                    httpGet = new HttpGet(uri);
                    response = client.execute(httpGet);
                    jsonResponse = EntityUtils.toString(response.getEntity());
                    jsonObj = (JSONObject) jsonParse.parse(jsonResponse);

                    // convert large piece of json to an array of artists
                    jsonObj = (JSONObject) jsonObj.get("toptracks");
                    JSONArray songArray = (JSONArray) jsonObj.get("track");

                    JSONObject j2 = (JSONObject) songArray.get(randomSongs.get(i));
                    String strTitle = j2.get("name").toString();

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
                    // get genre by clicking on song link and grabbings tags (will slow it down,tho)
                    song.setYoutubeLink(strYouTubeLink);
                    song.setYoutubeEmbedLink(strYouTubeEmbedLink);

                    // print detailed information to console
                    Main.output("Song " + (i + 1) + ": " + song.getArtist() + " - "
                                    + song.getTitle());
                    // System.out.println("Song " + i + ": " + song.getArtist() + " - " +
                    // song.getTitle());
                    writer.println("Song: " + (i + 1) + "\n" + song.toString() + "\n");
                } catch (IndexOutOfBoundsException e) {
                    Main.output("Narrowing search parameters to meet measly number of similar "
                                    + "artists and songs...");
                    topArtists = artistArray.size();
                    topSongs -= 2; // just to make sure :))
                    allVideoIds.addAll(fetchSimilar(artistName, (songsToFetch - i)));
                    writer.close();
                    fetchedInfo = "Similar Artists to: " + artistName;
                    return allVideoIds;
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        fetchedInfo = "Similar Artists to: " + artistName;
        writer.close();


        return allVideoIds;

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

        ArrayList<String> allVideoIds = new ArrayList<String>();
        File output = new File(strDate + "-last-fm.txt"); // keep local txt file as well
        PrintWriter writer = null;
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
            Main.output("Error fetching from last.fm -- username or password may be typed incorrectly.");
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

            videoIds = YouTubeScraper.ySearch(strArtist, strTitle);

            if (videoIds == null) {
                continue;
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
            Main.output("Song " + songPos + ": " + song.getArtist() + " - " + song.getTitle());
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
                Main.output("Error fetching from last.fm -- username or password may be typed incorrectly.");
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

