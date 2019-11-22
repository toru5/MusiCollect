package edu.wisc.cs.scraping_tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Class used to find Song objects in the Spotify database, record their URI, then add them to a
 * user's playlist. The class also handles all authentication that Spotify requires.
 * 
 * @author Zach Kremer
 *
 */
public class SpotifyScraper {
    private static CloseableHttpClient client;
    private static HttpGet httpGet;
    private static HttpPost httpPost;
    private static CloseableHttpResponse response;
    private static URI uri;
    private static URI redirectUri;
    private static String jsonResponse = null;
    private static JSONObject jsonObj = null;
    private static JSONParser jsonParse = new JSONParser();
    private static String code;
    private static String token;
    private static String refreshToken; // not used yet
    private static String userId;
    static final String CLIENT_ID = "f0d4411e78e74a61ac8b205bd3d21b61";
    static final String CLIENT_SECRET = "264bb63e909b44afa6cec4fe52238174";
    static int PORT_NUMBER = 8080;

    /**
     * Method for retrieving music from spotify. Will return an exact match, if found. Keeps track
     * of spotify API rate-limiting and pauses if needed, as this method seems to be solely
     * responsible for overriding the limit
     * 
     * @param artist artist identifier
     * @param track the track title
     * @return a valid spotify URI ID of the song, if found, null otherwise
     */
    private static String search(String artist, String track) {
        String uriId = null;
        String rateLimit;
        try {
            uri = new URIBuilder().setScheme("https").setHost("api.spotify.com")
                            .setPath("v1/search")
                            .setParameter("q", "artist:" + artist + " track:" + track)
                            .setParameter("type", "track").setParameter("limit", "1").build();

            httpGet = new HttpGet(uri);
            httpGet.addHeader("Authorization", "Bearer " + token);
            response = client.execute(httpGet);

            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            if (response.getStatusLine().getStatusCode() == 429) {
                rateLimit = response.getFirstHeader("Retry-After").toString();
                String[] tokens = rateLimit.split(":");
                int timeout = (int) Integer.parseInt(tokens[1].trim());
                Main.printLine("API RATE LIMIT EXCEEDED // Pausing for " + timeout + " second(s).");
                TimeUnit.SECONDS.sleep(timeout);
                return search(artist, track); // recursively retry
            }

            jsonObj = (JSONObject) jsonObj.get("tracks");
            if (jsonObj == null) {
                return null;
            }
            JSONArray songArray = (JSONArray) jsonObj.get("items");
            for (Object j : songArray) {
                JSONObject j2 = (JSONObject) j;
                uriId = "spotify:track:" + j2.get("id").toString();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return uriId;
    }

    /**
     * Method that opens a user's default browser with a prompt to authorize MusiCollect to make
     * changes to their Spotify account. Data is transmitted over a socket connection. Upon
     * authorization, the connection will terminate and the token will be used for further API
     * calls.
     * 
     * Future fix: need to print out an informational message in the browser that tells the user
     * that the authentication was sucessful and that they can return to the program -- right now it
     * just says that the connection failed.
     */
    private static boolean authenticate(int portNumber) {
        client = HttpClients.createDefault();
        PrintWriter webOut = null;
        ServerSocket serverSocket = null;
        Socket socket = null;
        
        try {
            redirectUri = new URIBuilder().setScheme("http").setHost("localhost:" + portNumber)
                            .setPath("Callback").build();
            uri = new URIBuilder().setScheme("https").setHost("accounts.spotify.com")
                            .setPath("authorize").setParameter("client_id", CLIENT_ID)
                            .setParameter("response_type", "code")
                            .setParameter("redirect_uri", redirectUri.toString())
                            .setParameter("scope", "playlist-modify-public playlist-modify-private")
                            .build();
            httpGet = new HttpGet(uri);
            response = client.execute(httpGet);

            serverSocket = new ServerSocket(portNumber);
            Main.openWebpage(uri);
            socket = serverSocket.accept();
            InputStreamReader input = new InputStreamReader(socket.getInputStream());
            BufferedReader info = new BufferedReader(input);
            String s = null;
            while ((s = info.readLine()) != null) {
                if (s.contains("code")) {
                    int i = s.indexOf("code");
                    int e = s.indexOf("HTTP");
                    code = s.substring(i + 5, e - 1); // extract code from substring
                    break;
                }
            }

            // code was successfully read -- print output in tab letting user know
            webOut = new PrintWriter(socket.getOutputStream());
            webOut.println("HTTP/1.1 200 OK");
            webOut.println("Content-Type: text/html");
            webOut.println("\r\n");
            webOut.println("<p>Verification code received successfully. You may now close this tab.</p>");

            webOut.close();
            socket.close();
            serverSocket.close();
            // Main.output("Verification code successfully received. Adding songs to playlist...");

            // set body of POST method and encode in x-www-url format
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
            pairs.add(new BasicNameValuePair("code", code));
            pairs.add(new BasicNameValuePair("redirect_uri", redirectUri.toString()));
            pairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
            pairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));

            httpPost = new HttpPost("https://accounts.spotify.com/api/token");
            httpPost.setEntity(new UrlEncodedFormEntity(pairs));

            response = client.execute(httpPost);
            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            token = jsonObj.get("access_token").toString();
            refreshToken = jsonObj.get("refresh_token").toString();

            httpGet = new HttpGet("https://api.spotify.com/v1/me");
            httpGet.addHeader("Authorization", "Bearer " + token);
            response = client.execute(httpGet);
            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            userId = jsonObj.get("id").toString();

        } catch (Exception e) {
            e.printStackTrace();
            webOut.close();
            try {
                socket.close();
                serverSocket.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            return false;
        }
        return true;
    }

    /**
     * Method for creating a playlist on Spotify. References the Search() method to retrieve valid
     * URI IDs of songs
     * 
     * @param songs the list of songs to add to the playlist
     * @param playlistName the name to give to the new playlist
     * @return a valid spotify playlist ID
     */
    public static String createPlaylist(ArrayList<Song> songs, String playlistName) {

        if (!authenticateHelper()) {
            return null;
        }

        ArrayList<String> ids;
        String playlistId = null;

        
        httpPost = new HttpPost("https://api.spotify.com/v1/users/" + userId + "/playlists");
        httpPost.addHeader("Authorization", "Bearer " + token);
        httpPost.addHeader("Content-type", "application/json");

        try {
            // create private playlist
            httpPost.setEntity(new StringEntity(
                            "{\"name\":\"" + playlistName + "\",\"public\":\"false\"}"));
            response = client.execute(httpPost);
            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            playlistId = jsonObj.get("id").toString();

            httpGet = new HttpGet(uri);
            httpGet.addHeader("Authorization", "Bearer " + token);
            response = client.execute(httpGet);

            int chunk = 100; // post in chunks
            int songsNotFound = 0;

            int iterations;

            if (songs.size() % 100 == 0) {
                iterations = (songs.size() / chunk) - 1;
            } else {
                iterations = songs.size() / chunk;
            }

            for (int i = 0; i <= iterations; i++) {
                ids = new ArrayList<String>();
                for (int j = 0; j < chunk; j++) {
                    String uriId = search(songs.get(j + (i * chunk)).getArtist(),
                                    songs.get(j + (i * chunk)).getTitle());
                    if (uriId != null) {
                        ids.add(uriId);
                        Main.printLine("Adding playlist item " + (j + (i * chunk)) + ": "
                                        + songs.get(j + (i * chunk)).getArtist() + " - "
                                        + songs.get(j + i).getTitle());
                    } else {
                        songsNotFound++;
                    }

                    if ((j + (i * chunk)) == songs.size() - 1) {
                        break;
                    }
                }

                String bigJsonString = "{\"uris\":[";
                for (String s : ids) {
                    bigJsonString += "\"" + s + "\",";
                }
                bigJsonString = bigJsonString.substring(0, bigJsonString.length() - 1) + "]}";

                httpPost = new HttpPost(
                                "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks");
                httpPost.setEntity(new StringEntity(bigJsonString));
                httpPost.addHeader("Authorization", "Bearer " + token);
                response = client.execute(httpPost);
                jsonResponse = EntityUtils.toString(response.getEntity());
            }
            Main.printLine(songs.size() - songsNotFound + " songs successfully added to playlist\n"
                            + songsNotFound + " songs not found on Spotify");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return playlistId;
    }

    /**
     * Method that converts a spotify playlist into a list of Song Objects that can be further
     * converted if so desired.
     * 
     * @param playlistID Spotify playlist ID (can be obtained by getting the URI and locating the
     *        section that says playlist:
     * @return a list of song objects
     */
    public static ArrayList<Song> playlistToSongObjects(String playlistID) {
        ArrayList<Song> results = new ArrayList<Song>();
        if (!authenticateHelper()) {
            return null;
        }
        
        int limit = 100; // max limit set by spotify
        int offset = 0;
        int totalSongs;
        try {
            uri = new URIBuilder().setScheme("https").setHost("api.spotify.com")
                            .setPath("v1/playlists/" + playlistID + "/tracks")
                            .setParameter("fields", "total,items(track(name, artists(name)))")
                            .setParameter("limit", Integer.toString(limit))
                            .setParameter("offset", Integer.toString(offset)).build();

            httpGet = new HttpGet(uri);
            httpGet.addHeader("Authorization", "Bearer " + token);
            response = client.execute(httpGet);

            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            totalSongs = Integer.parseInt(jsonObj.get("total").toString());

            if (jsonObj == null) {
                return null;
            }

            JSONArray songArray = (JSONArray) jsonObj.get("items");

            Main.printLine("Collecting music from your playlist... this may take a moment.");

            for (Object j : songArray) {
                JSONObject j2 = (JSONObject) j;
                JSONObject j3 = (JSONObject) j2.get("track");
                JSONArray j4 = (JSONArray) j3.get("artists");
                JSONObject artistJson = (JSONObject) j4.get(0);

                results.add(new Song(artistJson.get("name").toString(), j3.get("name").toString()));
            }

            int iterations;

            if (totalSongs < 100) {
                iterations = 0; // no need to add more songs
            } else {
                iterations = ((totalSongs - 100) / limit) + 1; // loop through and add remaining
                                                               // songs
            }

            for (int i = 0; i < iterations; i++) {
                offset += 100;
                uri = new URIBuilder().setScheme("https").setHost("api.spotify.com")
                                .setPath("v1/playlists/" + playlistID + "/tracks")
                                .setParameter("fields", "total,items(track(name, artists(name)))")
                                .setParameter("limit", Integer.toString(limit))
                                .setParameter("offset", Integer.toString(offset)).build();

                httpGet = new HttpGet(uri);
                httpGet.addHeader("Authorization", "Bearer " + token);
                response = client.execute(httpGet);

                jsonResponse = EntityUtils.toString(response.getEntity());
                jsonObj = (JSONObject) jsonParse.parse(jsonResponse);

                if (jsonObj == null) {
                    return null;
                }

                JSONArray songArrayInner = (JSONArray) jsonObj.get("items");
                for (Object j : songArrayInner) {
                    JSONObject j2 = (JSONObject) j;
                    JSONObject j3 = (JSONObject) j2.get("track");
                    JSONArray j4 = (JSONArray) j3.get("artists");
                    JSONObject artistJson = (JSONObject) j4.get(0);

                    results.add(new Song(artistJson.get("name").toString(),
                                    j3.get("name").toString()));
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * Small helper method to attempt authentication on ports 8080-8089
     * @return true if authentication succeeds or false if authentication fails.
     */
    private static boolean authenticateHelper() {
        if (!authenticate(PORT_NUMBER)) {
            for (int i = 0; i < 9;) {
                Main.printLine("Activity detected on port " + PORT_NUMBER + ".\n"
                                + "Attempting to connect on" + (PORT_NUMBER + 1));
                if (authenticate(++PORT_NUMBER)) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

}
