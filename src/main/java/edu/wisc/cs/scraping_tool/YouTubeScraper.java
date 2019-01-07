package edu.wisc.cs.scraping_tool;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;
import com.google.api.services.youtube.YouTube;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class YouTubeScraper {


    private static YouTube youtube;

    /**
     * 
     * @param videoIds video ids of the videos you would like to add to the playlist
     * @param playlistName the name of the playlist to be created
     * @return returns the playlistid for the playlist
     */
    public static String createPlaylist(List<String> videoIds, String playlistName) {
        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");
        String playlistId = "";

        try {
            // Authorize the request.
            Credential credential = Auth.authorize(scopes, "playlistupdates");
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                            .setApplicationName("MusiCollect").build();

            // Create a new, private playlist in the authorized user's channel.
            playlistId = insertPlaylist(playlistName);

            for (String vId : videoIds) {
                insertPlaylistItem(playlistId, vId);
            }

        } catch (GoogleJsonResponseException e) {
            // System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
            // + e.getDetails().getMessage());
//            e.printStackTrace();
            System.out.println("GoogleJsonResponseException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
//            e.printStackTrace();
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
        }
        
        return playlistId;
    }

    public List<String> search(String artistName, String songTitle, long numberOfVideosReturned)
                    throws NoSuchElementException {
        List<String> ids = null;

        try {
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),
                            new HttpRequestInitializer() {
                                public void initialize(HttpRequest request) throws IOException {

                            }
                            }).setApplicationName("MusiCollect").build();

            // String queryTerm = getInputQuery();
            String queryTerm = artistName + " " + songTitle;

            YouTube.Search.List search = youtube.search().list("id,snippet");

            String apiKey = "AIzaSyCOGuwJIL9iX-7zDfcposMkkuJwst1AXY8";
            search.setKey(apiKey);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields(
                            "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(numberOfVideosReturned);

            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                ids = getVideoIds(searchResultList.iterator());
            }

        } catch (GoogleJsonResponseException e) {
            System.out.println("There was a service error: " + e.getDetails().getCode() + " : "
                            + e.getDetails().getMessage());
        } catch (IOException e) {
            System.out.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
        }


        if (ids.isEmpty()) {
            throw new NoSuchElementException();
        }

        return ids;
    }

    /*
     * Prompt the user to enter a query term and return the user-specified term.
     */
    private static String getInputQuery() throws IOException {

        String inputQuery = "";

        System.out.print("Please enter a search term: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        inputQuery = bReader.readLine();

        if (inputQuery.length() < 1) {
            // Use the string "YouTube Developers Live" as a default.
            inputQuery = "YouTube Developers Live";
        }

        return inputQuery;
    }

    /**
     * 
     * @param iteratorSearchResults
     * @return
     */
    private static ArrayList<String> getVideoIds(Iterator<SearchResult> iteratorSearchResults) {
        ArrayList<String> videoIds = new ArrayList<String>();

        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();
            videoIds.add(rId.getVideoId());
        }

        return videoIds;
    }


    /**
     * 
     * @param videoId
     * @return
     */
    public static String generateLink(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    /**
     * 
     * @param videoId
     * @return
     */
    public static String generateEmbedLink(String videoId) {
        return "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + videoId
                        + "\" frameborder=\"0\" allow=\"autoplay; encrypted-media\" allowfullscreen></iframe>";
    }

    /*
     * Prints out all results in the Iterator. For each result, print the title, video ID, and
     * thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query Search query (String)
     */
    private static void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query) {
        int count = 1;
        System.out.println("\n=============================================================");
        System.out.println("   First " + count + " videos for search on \"" + query + "\".");
        System.out.println("=============================================================\n");

        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();

                System.out.println(" Video Id: " + rId.getVideoId());
                System.out.println(" Title: " + singleVideo.getSnippet().getTitle());
                System.out.println(" Thumbnail: " + thumbnail.getUrl());
                System.out.println(
                                "\n-------------------------------------------------------------\n");
            }
            count++;
        }
    }

    /**
     * Create a playlist and add it to the authorized account.
     */
    private static String insertPlaylist(String playlistName) throws IOException {

        String pId = "";
        if (playlistName.length() > 100) {
            playlistName = playlistName.substring(0, 99);
        }

        try {
            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put("part", "snippet,status,id");
            parameters.put("onBehalfOfContentOwner", "");


            Playlist playlist = new Playlist();
            PlaylistSnippet snippet = new PlaylistSnippet();
            PlaylistStatus status = new PlaylistStatus();
            snippet.setTitle(playlistName);
            playlist.setSnippet(snippet);
            playlist.setStatus(status);


            YouTube.Playlists.Insert playlistsInsertRequest =
                            youtube.playlists().insert(parameters.get("part").toString(), playlist);

            if (parameters.containsKey("onBehalfOfContentOwner")
                            && parameters.get("onBehalfOfContentOwner") != "") {
                playlistsInsertRequest.setOnBehalfOfContentOwner(
                                parameters.get("onBehalfOfContentOwner").toString());
            }

            Playlist response = playlistsInsertRequest.execute();

            pId = response.getId();

        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                            + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return pId;
    }

    /**
     * Create a playlist item with the specified video ID and add it to the specified playlist.
     *
     * @param playlistId assign to newly created playlistitem
     * @param videoId YouTube video id to add to playlistitem
     */
    private static String insertPlaylistItem(String playlistId, String videoId) throws IOException {

        // Define a resourceId that identifies the video being added to the
        // playlist.
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);

        // Set fields included in the playlistItem resource's "snippet" part.
        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setPlaylistId(playlistId);
        // playlistItemSnippet.setTitle("Beatport Results");
        playlistItemSnippet.setResourceId(resourceId);

        // Create the playlistItem resource and set its snippet to the
        // object created above.
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);

        // Call the API to add the playlist item to the specified playlist.
        // In the API call, the first argument identifies the resource parts
        // that the API response should contain, and the second argument is
        // the playlist item being inserted.
        YouTube.PlaylistItems.Insert playlistItemsInsertCommand =
                        youtube.playlistItems().insert("snippet,contentDetails", playlistItem);
        PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand.execute();

        // Print data from the API response and return the new playlist
        // item's unique playlistItem ID.

        try {
        System.out.println(
                        "Adding PlaylistItem: " + returnedPlaylistItem.getSnippet().getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // extra details (potentially) not needed for the user
        // System.out.println(" - Video id: "
        // + returnedPlaylistItem.getSnippet().getResourceId().getVideoId());
        // System.out.println(" - Posted: " + returnedPlaylistItem.getSnippet().getPublishedAt());
        // System.out.println(" - Channel: " + returnedPlaylistItem.getSnippet().getChannelId());
        return returnedPlaylistItem.getId();

    }
}
