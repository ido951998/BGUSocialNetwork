package bgu.spl.net.impl.BGSServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class DB {

    class userStats{
        String password;
        Vector<String> followings;
        Vector<String> followers;
        Vector<String> blockedBy;
        Vector<String> blocks;
        Vector<String> awaitingMessages;
        Date birthDate;
        int numberOfPosts;

        public userStats(String password, String birthday){
            this.password = password;
            this.followings = new Vector<>();
            this.followers = new Vector<>();
            this.blockedBy = new Vector<>();
            this.awaitingMessages = new Vector<>();
            this.blocks = new Vector<>();
            this.birthDate = new Date();
            birthDate.setYear(Integer.parseInt(birthday.substring(6))-1900);
            birthDate.setMonth(Integer.parseInt(birthday.substring(3,5))-1);
            birthDate.setDate(Integer.parseInt(birthday.substring(0,2)));
            numberOfPosts = 0;
        }

        public String getPassword() {
            return password;
        }

        public boolean addFollow(String username){
            if (followings.contains(username)) return false;
            followings.add(username);
            return true;
        }

        public boolean removeFollow(String username){
            if (!followings.contains(username)) return false;
            followings.remove(username);
            return true;
        }

        public boolean addFollower(String username){
            if (followers.contains(username)) return false;
            followers.add(username);
            return true;
        }

        public boolean removeFollower(String username){
            if (!followers.contains(username)) return false;
            followers.remove(username);
            return true;
        }

        public Vector<String> getFollowers(){
            return new Vector<>(followers);
        }

        public void increasePosts(){
            numberOfPosts++;
        }

        public int getNumberOfFollowers() {
            return followers.size();
        }

        public Integer getAge() {
            Date date = new Date();
            int age = date.getYear()-birthDate.getYear();
            date.setYear(birthDate.getYear());
            if (date.before(birthDate)){
                age--;
            }
            return age;
        }

        public int getNumberOfPosts() {
            return numberOfPosts;
        }

        public int getNumFollowing(){
            return followings.size();
        }

        public void addBlock(String username){
            blockedBy.add(username);
        }

        public List<String> getBlockedBy(){
            return new Vector<>(blockedBy);
        }

        public void addAwaitingMessage(String awaitingMessage){
            this.awaitingMessages.add(awaitingMessage);
        }

        public Vector<String> getAwaitingMessages(){
            Vector<String> ans = new Vector<>(awaitingMessages);
            awaitingMessages.clear();
            return ans;
        }

        public void addUserBlocks(String userToBlock){
            blocks.add(userToBlock);
        }

        public Vector<String> getBlocks(){
            return new Vector<>(blocks);
        }
    }
    ConcurrentHashMap<String,userStats> logins;
    ConcurrentHashMap<Integer, String> connectionIDtoUsername;
    ConcurrentHashMap<String, Integer> usernameToID;
    Vector<String> messages;
    int loggedIn;
    private static DB db = null;

    private DB(){
        this.logins = new ConcurrentHashMap<>();
        this.connectionIDtoUsername = new ConcurrentHashMap<>();
        this.usernameToID= new ConcurrentHashMap<>();
        this.messages = new Vector<>();
        this.loggedIn = 0;
    }

    public List<Integer> getFollowers(Integer connectionID){
        String username = getRequestingUsername(connectionID);
        if (username == null) return null;
        List<String> followers = logins.get(username).getFollowers();
        List<Integer> followersIDs = new ArrayList<>();
        for (String follower : followers){
            followersIDs.add(usernameToID.get(follower));
        }
        return followersIDs;
    }

    public Integer getID(String userToSend){
        return usernameToID.get(userToSend);
    }

    public static DB getInstance(){
        if (db == null){
            db = new DB();
        }
        return db;
    }

    public String getRequestingUsername(Integer connectionID){
        if (connectionIDtoUsername.containsKey(connectionID)) return connectionIDtoUsername.get(connectionID);
        return null;
    }

    public boolean registerUser(String username, String password, String birthday){
        if (logins.containsKey(username)) return false;
        logins.put(username,new userStats(password, birthday));
        return true;
    }

    public boolean loginUser(String username, String password, Integer connectionID){
        if (!logins.containsKey(username)) return false;
        if (logins.get(username).getPassword().equals(password) && !connectionIDtoUsername.containsValue(username)){
            connectionIDtoUsername.put(connectionID, username);
            usernameToID.put(username, connectionID);
            loggedIn++;
            return true;
        }
        return false;
    }

    public boolean logoutUser(Integer connectionID){
        if (loggedIn == 0) return false;
        List<Integer> l = new ArrayList<>();
        l.add(connectionID);
        updateDisconnected(l);
        return true;
    }

    public void updateDisconnected (List<Integer> connectionIDs){
        for (Integer connectionID : connectionIDs){
            if (getRequestingUsername(connectionID) != null) {
                usernameToID.remove(getRequestingUsername(connectionID));
                connectionIDtoUsername.remove(connectionID);
                loggedIn--;
            }
        }
    }

    public boolean followUser(Integer connectionID, String userToFollow, byte type){
        String username = getRequestingUsername(connectionID);
        if (username == null) return false;
        if (!logins.containsKey(userToFollow)) return false;

        if (type == '0') {
            logins.get(userToFollow).addFollower(username);
            return logins.get(username).addFollow(userToFollow);
        }
        else{
            logins.get(userToFollow).removeFollower(username);
            return logins.get(username).removeFollow(userToFollow);
        }
    }

    public boolean postMessage(Integer connectionID, String message, List<String> usersMentioned){
        String username = getRequestingUsername(connectionID);
        if (username == null) return false;
        for (String userMentioned : usersMentioned){
            if (!usernameToID.containsKey(userMentioned)){
                logins.get(userMentioned).addAwaitingMessage(message);
            }
        }
        Vector<String> followers = logins.get(username).getFollowers();
        for (String follower : followers){
            if (!usernameToID.containsKey(follower)){
                String msg = 1+username+(char)0+message+(char)0;
                logins.get(follower).addAwaitingMessage(msg);
            }
        }
        messages.add(message);
        logins.get(username).increasePosts();
        return true;
    }

    public boolean pmMessage(Integer connectionID, String userToSend, String message){
        String username = getRequestingUsername(connectionID);
        if (username == null) return false;
        if (!logins.containsKey(userToSend)) return false;
        if (!usernameToID.containsKey(userToSend)){
            String msg = 1+username+(char)0+message+(char)0;
            logins.get(userToSend).addAwaitingMessage(msg);
        }
        messages.add(message);
        return true;
    }

    public List<String> logStats(Integer connectionID){
        String username = getRequestingUsername(connectionID);
        if (username == null) return null;
        List<String> ans = new ArrayList<>();
        List<String> blockedBy = logins.get(username).getBlockedBy();
        List<String> blocks = logins.get(username).getBlocks();
        for (String user : usernameToID.keySet()) {
            if (!blockedBy.contains(user) && !blocks.contains(user)) {
                userStats stats = logins.get(user);
                String str = " " + stats.getAge().toString() + " " + Integer.valueOf(stats.getNumberOfPosts()).toString() + " " + Integer.valueOf(stats.getNumberOfFollowers()).toString() + " " + Integer.valueOf(stats.getNumFollowing()).toString();
                ans.add(str);
            }
        }
        return ans;
    }

    public List<String> stat(Integer connectionID, List<String> users){
        String username = getRequestingUsername(connectionID);
        if (username == null) return null;
        List<String> ans = new ArrayList<>();
        for (String user : users) {
            if (!logins.containsKey(user)) return null;
            userStats stats = logins.get(user);
            String str =  " " + stats.getAge().toString() + " " + Integer.valueOf(stats.getNumberOfPosts()).toString() + " " + Integer.valueOf(stats.getNumberOfFollowers()).toString() + " " + Integer.valueOf(stats.getNumFollowing()).toString();
            ans.add(str);
        }
        return ans;
    }

    public boolean block(Integer connectionID, String userToBlock){
        String username = getRequestingUsername(connectionID);
        if (username == null) return false;
        if (!logins.containsKey(userToBlock)) return false;
        logins.get(username).addUserBlocks(userToBlock);
        logins.get(userToBlock).addBlock(username);
        logins.get(userToBlock).removeFollow(username);
        return true;
    }

    public boolean connectionIDInSystem(int connectionID){
        return connectionIDtoUsername.containsKey(connectionID);
    }

    public List<String> blockedBy(Integer connectionID) {
        String username = getRequestingUsername(connectionID);
        if (!logins.containsKey(username)) return new ArrayList<String>();
        return logins.get(username).getBlockedBy();
    }

    public Set<Integer> getLoggedInId(){
        return new HashSet<>(connectionIDtoUsername.keySet());
    }

    public boolean isLoggedIn(String userToSend){
        return usernameToID.containsKey(userToSend);
    }

    public List<String> getAwaitingMessages(String username){
        return logins.get(username).getAwaitingMessages();
    }

    public List<String> blocks(int connectionID) {
        String username = getRequestingUsername(connectionID);
        if (!logins.containsKey(username)) return new ArrayList<String>();
        return logins.get(username).getBlocks();
    }
}
