package edu.wisc.cs.scraping_tool;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.guigarage.flatterfx.*;

/**
 * Main class that launches the GUI and handles requests made by the user.
 * 
 * @author Zach Kremer
 *
 */
public class Main extends Application {

    static HashMap<String, Genre> bpGenreMap = new HashMap<String, Genre>();
    static HashMap<String, Genre> billGenreMap = new HashMap<String, Genre>();

    ArrayList<CheckBox> bpGenreChecks = new ArrayList<CheckBox>();
    ArrayList<CheckBox> billboardGenreChecks = new ArrayList<CheckBox>();

    ArrayList<String> bpUserGenres;
    ArrayList<String> billUserGenres;

    ArrayList<Song> allSongs;

    // initialize scrapers
    BeatportScraper beatportScraper;
    IndieShuffleScraper indieShuffleScraper;
    Billboard100Scraper billboardScraper;
    RedditScraper redditScraper;
    LastFmScraper lastfmScraper;
    HypeMachineScraper hypeMachineScraper;

    String playlistId = "";

    // used for playlist name
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    static Date date = new Date();
    static String strDate = sdf.format(date);

    String playListName = "";

    // textfields
    TextField bpSongsToFetch = new TextField();
    TextField billSongsToFetch = new TextField();
    TextField indieSongsToFetch = new TextField();
    TextField redditSongsToFetch = new TextField();
    TextField redditMinUpvotes = new TextField();
    TextField uniqueSubreddit = new TextField();
    TextField lastFmSongsToFetch = new TextField();
    TextField lastFmFriendsSongsToFetch = new TextField();
    TextField lastFmUsername = new TextField();
    PasswordField lastFmPassword = new PasswordField();
    TextField similarSongsToFetch = new TextField();
    TextField similarArtists = new TextField();
    TextField spotifyPlaylistID = new TextField();
    TextField hypeMachineSongsToFetch = new TextField();

    static TextArea ta;

    // website checks
    CheckBox bCheck = new CheckBox("Beatport");
    CheckBox iCheck = new CheckBox("Indie Shuffle");
    CheckBox billCheck = new CheckBox("Billboard");
    CheckBox redditCheck = new CheckBox("Reddit");
    CheckBox lastFmCheck = new CheckBox("Last.FM Suggested Tracks");
    CheckBox lastFmFriendsCheck = new CheckBox("Last.FM Friend's Top Tracks");
    CheckBox similarCheck = new CheckBox("Similar Music");
    CheckBox hypeCheck = new CheckBox("Hype Machine");

    // output toggle buttons
    ToggleGroup outputSites = new ToggleGroup();
    ToggleButton youtubeBtn = new ToggleButton("Post playlist to YouTube");
    ToggleButton spotifyBtn = new ToggleButton("Post playlist to Spotify  ");

    // last.fm friend time period buttons
    ToggleGroup lastFmTimePeriods = new ToggleGroup();
    ToggleButton weekBtn = new ToggleButton("Week");
    ToggleButton monthBtn = new ToggleButton("Month");
    ToggleButton yearBtn = new ToggleButton("Year");

    // modifier checks
    CheckBox billRandomCheck = new CheckBox("BILLBOARD Random Song Order?");
    CheckBox bpRandomCheck = new CheckBox("BEATPORT Random Song Order?");
    CheckBox shuffleSongsCheck = new CheckBox("Shuffle Songs in Playlist?");
    CheckBox textFileOutputCheck = new CheckBox("Output detailed text file?");

    class HiddenTextField extends TextField {
        public HiddenTextField() {
            this.setVisible(false);
        }
    }

    /**
     * Standard javafx procedure
     */
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MusiCollect");
        Scene scene = new Scene(setup(), 1400, 800);
        scene.getStylesheets().add("modena.css");
        // setUserAgentStylesheet(STYLESHEET_CASPIAN);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /*
     * Launches the GUI
     * 
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Big method to setup the GUI and event handlers for buttons
     * 
     * @return
     */
    private BorderPane setup() {
        BorderPane root = new BorderPane();

        populateGenres();
        createGenreArray();

        // output business
        ta = new TextArea();
        ta.setEditable(false);
        ta.setPrefWidth(400);
        ta.setWrapText(true);

        // vbox containers
        VBox sites = new VBox(16);
        sites.setPrefWidth(200);
        VBox fetch = new VBox(9);
        fetch.setPrefWidth(250);
        VBox upvotes = new VBox(9);
        upvotes.setPrefWidth(200);
        VBox bpGenres = new VBox();
        bpGenres.setPrefWidth(50);
        VBox billboardGenres = new VBox();
        billboardGenres.setPrefWidth(50);
        VBox outputOptions = new VBox(16);

        GridPane center = new GridPane();
        HBox bottom = new HBox();

        // column headers
        Label col1 = new Label("Websites to scrape:");
        Label col2 = new Label("Songs to fetch:");
        Label col3 = new Label("Additional info");
        Label col4 = new Label("BEATPORT genres to select from:");
        Label col5 = new Label("BILLBOARD genres to select from:");

        Label subredditLbl = new Label("Subreddit to scrape:");
        Label lfmUsername = new Label("Last.fm Username:");
        Label lfmPassword = new Label("Last.fm Password:");

        // prompt text
        bpSongsToFetch.setPromptText("Songs to be fetched (MAX 100)");
        billSongsToFetch.setPromptText("Songs to be fetched (MAX 100)");
        indieSongsToFetch.setPromptText("Songs to be fetched (MAX 15)");
        redditSongsToFetch.setPromptText("Songs to be fetched (MAX 100)");
        redditMinUpvotes.setPromptText("minimum upvotes (defaults to 1)");
        uniqueSubreddit.setText("listentothis");
        lastFmSongsToFetch.setPromptText("Songs to be fetched (MAX 100)");
        lastFmFriendsSongsToFetch.setPromptText("Songs to be fetched");
        lastFmUsername.setPromptText("*required for use with last.fm");
        lastFmPassword.setPromptText("*required for use with last.fm suggested tracks");
        similarSongsToFetch.setPromptText("Songs to be fetched (MAX 200)");
        similarArtists.setPromptText("*artist1; artist2; etc");
        spotifyPlaylistID.setPromptText("Spotify playlist ID or URI to convert");
        hypeMachineSongsToFetch.setPromptText("Songs to be fetched");

        youtubeBtn.setPrefWidth(200);
        spotifyBtn.setPrefWidth(200);
        spotifyBtn.setSelected(true);

        youtubeBtn.setToggleGroup(outputSites);
        spotifyBtn.setToggleGroup(outputSites);

        weekBtn.setToggleGroup(lastFmTimePeriods);
        monthBtn.setToggleGroup(lastFmTimePeriods);
        yearBtn.setToggleGroup(lastFmTimePeriods);
        weekBtn.setSelected(true);
        HBox times = new HBox();
        times.getChildren().addAll(weekBtn, monthBtn, yearBtn);

        // buttons
        final Button submit = new Button("Submit");
        Button exit = new Button("Exit");
        Button stop = new Button("Stop");
        Button spotifyToYouTube = new Button("Convert Spotify Playlist to YouTube");

        exit.setPrefSize(400, 100);
        // stop.setPrefSize(400, 100);
        submit.setPrefSize(400, 100);

        sites.getChildren().addAll(col1, bCheck, iCheck, billCheck, redditCheck, subredditLbl,
                        lastFmCheck, lastFmFriendsCheck, lfmUsername, lfmPassword, similarCheck,
                        hypeCheck, spotifyToYouTube);

        fetch.getChildren().addAll(col2, bpSongsToFetch, indieSongsToFetch, billSongsToFetch,
                        redditSongsToFetch, uniqueSubreddit, lastFmSongsToFetch,
                        lastFmFriendsSongsToFetch, lastFmUsername, lastFmPassword,
                        similarSongsToFetch, hypeMachineSongsToFetch, spotifyPlaylistID);

        upvotes.getChildren().addAll(col3, new HiddenTextField(), new HiddenTextField(),
                        new HiddenTextField(), new HiddenTextField(), redditMinUpvotes,
                        new HiddenTextField(), times, new HiddenTextField(), new HiddenTextField(),
                        similarArtists);
        bpGenres.getChildren().add(col4);
        billboardGenres.getChildren().add(col5);

        bpGenreChecks.sort(new Comparator<CheckBox>() {

            public int compare(CheckBox o1, CheckBox o2) {
                return o1.getText().compareTo(o2.getText());
            }

        });

        billboardGenreChecks.sort(new Comparator<CheckBox>() {

            public int compare(CheckBox o1, CheckBox o2) {
                return o1.getText().compareTo(o2.getText());
            }

        });

        for (CheckBox c : bpGenreChecks) {
            bpGenres.getChildren().add(c);
        }

        for (CheckBox c : billboardGenreChecks) {
            billboardGenres.getChildren().add(c);
        }

        // add items to center container
        center.setHgap(50);
        center.add(sites, 0, 0);
        center.add(fetch, 1, 0);
        center.add(upvotes, 2, 0);
        center.add(bpGenres, 3, 0);
        center.add(bpRandomCheck, 3, 1);
        center.add(billboardGenres, 4, 0);
        center.add(billRandomCheck, 4, 1);

        outputOptions.setPrefWidth(200);
        outputOptions.getChildren().addAll(shuffleSongsCheck, textFileOutputCheck);
        bottom.getChildren().addAll(submit, exit, outputOptions, youtubeBtn, spotifyBtn);

        root.setCenter(center);
        root.setBottom(bottom);

        ScrollPane console = new ScrollPane();
        console.setMaxHeight(2000);
        console.setFitToWidth(true);
        console.setFitToHeight(true);
        console.setContent(ta);
        root.setRight(console);

        spotifyToYouTube.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                // Define a new Runnable
                clearTextArea();
                Runnable convertPlaylist = new Runnable() {
                    public void run() {
                        reset();
                        Main.output("Verifying input parameters..");
                        if (spotifyPlaylistID.getText().equals("")) {
                            Main.output("ERROR: Spotify playlist ID cannot be empty");
                            return;
                        }

                        ArrayList<Song> songs = SpotifyScraper.playlistToSongObjects(
                                        parseSpotifyPlaylistURI(spotifyPlaylistID.getText()));
                        if (songs == null) {
                            Main.output("Error trying to authenticate with Spotify.  "
                                            + "Please try again later or kill the processes "
                                            + "using ports 8080 - 8089");
                            return;
                        }
                        // create youtube playlist from song list
                        YouTubeScraper.createPlaylist(songs, "MusiCollect Converted Playlist");
                    }
                };
                try {

                    Thread submitThread = new Thread(convertPlaylist);
                    submitThread.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        submit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                // Define a new Runnable
                clearTextArea();
                Runnable fetchMusic = new Runnable() {
                    public void run() {
                        reset();
                        Main.output("Verifying input parameters..");
                        // check user input and do not continue if it is invalid
                        if (!verifyInput()) {
                            inputError();
                            return;
                        }

                        Main.output("Initializing request...");

                        try {
                            if (bCheck.isSelected()) {
                                beatportFetch();
                            }

                            if (iCheck.isSelected()) {
                                indieShuffleFetch();
                            }

                            if (billCheck.isSelected()) {
                                billboardFetch();
                            }

                            if (redditCheck.isSelected()) {
                                redditFetch();
                            }

                            if (lastFmCheck.isSelected()) {
                                lastFmFetch();
                            }

                            if (similarCheck.isSelected()) {
                                similarFetch();
                            }

                            if (lastFmFriendsCheck.isSelected()) {
                                lastFmFriendsFetch();
                            }

                            if (hypeCheck.isSelected()) {
                                hypeMachineFetch();
                            }

                        } catch (Exception e) {
                            Main.output(e.getMessage());
                        }
                        if (allSongs.size() != 0) {
                            if (textFileOutputCheck.isSelected()) {
                                printConsole();
                            }

                            if (shuffleSongsCheck.isSelected()) {
                                allSongs = shuffleArray(allSongs);
                            }

                            // shed off extra comma from end
                            playListName = playListName.substring(0, playListName.length() - 2);

                            Main.output("Getting ready to create your playlist\n"
                                            + "Authentication process will begin soon...\nThis can sometimes take a minute...");
                            playListName = "MusiCollect Results - " + strDate + ": " + playListName;

                            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop()
                                            : null;

                            try {
                                if (spotifyBtn.isSelected()) {

                                    playlistId = SpotifyScraper.createPlaylist(allSongs,
                                                    playListName);

                                    if (playlistId == null) {
                                        Main.output("An error occured with Spotify authentication."
                                                        + "  Please ensure port 8080 on your "
                                                        + "computer is not being used and"
                                                        + " try again.");
                                    } else {
                                        Main.output("Fetching complete. Your Spotify playlist can be found"
                                                        + " here: https://open.spotify.com/playlist/"
                                                        + playlistId);
                                        desktop.browse(new URIBuilder().setPath(
                                                        "https://open.spotify.com/playlist/"
                                                                        + playlistId)
                                                        .build());
                                    }


                                } else {
                                    playlistId = YouTubeScraper.createPlaylist(allSongs,
                                                    playListName);

                                    Main.output("Fetching complete. Your YouTube playlist can be found"
                                                    + " here: https://www.youtube.com/playlist?list="
                                                    + playlistId);
                                    desktop.browse(new URI("https://www.youtube.com/playlist?list="
                                                    + playlistId));

                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (URISyntaxException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                };

                try {

                    Thread submitThread = new Thread(fetchMusic);
                    submitThread.start();

                } catch (Exception e) {
                    File output = new File("errors.txt");
                    PrintWriter writer = null;

                    try {
                        writer = new PrintWriter(output);
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    writer.println("EXCEPTION MESSAGE: " + e.getMessage());
                    writer.println("STACK TRACE:\n" + e.getStackTrace());

                    writer.close();
                }
            }

        });

        exit.setOnAction(new EventHandler<ActionEvent>() {

            public void handle(ActionEvent event) {
                System.exit(0);
            }

        });

        return root;

    }

    /**
     * Helper method that fetches from beatport and adds the songs it retrieved to allSongs
     */
    private void beatportFetch() {
        try {
            allSongs.addAll(beatportScraper.fetch(Integer.parseInt(bpSongsToFetch.getText()),
                            bpUserGenres, bpRandomCheck.isSelected(), false));
            playListName += beatportScraper.getFetchedInfo() + ", ";
        } catch (FailingHttpStatusCodeException e) {
            Main.output("HTTP ERROR: Trying again...");
            try {
                allSongs.addAll(beatportScraper.fetch(Integer.parseInt(bpSongsToFetch.getText()),
                                bpUserGenres, bpRandomCheck.isSelected(), false));
                playListName += beatportScraper.getFetchedInfo() + ", ";
            } catch (FailingHttpStatusCodeException e1) {
                Main.output("HTTP ERROR: Exiting Beatport Scraping procedure.");
            }

        }
    }

    /**
     * Helper method that fetches from billboard and adds the songs it retrieved to allSongs
     */
    private void billboardFetch() {
        allSongs.addAll(billboardScraper.fetch(Integer.parseInt(billSongsToFetch.getText()),
                        billUserGenres, billRandomCheck.isSelected()));
        playListName += billboardScraper.getFetchedInfo() + ", ";
    }

    /**
     * Helper method that fetches from indieshuffle and adds the songs it retrieved to allSongs
     */
    private void indieShuffleFetch() {
        allSongs.addAll(indieShuffleScraper.fetch(Integer.parseInt(indieSongsToFetch.getText())));
        playListName += indieShuffleScraper.getFetchedInfo() + ", ";
    }

    /**
     * Helper method that fetches from reddit and adds the songs it retrieved to allSongs
     */
    private void redditFetch() {
        // check if textbox is empty -- if so -- default to 1 min upvote
        if (redditMinUpvotes.getText().equals("")) {
            allSongs.addAll(redditScraper.fetch(
                            uniqueSubreddit.getText().replaceAll("/", "").trim(),
                            Integer.parseInt(redditSongsToFetch.getText()), 1));
        } else {
            allSongs.addAll(redditScraper.fetch(
                            uniqueSubreddit.getText().replaceAll("/", "").trim(),
                            Integer.parseInt(redditSongsToFetch.getText()),
                            Integer.parseInt(redditMinUpvotes.getText())));
        }

        playListName += redditScraper.getFetchedInfo() + ", ";
    }

    /**
     * Helper method that fetches from last.fm and adds the songs it retrieved to allSongs
     */
    private void lastFmFetch() {
        allSongs.addAll(lastfmScraper.fetch(Integer.parseInt(lastFmSongsToFetch.getText())));
        playListName += lastfmScraper.getFetchedInfo() + ", ";
    }

    /**
     * Helper method that fetches from last.fm's API and database of artist similarities and adds
     * the songs it retrieved to allSongs
     */
    private void similarFetch() {
        String[] tokens = similarArtists.getText().split(";");
        String artistList = "";
        for (String artist : tokens) {
            if (!(artist = artist.trim()).equals("")) {
                allSongs.addAll(lastfmScraper.fetchSimilar(artist,
                                Integer.parseInt(similarSongsToFetch.getText())));
                artistList += artist + ", ";
            }
        }
        artistList = artistList.substring(0, artistList.length() - 2);
        playListName += "music similar to - " + artistList + ", ";
    }

    /**
     * Helper method that fetches from last.fm's API and database of user's and top tracks and adds
     * songs from the user's friends list
     */
    private void lastFmFriendsFetch() {
        String timePeriod;
        if (weekBtn.isSelected()) {
            timePeriod = "7day";
        } else if (monthBtn.isSelected()) {
            timePeriod = "1month";
        } else {
            timePeriod = "12month";
        }

        allSongs.addAll(lastfmScraper.fetchFriendsMusic(lastFmUsername.getText(), timePeriod,
                        Integer.parseInt(lastFmFriendsSongsToFetch.getText())));
        playListName += lastfmScraper.getFetchedInfo() + ", ";
    }

    /**
     * Helper method that fetches from beatport and adds the songs it retrieved to allSongs
     */
    private void hypeMachineFetch() {
        try {
            allSongs.addAll(hypeMachineScraper.fetch(Integer.parseInt(hypeMachineSongsToFetch.getText())));
            playListName += hypeMachineScraper.getFetchedInfo() + ", ";
        } catch (FailingHttpStatusCodeException e) {
            Main.output("HTTP ERROR: Trying again...");
            try {
                allSongs.addAll(hypeMachineScraper
                                .fetch(Integer.parseInt(hypeMachineSongsToFetch.getText())));
                playListName += hypeMachineScraper.getFetchedInfo() + ", ";
            } catch (FailingHttpStatusCodeException e1) {
                Main.output("HTTP ERROR: Exiting Beatport Scraping procedure.");
            }

        }
    }

    /**
     * Method to shuffle the order of songs into a new ArrayList
     * 
     * @param original the array to be shuffled
     * @return a new array with random order
     */
    private ArrayList<Song> shuffleArray(ArrayList<Song> original) {
        ArrayList<Song> newArray = new ArrayList<Song>();
        while (original.size() != 0) {
            int size = original.size();
            Integer pick = (int) (Math.random() * size);
            newArray.add(original.get(pick));
            original.remove((int) pick);
        }
        return newArray;
    }

    /**
     * Populates a HashMap with genre titles and their corresponding link-IDs on beatport.
     */
    private static void populateGenres() {

        // this should eventually be converted into its own dynamic scraping process where we fetch
        // genre titles and links from beatport, store in a text file (in this directory) and then
        // reference that text file when populating the hashmap. Genre fetching/scraping could
        // probably be done once every few months to keep links accurate.

        bpGenreMap.put("Afro House", new Genre("afro-house/89/top-100"));
        bpGenreMap.put("Big Room", new Genre("big-room/79/top-100"));
        bpGenreMap.put("Breaks", new Genre("breaks/9/top-100"));
        bpGenreMap.put("Dance", new Genre("dance/39/top-100"));
        bpGenreMap.put("Deep House", new Genre("deep-house/12/top-100"));
        bpGenreMap.put("DJ Tools", new Genre("dj-tools/16/top-100"));
        bpGenreMap.put("Drum & Bass", new Genre("drum-and-bass/1/top-100"));
        bpGenreMap.put("Dubstep", new Genre("dubstep/18/top-100"));
        bpGenreMap.put("Electro House", new Genre("electro-house/17/top-100"));
        bpGenreMap.put("Electronica / Downtempo", new Genre("electronica-downtempo/3/top-100"));
        bpGenreMap.put("Funk / Soul / Disco", new Genre("funk-soul-disco/40/top-100"));
        bpGenreMap.put("Funky / Groove / Jackin' House",
                        new Genre("funky-groove-jackin-house/81/top-100"));
        bpGenreMap.put("Future House", new Genre("future-house/65/top-100"));
        bpGenreMap.put("Garage / Bassline / Grime", new Genre("garage-bassline-grime/86/top-100"));
        bpGenreMap.put("Glitch Hop", new Genre("glitch-hop/49/top-100"));
        bpGenreMap.put("Hard Dance", new Genre("hard-dance/8/top-100"));
        bpGenreMap.put("Hardcore / Hard Techno", new Genre("hardcore-hard-techno/2/top-100"));
        bpGenreMap.put("Hip Hop and R&B", new Genre("hip-hop-r-and-b/38/top-100"));
        bpGenreMap.put("House", new Genre("house/5/top-100"));
        bpGenreMap.put("Indie Dance / Nu Disco", new Genre("indie-dance-nu-disco/37/top-100"));
        bpGenreMap.put("Leftfield Bass", new Genre("leftfield-bass/85/top-100"));
        bpGenreMap.put("Leftfield House & Techno",
                        new Genre("leftfield-house-and-techno/80/top-100"));
        bpGenreMap.put("Melodic House & Techno", new Genre("melodic-house-and-techno/90/top-100"));
        bpGenreMap.put("Minimal / Deep Tech", new Genre("minimal-deep-tech/14/top-100"));
        bpGenreMap.put("Progressive House", new Genre("progressive-house/15/top-100"));
        bpGenreMap.put("Psy-Trance", new Genre("psy-trance/13/top-100"));
        bpGenreMap.put("Reggae / Dancehall / Dub", new Genre("reggae-dancehall-dub/41/top-100"));
        bpGenreMap.put("Tech House", new Genre("tech-house/11/top-100"));
        bpGenreMap.put("Techno", new Genre("techno/6/top-100"));
        bpGenreMap.put("Trance", new Genre("trance/7/top-100"));
        bpGenreMap.put("Trap / Future Bass", new Genre("trap-future-bass/87/top-100"));


        // billboard stuff
        billGenreMap.put("Alternative", new Genre("alternative-songs"));
        billGenreMap.put("Rock", new Genre("rock-songs"));
        billGenreMap.put("Top 200", new Genre("billboard-200"));
        billGenreMap.put("Adult Contemporary", new Genre("adult-contemporary"));
        billGenreMap.put("Adult Pop", new Genre("adult-pop-songs"));
        billGenreMap.put("Pop", new Genre("pop-songs"));
        billGenreMap.put("Country", new Genre("country-songs"));
        billGenreMap.put("Hip Hop & R&B", new Genre("r-b-hip-hop-songs"));
        billGenreMap.put("Mainstream Rock", new Genre("hot-mainstream-rock-tracks"));

    }

    /**
     * Helper method to create an array of checkboxes with names relating genres
     */
    private void createGenreArray() {

        for (Map.Entry<String, Genre> entry : bpGenreMap.entrySet()) {
            bpGenreChecks.add(new CheckBox(entry.getKey()));
        }

        for (Map.Entry<String, Genre> entry : billGenreMap.entrySet()) {
            billboardGenreChecks.add(new CheckBox(entry.getKey()));
        }

    }

    /**
     * simple getter method for 'allSongs' ArrayList
     * 
     * @return the arraylist of songs
     */
    public ArrayList<Song> getAllSongs() {
        return allSongs;
    }

    /**
     * simple setter method for allSongs
     * 
     * @param allSongs
     */
    public void setAllSongs(ArrayList<Song> allSongs) {
        this.allSongs = allSongs;
    }

    /**
     * helper method that ensures all paramters required for scraping Beatport are met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyBeatport() {
        if (bCheck.isSelected()) {
            if (!StringUtils.isNumeric(bpSongsToFetch.getText())
                            || Integer.parseInt(bpSongsToFetch.getText()) <= 0
                            || Integer.parseInt(bpSongsToFetch.getText()) > 100) {
                return false;
            }

            for (CheckBox c : bpGenreChecks) {
                if (c.isSelected()) {
                    bpUserGenres.add(c.getText());
                }
            }
        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping Billboard are met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyBillboard() {
        if (billCheck.isSelected()) {
            if (!StringUtils.isNumeric(billSongsToFetch.getText())
                            || Integer.parseInt(billSongsToFetch.getText()) <= 0
                            || Integer.parseInt(billSongsToFetch.getText()) > 200) {
                return false;
            }

            for (CheckBox c : billboardGenreChecks) {
                if (c.isSelected()) {
                    billUserGenres.add(c.getText());
                }
            }
        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping IndieShuffle are met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyIndieShuffle() {
        if (iCheck.isSelected()) {
            if (!StringUtils.isNumeric(indieSongsToFetch.getText())
                            || Integer.parseInt(indieSongsToFetch.getText()) <= 0) {
                return false;
            }

        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping reddit are met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyReddit() {
        if (redditCheck.isSelected()) {
            if (redditMinUpvotes.getText().equals("")) {
                if (!StringUtils.isNumeric(redditSongsToFetch.getText())
                                || Integer.parseInt(redditSongsToFetch.getText()) <= 0
                                || Integer.parseInt(redditSongsToFetch.getText()) > 100) {
                    return false;
                }
            } else {
                if (!StringUtils.isNumeric(redditSongsToFetch.getText())
                                || (!StringUtils.isNumeric(redditMinUpvotes.getText())
                                                && !redditMinUpvotes.getText().equals(""))
                                || Integer.parseInt(redditSongsToFetch.getText()) <= 0
                                || Integer.parseInt(redditSongsToFetch.getText()) > 100
                                || Integer.parseInt(redditMinUpvotes.getText()) <= 0
                                || Integer.parseInt(redditMinUpvotes.getText()) > 100000) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping last.fm are met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyLastFm() {
        if (lastFmCheck.isSelected()) {
            lastfmScraper.setUserLogin(lastFmUsername.getText(), lastFmPassword.getText());
            if (!StringUtils.isNumeric(lastFmSongsToFetch.getText())
                            || Integer.parseInt(lastFmSongsToFetch.getText()) <= 0
                            || lastFmUsername.getText().equals("")
                            || lastFmPassword.getText().equals("")
                            || !lastfmScraper.verifyUserNamePassword()) {
                return false;
            }
        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping last.fm's database for related
     * music is met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyRelatedMusic() {
        if (similarCheck.isSelected()) {
            if (!StringUtils.isNumeric(similarSongsToFetch.getText())
                            || Integer.parseInt(similarSongsToFetch.getText()) <= 0
                            || similarArtists.getText().equals("")) {
                return false;
            }
        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping last.fm's database for friends
     * music is met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyLastFmFriendsMusic() {
        if (lastFmFriendsCheck.isSelected()) {
            if (!StringUtils.isNumeric(lastFmFriendsSongsToFetch.getText())
                            || Integer.parseInt(lastFmFriendsSongsToFetch.getText()) <= 0
                            || lastFmUsername.getText().equals("")) {
                return false;
            }
        }
        return true;
    }

    /**
     * helper method that ensures all paramters required for scraping IndieShuffle are met
     * 
     * @return true if it is okay to continue or false if there is an error
     */
    private boolean verifyHypeMachine() {
        if (hypeCheck.isSelected()) {
            if (!StringUtils.isNumeric(hypeMachineSongsToFetch.getText())
                            || Integer.parseInt(hypeMachineSongsToFetch.getText()) <= 0) {
                return false;
            }

        }
        return true;
    }

    /**
     * Method that ensures no bad input, or missed checkboxes are being submitted to the program.
     * can be broken up into smaller methods in the future
     * 
     * @return
     */
    public boolean verifyInput() {

        if (!bCheck.isSelected() && !iCheck.isSelected() && !billCheck.isSelected()
                        && !redditCheck.isSelected() && !lastFmCheck.isSelected()
                        && !similarCheck.isSelected() && !lastFmFriendsCheck.isSelected()
                        && !hypeCheck.isSelected()) {
            Main.output("Error: Please select something to scrape.");
            return false;
        } else {
            return verifyBeatport() && verifyBillboard() && verifyIndieShuffle() && verifyReddit()
                            && verifyLastFm() && verifyRelatedMusic() && verifyLastFmFriendsMusic()
                            && verifyHypeMachine();
        }
    }

    /**
     * Simple method to output generic text relating to invalid input
     */
    private void inputError() {
        Main.output("Input error -- Fields missing or entered incorrectly.\nMake sure a website is "
                        + "selected and a number of songs to be "
                        + "fetched is correctly typed into the input box, as well as any other"
                        + " information required for that website\n");
    }

    /**
     * Helper method to reset all objects. Needs to be run after every 'submit' click
     */
    private void reset() {

        // reset everything
        beatportScraper = new BeatportScraper();
        indieShuffleScraper = new IndieShuffleScraper();
        billboardScraper = new Billboard100Scraper();
        redditScraper = new RedditScraper();
        lastfmScraper = new LastFmScraper();
        hypeMachineScraper = new HypeMachineScraper();

        // set hashmaps
        beatportScraper.setGenreMap(bpGenreMap);
        billboardScraper.setGenreMap(billGenreMap);

        bpUserGenres = new ArrayList<String>();
        billUserGenres = new ArrayList<String>();
        allSongs = new ArrayList<Song>();
        playListName = "";

    }

    /**
     * Thread-safe method to output text to the TextArea object. If output comes out at unreasonably
     * fast rates this can sometimes bug-out
     * 
     * @param msg the String to be displayed in the text area. The String will be appeneded to the
     *        pre-existing text in the text area.
     */
    public static synchronized void output(String msg) {
        if (ta.getText().equals("")) {
            ta.setText(msg);
        } else {
            ta.setText(ta.getText() + "\n" + msg);
        }
        ta.appendText("");
    }

    /**
     * simple method to clear the text area
     */
    public static synchronized void clearTextArea() {
        ta.clear();
    }

    /**
     * Simple method to open a website in the user's default browser
     * 
     * @param uri the address of the website to be opened
     * @return true for success and false for an error
     */
    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Static method that prints the contents of the console (text area) to a txt file in the
     * current directory.
     */
    public static void printConsole() {

        File file = new File("MusiCollect_Results-" + strDate + ".txt");
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(file);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        writer.println(ta.getText());
        writer.close();
    }


    /**
     * Method used to extract a URI from potentially bad user input
     * 
     * @param userText user input
     * @return the spotify playlist uri (if it exists within the input), else just returns the user
     *         input
     */
    public static String parseSpotifyPlaylistURI(String userText) {
        int index;
        String uri = null;
        String searchCriteria = "playlist:";
        if ((index = userText.lastIndexOf(searchCriteria)) != -1) {
            uri = userText.substring(index + searchCriteria.length(), userText.length());
        } else {
            uri = userText;
        }
        return uri;
    }
}
