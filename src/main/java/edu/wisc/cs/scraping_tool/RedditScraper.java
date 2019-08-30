package edu.wisc.cs.scraping_tool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;

/**
 * Class that fetches music (by API calls from a reddit library) from any subreddit on reddit.com
 * 
 * @author Zach Kremer
 *
 */
public class RedditScraper {
    private RestClient restClient;
    private User user;

    // used for playlist name
    String fetchedInfo = "";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);

    /**
     * Simple method for connecting to a RestClient this is also the method where user credentials
     * must be set
     */
    private void connect() {
        // Initialize REST Client
        restClient = new HttpRestClient();
        restClient.setUserAgent("musiCollect 1.0/toru5/");

        // Connect the user
        user = new User(restClient, "korvaxe", "musiCollect1");
    }

    /**
     * Fetching method that retrieves a list of (up to) 100 songs from any valid subreddit
     * 
     * @param subreddit the subreddit string to use for scraping
     * @param songsToFetch the song quota
     * @param minUpvotes a minimum amount of upvotes to justify fetching the object
     * @return a list of Song objects
     */
    public ArrayList<Song> fetch(String subreddit, int songsToFetch, int minUpvotes) {
        Main.output("Fetching from: reddit/r/" + subreddit);
        connect();
        Submissions subs = new Submissions(restClient, user);

        final int redditListingLimit = 100;

        List<Submission> submissions = null;
        ArrayList<Song> allSongs = new ArrayList<Song>();
        int songCount = 0;
        Submission after = null; // used when we can't meet the song requirement in the first 100
                                 // subs
        boolean lastCall = false;

        while (songCount < songsToFetch && !lastCall) {
            submissions = subs.ofSubreddit(subreddit, SubmissionSort.HOT, -1, redditListingLimit,
                            after, null, true);

            if (submissions.size() < redditListingLimit) {
                lastCall = true; // when < 100 songs are retrieved it means the reddit API can no
                                 // longer fetch songs after this
            }

            for (Submission s : submissions) {

                after = s; // set after to be the last submission

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

                        // attempt to collect genre info
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

                        // only increment count if we correctly find a song with the minimum
                        // number of
                        // upvotes
                        songCount++;
                    }

                }

                if (title.equals("") || artist.equals("")) {
                    // do nothing
                } else {

                    if (genre.equals("")) {
                        genre = "Unknown Genre";
                    }

                    // create song object
                    Song song = new Song();

                    song.setTitle(title);
                    song.setArtist(artist);
                    song.setGenre(genre);

                    allSongs.add(song);

                    // print detailed information to console
                    Main.output("Song " + songCount + ": " + song.getArtist()
                                    + " - " + song.getTitle() + " [" + song.getGenre() + "]");
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (songCount >= songsToFetch) {
                        break;
                    }
                }
            }

        }

        if (songCount < songsToFetch) {
            Main.output("\n*** Sorry, we could only find " + songCount + " songs with over "
                            + minUpvotes + " minimum upvotes on /r/" + subreddit
                            + ".  This is due to limits imposed by the reddit API.  "
                            + "For more listings, try reducing the minimum number of upvotes. ***\n");
        }

        fetchedInfo = "reddit/" + subreddit;
        return allSongs;
    }

    /**
     * simple getter method
     * 
     * @return fetchedInfo
     */
    public String getFetchedInfo() {
        return fetchedInfo;
    }


    /**
     * simple setter method
     * 
     * @param fetchedInfo
     */
    public void setFetchedInfo(String fetchedInfo) {
        this.fetchedInfo = fetchedInfo;
    }
}
