package utils.sql;
/*
    Created by ConnysSoftware / ConCode on 01.05.2018 at 01:16.
    
    (c) ConnysSoftware / ConCode
    (c) strukteon
*/

import core.Main;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import utils.Tools;

import java.util.*;
import java.util.regex.Pattern;

public class VoteSQL {
    private static MySQL mySQL;
    private static String table = "votes";

    private String userid;

    public static void init(MySQL mySQL){
        VoteSQL.mySQL = mySQL;
    }


    private VoteSQL(String userid){
        this.userid = userid;
    }


    private static VoteSQL fromUserId(String userid){
        return new VoteSQL(userid);
    }

    public static VoteSQL fromUser(User user){
        return fromUserId(user.getId());
    }

    public static VoteSQL fromMember(Member member){
        return fromUser(member.getUser());
    }

    public static VoteSQL createNew(String userid, String msgid, String question, List<String> options){
        mySQL.INSERT(table, "`id`, `msgid`, `question`, `options`, `votes`", String.format("'%s', '%s', '%s', '%s', ''", userid, msgid, question.replace('\'', '"'), Tools.listToString(options, "|").replace('\'', '"')));
        return fromUserId(userid);
    }

    public static VoteSQL fromMessageId(String msgid){
        return VoteSQL.fromUserId(mySQL.SELECT("*", table, "msgid='"+msgid+"'").get("id"));
    }


    public boolean exists(){
        return mySQL.SELECT("*", table, "id="+userid).size() != 0;
    }


    public User getAuthor(){
        return Main.jda.getUserById(userid);
    }

    public String getQuestion(){
        return mySQL.SELECT("*", table, "id="+userid).get("question");
    }

    public List<String> getOptions(){
        return Arrays.asList(mySQL.SELECT("*", table, "id="+userid).get("options").split(Pattern.quote("|")));
    }

    public HashMap<Integer, List<String>> getVotes(){
        HashMap<Integer, List<String>> votes = new HashMap<>();
        try {
            for (String us : mySQL.SELECT("*", table, "id='" + userid + "'").get("votes").split(" ")) {
                String[] split = us.split(":");
                int pos = Integer.parseInt(split[0]);
                String userid = split[1];
                if (!votes.containsKey(pos))
                    votes.put(pos, new ArrayList<>());
                List<String> voters = votes.get(pos);
                voters.add(userid);
            }
        } catch (NumberFormatException ignored) { }
        return votes;
    }

    public String getMessageId(){
        return mySQL.SELECT("*", table, "id="+userid).get("msgid");
    }

    public void addVote(int pos, String uid){
        HashMap<Integer, List<String>> votes = getVotes();
        if (!votes.containsKey(pos))
            votes.put(pos, new ArrayList<>());
        List<String> voters = votes.get(pos);
        voters.add(uid);
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Integer, List<String>> entry :votes.entrySet())
            for (String user : entry.getValue())
                out.append(entry.getKey()).append(":").append(user).append(" ");
        mySQL.UPDATE(table, "`votes`='"+out.toString()+"'", "id="+userid);
    }

    public boolean hasVoted(String userid){
        HashMap<Integer, List<String>> votes = getVotes();
        for (List<String> users : votes.values())
            if (users.contains(userid))
                return true;
        return false;
    }

    public void close(){
        mySQL.DELETE(table, "id="+userid);
    }
}
