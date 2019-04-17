package edu.wisc.cs.scraping_tool;

/**
 * Class used to store song data in one encompassing object.
 * @author Zach Kremer
 *
 */
public class Song {
    private String title;
    private String artist;
    private String remixer;
    private String album;
    private String recordLabel;
    private String releaseDate;
    private String genre;
    private String albumArtLink;
    private String youtubeLink;
    private String youtubeEmbedLink;
    private String link;
    private String embedLink;
    private final String SEPARATOR = ";";
    
    public Song() {
        // do nothing
    }
    
    public Song(String artist, String title) {
        this.artist = artist;
        this.title = title;
    }

    public String toString() {
        return "Title: " + title + "\nArtist: " + artist + "\nAlbum: " + album + "\nRecord Label: "
                        + recordLabel + "\nRelease Date: " + releaseDate + "\nGenre: " + genre
                        + "\nLink: " + link + "\nAlbum Art Link: " + albumArtLink
                        + "\nYouTube Link: " + youtubeLink + "\nYouTube Embed Link: "
                        + youtubeEmbedLink + "\nGeneral Embed Link" + embedLink;
    }


    public String toSv() {
        return title + SEPARATOR + artist + SEPARATOR + album + SEPARATOR + recordLabel + SEPARATOR
                        + releaseDate + SEPARATOR + genre + SEPARATOR + link + SEPARATOR
                        + albumArtLink + SEPARATOR + youtubeLink + SEPARATOR + youtubeEmbedLink
                        + SEPARATOR + embedLink + SEPARATOR;

    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAlbumArtLink() {
        return albumArtLink;
    }

    public void setAlbumArtLink(String albumArtLink) {
        this.albumArtLink = albumArtLink;
    }

    public String getYoutubeLink() {
        return youtubeLink;
    }

    public void setYoutubeLink(String youtubeLink) {
        this.youtubeLink = youtubeLink;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getEmbedLink() {
        return embedLink;
    }

    public void setEmbedLink(String embedLink) {
        this.embedLink = embedLink;
    }

    public String getRecordLabel() {
        return recordLabel;
    }

    public void setRecordLabel(String recordLabel) {
        this.recordLabel = recordLabel;
    }


    public String getRemixer() {
        return remixer;
    }


    public void setRemixer(String remixer) {
        this.remixer = remixer;
    }


    public String getYoutubeEmbedLink() {
        return youtubeEmbedLink;
    }


    public void setYoutubeEmbedLink(String youtubeEmbedLink) {
        this.youtubeEmbedLink = youtubeEmbedLink;
    }
}
