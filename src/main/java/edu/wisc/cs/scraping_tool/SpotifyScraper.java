package edu.wisc.cs.scraping_tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    private static String refreshToken;
    private static String userId;
    static final String CLIENT_ID = "f0d4411e78e74a61ac8b205bd3d21b61";
    static final String CLIENT_SECRET = "264bb63e909b44afa6cec4fe52238174";

    private static void authenticate() {
        client = HttpClients.createDefault();

        try {
            redirectUri = new URIBuilder().setScheme("http").setHost("localhost:8080")
                            .setPath("Callback").build();
            uri = new URIBuilder().setScheme("https").setHost("accounts.spotify.com")
                            .setPath("authorize").setParameter("client_id", CLIENT_ID)
                            .setParameter("response_type", "code")
                            .setParameter("redirect_uri", redirectUri.toString())
                            .setParameter("scope", "playlist-modify-public playlist-modify-private")
                            .build();
            httpGet = new HttpGet(uri);
            response = client.execute(httpGet);

            ServerSocket serverSocket = new ServerSocket(8080);
            Main.openWebpage(uri);
            Socket socket = serverSocket.accept();
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
            socket.close();
            serverSocket.close();
            Main.output("Verification code successfully received.");

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
        }
    }


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
                Main.output("API RATE LIMIT EXCEEDED // Pausing for " + timeout + " second(s).");
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
     * Method for creating a playlist on Spotify. References the Search() method to retrieve valid
     * URI IDs of songs
     * 
     * @param songs the list of songs to add to the playlist
     * @param playlistName the name to give to the new playlist
     * @return a valid spotify playlist ID
     */
    public static String createPlaylist(ArrayList<Song> songs, String playlistName) {
        authenticate();

        ArrayList<String> ids;
        String playlistId = null;

        httpPost = new HttpPost("https://api.spotify.com/v1/users/" + userId + "/playlists");
        httpPost.addHeader("Authorization", "Bearer " + token);
        httpPost.addHeader("Content-type", "application/json");

        try {
            // create private playlist
            httpPost.setEntity(new StringEntity("{\"name\":\"" + playlistName + "\",\"public\":\"false\"}"));
            response = client.execute(httpPost);
            jsonResponse = EntityUtils.toString(response.getEntity());
            jsonObj = (JSONObject) jsonParse.parse(jsonResponse);
            playlistId = jsonObj.get("id").toString();


            httpGet = new HttpGet(uri);
            httpGet.addHeader("Authorization", "Bearer " + token);
            response = client.execute(httpGet);

            int chunk = 100; // post in chunks
            int songsNotFound = 0;
            int iterations = 0;

            // do the POST in chunks
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
                        Main.output("Adding PlaylistItem " + (j + (i * chunk) + 1) + ": "
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
            Main.output(songs.size() - songsNotFound + " songs successfully added to playlist\n"
                            + songsNotFound + " songs not found on Spotify");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return playlistId;
    }

}