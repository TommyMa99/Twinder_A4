package model;

public class MatchStats {
    private int numLikes;
    private int numDislikes;

    public MatchStats(int numLikes, int numDislikes) {
        this.numLikes = numLikes;
        this.numDislikes = numDislikes;
    }

    public int getNumLikes() {
        return numLikes;
    }

    public void setNumLikes(int numLikes) {
        this.numLikes = numLikes;
    }

    public int getNumDislikes() {
        return numDislikes;
    }

    public void setNumDislikes(int numDislikes) {
        this.numDislikes = numDislikes;
    }
}
