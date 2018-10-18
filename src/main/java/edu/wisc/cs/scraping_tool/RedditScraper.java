package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;

public class RedditScraper {
    private RestClient restClient;
    private User user;

    // used for playlist name
    String fetchedInfo = "";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);

    private void connect() {
        // Initialize REST Client
        restClient = new HttpRestClient();
        restClient.setUserAgent("musiCollect 1.0/toru5/");

        // Connect the user
        user = new User(restClient, "toru5", "peopleonthego2");
    }

    public List<Submission> getTopSubmissions(String subreddit, int songsToFetch) {
        connect();
        Submissions subs = new Submissions(restClient, user);

        List<Submission> subsOfSub = subs.ofSubreddit(subreddit, SubmissionSort.TOP, -1,
                        songsToFetch, null, null, true);
        

        return subsOfSub;
    }

    public ArrayList<String> fetch(String subreddit, int songsToFetch, int minUpvotes) {
        List<Submission> songs = getTopSubmissions(subreddit, songsToFetch);
        System.out.println("Fetching from: reddit/r/" + subreddit);

        File output = new File(strDate + "-reddit-" + subreddit + "-songs.txt"); // keep local txt
                                                                                 // file as well
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(output);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        ArrayList<String> allVideoIds = new ArrayList<String>();
        int count = 0;
        for (Submission s : songs) {
            String title = "";
            String artist = "";
            String genre = "";

            if (s.getUpVotes() >= minUpvotes) {
                if (s.getTitle().contains("-")) {
                    String[] tokens = s.getTitle().split("-");
                    artist = tokens[0];
                    title = tokens[1];
                    if (title.equals("")) {
                        title = tokens[2];
                    }

                    if (title.contains("[") && title.contains("]")) {
                        tokens = title.split("\\[");
                        title = tokens[0];
                        tokens = tokens[1].split("\\]");
                        genre = tokens[0];
                    } else {
                        tokens = title.split("\\[");
                        title = tokens[0];
                    }

                    title = title.trim();
                    artist = artist.trim();

                }
            } else {
                count++;
                continue;
            }

            if (title.equals("") || artist.equals("")) {
                continue;               
            }
            
            if (genre.equals("")) {
                genre = "No Genre";
            }
            
            YouTubeScraper y = new YouTubeScraper();

            List<String> videoIds;


            // searches for the song on youtube, returns the first video, if nothing is
            // found, the program narrows down the search by shedding the "artist"
            // parameter
            try {
                videoIds = y.search(artist, title, 1); // fetch 1 video
            } catch (NoSuchElementException e) {
                // System.out.println("No videos found for: " + artist + " " + title
                //                + "\nSearching for: " + title);
                try {
                    videoIds = y.search("", title, 1); // simplify parameters
                } catch (NoSuchElementException ee) {
                    // System.out.println(
                    //                "Error finding song: " + title + "\nSkipping to next track...");
                    break;
                }
            }

            // generate links to videos and playlists
            String strYouTubeLink = YouTubeScraper.generateLink(videoIds.get(0));
            String strYouTubeEmbedLink = YouTubeScraper.generateEmbedLink(videoIds.get(0));
            allVideoIds.add(videoIds.get(0));

            // create song object
            Song song = new Song();

            song.setTitle(title);
            song.setArtist(artist);
            song.setGenre(genre);
            song.setYoutubeLink(strYouTubeLink);
            song.setYoutubeEmbedLink(strYouTubeEmbedLink);

            // print detailed information to console
            System.out.println(song.getGenre() + " - Song " + (count + 1) + ": " + song.getArtist()
                            + " - " + song.getTitle());
            writer.println("Song: " + (count + 1) + "\n" + song.toString() + "\n");
            count++;

        }

//        fetchedInfo = "Reddit" + subreddit + " (Top " + songsToFetch + " songs)";
        fetchedInfo = "reddit/" + subreddit;
        writer.close();
        return allVideoIds;
    }

    public String getFetchedInfo() {
        return fetchedInfo;
    }


    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }
}
