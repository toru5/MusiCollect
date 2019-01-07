package edu.wisc.cs.scraping_tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Main extends Application {

    static HashMap<String, Genre> bpGenreMap = new HashMap<String, Genre>();
    static HashMap<String, Genre> billGenreMap = new HashMap<String, Genre>();

    ArrayList<CheckBox> bpGenreChecks = new ArrayList<CheckBox>();
    ArrayList<CheckBox> billboardGenreChecks = new ArrayList<CheckBox>();

    ArrayList<String> bpUserGenres;
    ArrayList<String> billUserGenres;

    ArrayList<String> allVideoIds;

    // initialize scrapers
    BeatportScraper b;
    IndieShuffleScraper i;
    Billboard100Scraper bill;
    RedditScraper r;
    lastFmScraper l;

    String playlistId = "";

    // used for playlist name
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String strDate = sdf.format(date);

    String playListName = "";

    // textfields
    TextField bpSongsToFetch = new TextField();
    TextField billSongsToFetch = new TextField();
    TextField indieSongsToFetch = new TextField();
    TextField redditSongsToFetch = new TextField();
    TextField redditMinUpvotes = new TextField();
    TextField uniqueSubreddit = new TextField();
    TextField lastFmSongsToFetch = new TextField();
    TextField lastFmUsername = new TextField();
    TextField lastFmPassword = new TextField();

    static TextArea ta;


    // website checks
    CheckBox bCheck = new CheckBox("Beatport");
    CheckBox iCheck = new CheckBox("Indie Shuffle");
    CheckBox billCheck = new CheckBox("Billboard");
    CheckBox redditCheck = new CheckBox("Reddit");
    CheckBox lastFmCheck = new CheckBox("Last FM");

    // modifier checks
    CheckBox billRandomCheck = new CheckBox("BILLBOARD Random Song Order?");
    CheckBox bpRandomCheck = new CheckBox("BEATPORT Random Song Order?");
    CheckBox shuffleSongsCheck = new CheckBox("Shuffle songs in playlist?");

    /**
     * 
     */
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MusiCollect");
        Scene scene = new Scene(setup(), 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /*
     * 
     * 
     */
    public static void main(String[] args) {
        launch(args);
    }


    /**
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
        ta.setPrefHeight(698);
        ta.setWrapText(true);

        //
        // ta.textProperty().addListener(new ChangeListener<Object>() {
        // @Override
        // public void changed(ObservableValue<?> observable, Object oldValue,
        // Object newValue) {
        // ta.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
        // //use Double.MIN_VALUE to scroll to the top
        // }
        // });

        // TextAreaOutputStream taos = new TextAreaOutputStream(ta, 100);
        // PrintStream ps = new PrintStream(taos);
        // System.setOut(ps);
        // System.setErr(ps);

        // vbox containers
        VBox sites = new VBox(15);
        VBox fetch = new VBox(9);
        VBox upvotes = new VBox(112);
        VBox bpGenres = new VBox();
        VBox billboardGenres = new VBox();

        GridPane center = new GridPane();
        HBox bottom = new HBox();

        // column headers
        Label col1 = new Label("Websites to scrape:");
        Label col2 = new Label("Songs to be fetched (from each genre):");
        Label col3 = new Label("Minimum Upvotes");
        Label col4 = new Label("BEATPORT genres to select from:");
        Label col5 = new Label("BILLBOARD genres to select from:");

        Label subredditLbl = new Label("Subreddit to scrape:");
        Label lfmUsername = new Label("Last.fm Username:");
        Label lfmPassword = new Label("Last.fm Password:");

        // prompt text
        bpSongsToFetch.setPromptText("MAX 100");
        billSongsToFetch.setPromptText("MAX 100");
        indieSongsToFetch.setPromptText("MAX 15");
        redditSongsToFetch.setPromptText("MAX 100");
        redditMinUpvotes.setText("1");
        uniqueSubreddit.setText("listentothis");

        shuffleSongsCheck.setPrefSize(400, 100);

        // buttons
        final Button submit = new Button("Submit");
        Button exit = new Button("Exit");
        Button stop = new Button("Stop");

        exit.setPrefSize(400, 100);
        // stop.setPrefSize(400, 100);
        submit.setPrefSize(400, 100);

        sites.getChildren().addAll(col1, bCheck, iCheck, billCheck, redditCheck, subredditLbl,
                        lastFmCheck, lfmUsername, lfmPassword);

        fetch.getChildren().addAll(col2, bpSongsToFetch, indieSongsToFetch, billSongsToFetch,
                        redditSongsToFetch, uniqueSubreddit, lastFmSongsToFetch, lastFmUsername,
                        lastFmPassword);
        upvotes.getChildren().addAll(col3, redditMinUpvotes);
        bpGenres.getChildren().add(col4);
        billboardGenres.getChildren().add(col5);

        bpGenreChecks.sort(new Comparator<CheckBox>() {

            @Override
            public int compare(CheckBox o1, CheckBox o2) {
                return o1.getText().compareTo(o2.getText());
            }

        });

        billboardGenreChecks.sort(new Comparator<CheckBox>() {

            @Override
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

        bottom.getChildren().addAll(submit, exit, shuffleSongsCheck);

        root.setCenter(center);
        root.setBottom(bottom);

        ScrollPane console = new ScrollPane();
        console.setMaxHeight(2000);
        console.setFitToWidth(true);
        console.setContent(ta);
        root.setRight(console);

        submit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                // Define a new Runnable
                Runnable fetchMusic = new Runnable() {
                    public void run() {
                        reset();

                        // check user input and do not continue if it is invalid
                        if (!verifyInput()) {
                            return;
                        }

                        if (bCheck.isSelected()) {
                            try {
                                allVideoIds.addAll(b.fetch(
                                                Integer.parseInt(bpSongsToFetch.getText()),
                                                bpUserGenres, bpRandomCheck.isSelected(), false));
                                playListName += b.getFetchedInfo() + ", ";
                            } catch (FailingHttpStatusCodeException e) {
                                Main.output("HTTP ERROR: Trying again...");
                                try {
                                    allVideoIds.addAll(b.fetch(
                                                    Integer.parseInt(bpSongsToFetch.getText()),
                                                    bpUserGenres, bpRandomCheck.isSelected(),
                                                    false));
                                    playListName += b.getFetchedInfo() + ", ";
                                } catch (FailingHttpStatusCodeException e1) {
                                    Main.output("HTTP ERROR: Exiting Beatport Scraping procedure.");
                                }

                            }
                        }

                        if (iCheck.isSelected()) {
                            allVideoIds.addAll(
                                            i.fetch(Integer.parseInt(indieSongsToFetch.getText())));
                            playListName += i.getFetchedInfo() + ", ";
                        }

                        if (billCheck.isSelected()) {
                            allVideoIds.addAll(bill.fetch(
                                            Integer.parseInt(billSongsToFetch.getText()),
                                            billUserGenres, billRandomCheck.isSelected()));
                            playListName += bill.getFetchedInfo() + ", ";
                        }

                        if (redditCheck.isSelected()) {
                            allVideoIds.addAll(r.fetch(
                                            uniqueSubreddit.getText().replaceAll("/", "").trim(),
                                            Integer.parseInt(redditSongsToFetch.getText()),
                                            Integer.parseInt(redditMinUpvotes.getText())));
                            playListName += r.getFetchedInfo() + ", ";
                        }

                        if (lastFmCheck.isSelected()) {
                            allVideoIds.addAll(l
                                            .fetch(Integer.parseInt(lastFmSongsToFetch.getText())));
                            playListName += l.getFetchedInfo() + ", ";
                        }

                        if (allVideoIds.size() != 0) {

                            if (shuffleSongsCheck.isSelected()) {
                                allVideoIds = shuffleArray(allVideoIds);
                            }

                            // shed off extra comma from end
                            playListName = playListName.substring(0, playListName.length() - 2);

                            playlistId = YouTubeScraper.createPlaylist(allVideoIds,
                                            "MusiCollect Results - " + strDate + ": "
                                                            + playListName);
                            Main.output("Fetching complete. Your YouTube playlist can be found"
                                            + " here: https://www.youtube.com/playlist?list="
                                            + playlistId);
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

    private ArrayList<String> shuffleArray(ArrayList<String> original) {
        ArrayList<String> newArray = new ArrayList<String>();
        while (original.size() != 0) {
            int size = original.size();
            Integer pick = (int) (Math.random() * size);
            newArray.add(original.get(pick));
            original.remove((int) pick);
        }
        return newArray;
    }

    /**
     * 
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
     * 
     */
    private void createGenreArray() {

        for (Map.Entry<String, Genre> entry : bpGenreMap.entrySet()) {
            bpGenreChecks.add(new CheckBox(entry.getKey()));
        }

        for (Map.Entry<String, Genre> entry : billGenreMap.entrySet()) {
            billboardGenreChecks.add(new CheckBox(entry.getKey()));
        }

    }

    public ArrayList<String> getAllVideoIds() {
        return allVideoIds;
    }

    public void setAllVideoIds(ArrayList<String> allVideoIds) {
        this.allVideoIds = allVideoIds;
    }

    public boolean verifyInput() {

        billUserGenres = new ArrayList<String>();
        bpUserGenres = new ArrayList<String>();

        if (!bCheck.isSelected() && !iCheck.isSelected() && !billCheck.isSelected()
                        && !redditCheck.isSelected() && !lastFmCheck.isSelected()) {
            Main.output("Error: Please select a website to scrape.");
            return false;
        } else {
            if (bCheck.isSelected()) {
                if (!StringUtils.isNumeric(bpSongsToFetch.getText())
                                || Integer.parseInt(bpSongsToFetch.getText()) <= 0
                                || Integer.parseInt(bpSongsToFetch.getText()) > 100) {
                    inputError();
                    return false;
                }

                for (CheckBox c : bpGenreChecks) {
                    if (c.isSelected()) {
                        bpUserGenres.add(c.getText());
                    }
                }

            }

            if (billCheck.isSelected()) {
                if (!StringUtils.isNumeric(billSongsToFetch.getText())
                                || Integer.parseInt(billSongsToFetch.getText()) <= 0
                                || Integer.parseInt(billSongsToFetch.getText()) > 200) {
                    inputError();
                    return false;
                }

                for (CheckBox c : billboardGenreChecks) {
                    if (c.isSelected()) {
                        billUserGenres.add(c.getText());
                    }
                }
            }

            // someone please fucking kill me already =]]]
            if (iCheck.isSelected()) {
                if (!StringUtils.isNumeric(indieSongsToFetch.getText())
                                || Integer.parseInt(indieSongsToFetch.getText()) <= 0
                                || Integer.parseInt(indieSongsToFetch.getText()) > 15) {
                    inputError();
                    return false;
                }

            }

            if (redditCheck.isSelected()) {
                if (!StringUtils.isNumeric(redditSongsToFetch.getText())
                                || !StringUtils.isNumeric(redditMinUpvotes.getText())
                                || Integer.parseInt(redditSongsToFetch.getText()) <= 0
                                || Integer.parseInt(redditSongsToFetch.getText()) > 100
                                || Integer.parseInt(redditMinUpvotes.getText()) <= 0
                                || Integer.parseInt(redditMinUpvotes.getText()) > 100000) {
                    inputError();
                    return false;
                }

            }

            if (lastFmCheck.isSelected()) {
                l.setUserLogin(lastFmUsername.getText(), lastFmPassword.getText());
                if (!StringUtils.isNumeric(lastFmSongsToFetch.getText())
                                || Integer.parseInt(lastFmSongsToFetch.getText()) <= 0
                                || lastFmUsername.getText().equals("")
                                || lastFmPassword.getText().equals("")
                                || !l.verifyUserNamePassword()) {
                    inputError();
                    return false;
                }

            }

        }
        return true;
    }

    /**
     * 
     */
    private void inputError() {
        Main.output("Input error.  Make sure a website is selected and a number of songs to be fetched is correctly typed into the input box.");
    }

    /**
     * 
     */
    private void reset() {

        // reset everything
        b = new BeatportScraper();
        i = new IndieShuffleScraper();
        bill = new Billboard100Scraper();
        r = new RedditScraper();
        l = new lastFmScraper();

        // set hashmaps
        b.setGenreMap(bpGenreMap);
        bill.setGenreMap(billGenreMap);

        bpUserGenres = new ArrayList<String>();
        billUserGenres = new ArrayList<String>();
        allVideoIds = new ArrayList<String>();
        playListName = "";

    }

    public static synchronized void output(String msg) {
        ta.setText(ta.getText() + "\n" + msg);
        ta.appendText("");
    }
}
