package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet httpGet = null;
    CloseableHttpResponse response = null;
    String jsonResponse = null;
    URI uri = null;
    JSONObject jsonObj = null;
    JSONParser jsonParse = new JSONParser();

    final static String API_KEY = "3f0f17bc3fcf1b5fcda4cc9c776391d5";
    static int topSongs = 5; // can dynamically change if artist does not have 5 songs released
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

    /**
     * Fetching method that retrieves a list of friends from a specified user and fetches an equal
     * amount of songs from each user until the quota is met. The method is dynamic and can handle
     * friend's not having music, as it will keep track of this and run extra iterations to fill
     * gaps
     * 
     * @param username the user of which to retrieve a friends list from
     * @param timePeriod time interval to fetch songs from, valid identifiers are: "7day", "1month",
     *        "3month", "12month"
     * @param songsToFetch the song quota to be met
     * @return returns a list of Song objects
     */
    public ArrayList<Song> fetchFriendsMusic(String username, String timePeriod, int songsToFetch) {
        Main.output("Fetching music from friends of " + username);
        ArrayList<Song> allSongs = new ArrayList<Song>();
        File output = new File(strDate + "-lfm-friends.txt"); // keep local txt file as well
        PrintWriter writer = null;

        int songsFromEachUser;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        try {
            uri = new URIBuilder().setScheme("http").setHost("ws.audioscrobbler.com")
                            .setPath("2.0/").setParameter("method", "user.getfriends")
                            .setParameter("user", username).setParameter("api_key", API_KEY)
                            .setParameter("format", "json").build();
            httpGet = new HttpGet(uri);
            response = httpClient.execute(httpGet);
            jsonResponse = EntityUtils.toString(response.getEntity());
            System.out.println(jsonResponse);
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            System.out.println(jsonResponse);

            try {
                if (jsonObj.get("error").toString().equals("6")) {
                    Main.output("ERROR: We couldn't find any users with name: " + username);
                    return allSongs;
                }
            } catch (NullPointerException e) {
                // this just means no errors were thrown... proceed as usual.
            }

            jsonObj = (JSONObject) jsonObj.get("friends");
            if (jsonObj == null) {
                Main.output("ERROR: This is awkward.. you don't have any friends");
                return allSongs;
            }

            // convert large piece of json to an array of friends
            JSONArray userArray = (JSONArray) jsonObj.get("user");

            songsFromEachUser = (int) Math.ceil(((double) songsToFetch) / userArray.size());

            int iteration = 0;
            while (allSongs.size() < songsToFetch) {
                for (Object o : userArray) {
                    JSONObject user = (JSONObject) o;
                    String currentFriend = user.get("name").toString();

                    uri = new URIBuilder().setScheme("http").setHost("ws.audioscrobbler.com")
                                    .setPath("2.0/").setParameter("method", "user.gettoptracks")
                                    .setParameter("user", currentFriend)
                                    .setParameter("period", timePeriod)
                                    .setParameter("api_key", API_KEY).setParameter("format", "json")
                                    .build();
                    httpGet = new HttpGet(uri);
                    response = httpClient.execute(httpGet);
                    jsonResponse = EntityUtils.toString(response.getEntity());
                    jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
                    jsonObj = (JSONObject) jsonObj.get("toptracks");
                    JSONArray songArray = (JSONArray) jsonObj.get("track");

                    int count = 0;
                    int skip = 0;
                    for (Object currentObject : songArray) {
                        if ((iteration * songsFromEachUser) > skip) {
                            skip++;
                            if (skip >= songArray.size()) {
                                Main.output("ERROR: You do not have enough friends with music "
                                                + "to meet the song request quota of: "
                                                + songsToFetch
                                                + "\nPlease try again with a lower number.");
                                return allSongs;
                            }
                            continue;
                        }

                        Song song = new Song();
                        JSONObject currentSong = (JSONObject) currentObject;

                        song.setTitle(currentSong.get("name").toString()); // fetch song name
                        currentSong = (JSONObject) currentSong.get("artist");
                        song.setArtist(currentSong.get("name").toString()); // fetch artist name
                        allSongs.add(song);

                        // print detailed information to console
                        Main.output("Friend: " + currentFriend + " // Song " + allSongs.size()
                                        + ": " + song.getArtist() + " - " + song.getTitle());

                        writer.println("Song: " + allSongs.size() + " from " + currentFriend + "\n"
                                        + song.toString() + "\n");

                        if (allSongs.size() >= songsToFetch) {
                            // in the case where songsToFetch is an odd number we don't want to
                            // fetch an extra song
                            fetchedInfo = "Last.fm friends music";
                            writer.close();
                            client.close();
                            return allSongs;
                        }

                        count++;
                        if (count == songsFromEachUser) {
                            break;
                        }

                        TimeUnit.MILLISECONDS.sleep(50);
                    }
                }
                iteration++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        fetchedInfo = "Last.fm friends music";
        client.close();
        writer.close();
        return allSongs;

    }

    /**
     * Method to fetch a list of songs similar to a specific artist
     * 
     * @param artistName the artist of which to find similar music
     * @param songsToFetch the number of songs you would like to retrieve
     * @return an ArrayList of Song objects that are similar to the artist specified
     * @throws FailingHttpStatusCodeException
     */
    public ArrayList<Song> fetchSimilar(String artistName, int songsToFetch)
                    throws FailingHttpStatusCodeException {
        artistName = artistName.trim();
        Main.output("Fetching songs similar to " + artistName);
        File output = new File(strDate + "-similar.txt"); // keep local txt file as well
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        ArrayList<Song> allSongs = new ArrayList<Song>();

        ArrayList<String> uniqueChoices = generateUniqueChoices(songsToFetch);

        try {
            uri = new URIBuilder().setScheme("http").setHost("ws.audioscrobbler.com/")
                            .setPath("2.0/").setParameter("method", "artist.getsimilar")
                            .setParameter("artist", artistName).setParameter("api_key", API_KEY)
                            .setParameter("format", "json").build();
            httpGet = new HttpGet(uri);
            response = httpClient.execute(httpGet);
            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);

            // convert large piece of json to an array of artists
            jsonObj = (JSONObject) jsonObj.get("similarartists");
            if (jsonObj == null) {
                Main.output("ERROR: Could not find any artists similar to " + artistName
                                + "\nCheck spelling and try again.");
                return allSongs;
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
                return allSongs;
            }

            // loop through random artists and fetch their top songs
            for (int i = 0; i < songsToFetch; i++) {

                String[] tokens = uniqueChoices.get(i).split("-");
                int randomArtist = (int) Integer.parseInt(tokens[0].trim());
                int randomSong = (int) Integer.parseInt(tokens[1].trim());

                try {
                    JSONObject js = (JSONObject) artistArray.get(randomArtist);
                    String strArtist = js.get("name").toString();

                    uri = new URIBuilder().setScheme("http").setHost("ws.audioscrobbler.com/")
                                    .setPath("2.0/").setParameter("method", "artist.gettoptracks")
                                    .setParameter("artist", strArtist)
                                    .setParameter("api_key", API_KEY).setParameter("format", "json")
                                    .build();

                    httpGet = new HttpGet(uri);
                    response = httpClient.execute(httpGet);
                    jsonResponse = EntityUtils.toString(response.getEntity());
                    jsonObj = (JSONObject) jsonParse.parse(jsonResponse);

                    // convert large piece of json to an array of artists
                    jsonObj = (JSONObject) jsonObj.get("toptracks");
                    JSONArray songArray = (JSONArray) jsonObj.get("track");

                    JSONObject j2 = (JSONObject) songArray.get(randomSong);
                    String strTitle = j2.get("name").toString();

                    // create song object
                    Song song = new Song();
                    song.setTitle(strTitle);
                    song.setArtist(strArtist);
                    // get genre by clicking on song link and grabbings tags (will slow it down,tho)

                    allSongs.add(song);
                    // print detailed information to console
                    Main.output("Song " + (i + 1) + ": " + song.getArtist() + " - "
                                    + song.getTitle());

                    writer.println("Song: " + (i + 1) + "\n" + song.toString() + "\n");
                } catch (IndexOutOfBoundsException e) {
                    Main.output("Narrowing search parameters to meet measly number of similar "
                                    + "artists and songs...");
                    topArtists = artistArray.size();
                    topSongs -= 1; // just to make sure :))
                    allSongs.addAll(fetchSimilar(artistName, (songsToFetch - i)));
                    writer.close();
                    fetchedInfo = "Similar Artists to: " + artistName;
                    return allSongs;
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        fetchedInfo = "Similar Artists to: " + artistName;
        writer.close();

        return allSongs;

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
    public ArrayList<Song> fetch(int songsToFetch) throws FailingHttpStatusCodeException {

        Main.output("Fetching from Last.fm suggested tracks: ");

        ArrayList<Song> allSongs = new ArrayList<Song>();
        File output = new File(strDate + "-last-fm.txt"); // keep local txt file as well
        PrintWriter writer = null;

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

            // create song object
            song = new Song();
            song.setTitle(strTitle);
            song.setArtist(strArtist);
            // get genre by clicking on song link and grabbings tags (will slow it down,tho)

            allSongs.add(song);

            // print detailed information to console
            Main.output("Song " + songPos + ": " + song.getArtist() + " - " + song.getTitle());
            writer.println("Song: " + songPos + "\n" + song.toString() + "\n");
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (songPos == songsToFetch) {
                break;
            } else {
                songPos++;
            }
        }

        client.close();
        writer.close();

        fetchedInfo = "Last.fm";
        return allSongs;

    }

    /**
     * Helper method to create unique combinations of artists and songs. Top artists and top songs
     * can be set by the global variables inside the LastFmScraper class
     * 
     * @param songsToFetch the number of songs to be fetched from the fetch() method
     * @return a unique list of combinations in the formation <artist>-<song#>
     */
    private ArrayList<String> generateUniqueChoices(int songsToFetch) {
        ArrayList<String> uniqueChoices = new ArrayList<String>();

        // create list of unique combinations, chosen from random numbers which correspond to
        // <artist>-<track#>
        for (int i = 0; i < songsToFetch; i++) {
            int artistTest = (int) (Math.random() * topArtists);
            int songTest = (int) (Math.random() * topSongs);
            String choice = (String) (Integer.toString(artistTest) + "-"
                            + Integer.toString(songTest));

            // make sure choices are unique -- flip a coin and re-roll if not
            while (uniqueChoices.contains(choice)) {
                // flip a coin
                int coin = (int) Math.random() * 2;
                if (coin == 0) {
                    songTest = (int) (Math.random() * topSongs);
                } else {
                    artistTest = (int) (Math.random() * topArtists);
                }
                choice = (String) (Integer.toString(artistTest) + "-" + Integer.toString(songTest));
            }

            uniqueChoices.add(choice);
        }

        return uniqueChoices;
    }

    /**
     * Helper method to verify the username and password are valid. This method is protected, but
     * not private, so that it can be used outside of the fetch() method, so that a user does not
     * waste time submitting a request just to be shut down by invalid user credentials. This method
     * does, however, NEED to be called before using the fetch() method, as it sets the user's
     * credentials in the LastFm object
     * 
     * @return true if the credentials are valid, false otherwise
     */
    @SuppressWarnings("unchecked")
    protected boolean verifyUserNamePassword() {
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
}

