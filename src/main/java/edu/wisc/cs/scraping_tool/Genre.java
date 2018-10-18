package edu.wisc.cs.scraping_tool;

public class Genre {
    
    String postLink;
    
    public Genre(String pLink) {
        postLink = pLink;
    }
    
    public String getPostLink() {
        return postLink;
    }
    public void setPostLink(String postLink) {
        this.postLink = postLink;
    }

}
