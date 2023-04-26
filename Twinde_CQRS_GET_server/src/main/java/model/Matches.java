package model;

import java.util.ArrayList;

public class Matches {
    private ArrayList<String> matchList;

    public Matches(ArrayList<String> matchList) {
        this.matchList = matchList;
    }

    public void addToList(String userId){
        this.matchList.add(userId);
    }

    public ArrayList<String> getMatchList() {
        return matchList;
    }

    public void setMatchList(ArrayList<String> matchList) {
        this.matchList = matchList;
    }
}
