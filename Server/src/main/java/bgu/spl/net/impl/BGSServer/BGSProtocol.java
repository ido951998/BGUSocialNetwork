package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.Connections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BGSProtocol implements BidiMessagingProtocol<Message> {
    Connections<Message> connections;
    int connectionId;
    DB db;
    List<String> forbidden;
    boolean shouldTerminate;

    public BGSProtocol(){
        db = DB.getInstance();
        connections = null;
        connectionId = -1;
        this.forbidden = new ArrayList<>();
        forbidden.add("Trump");
        forbidden.add("Bibi");
        this.shouldTerminate = false;
    }

    private void sendAck(String str){
        connections.send(connectionId, new MessageImpl((short) 10, str));
    }

    private void sendError(String str){
        connections.send(connectionId,new MessageImpl((short) 11,str));
    }

    private void sendNotification(Integer ID, String str){
        connections.send(ID,new MessageImpl((short) 9,str));
    }

    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    private void registerRequest(String message){
        String s = "" + (char)0;
        s += (char)1;
        if (db.connectionIDInSystem(connectionId)){
            sendError(s);
            return;
        }
        String username = "", password = "", birthday = "";
        int counter = 0;
        for (int i=0; i<message.length()-1; i++){
            if (message.charAt(i) == 0){
                counter++;
                continue;
            }
            if (counter == 0) {
                username += message.charAt(i);
            }
            else if (counter == 1){
                password += message.charAt(i);
            }
            else{
                birthday += message.charAt(i);
            }
        }

        boolean successful = db.registerUser(username,password,birthday);
        if (successful){
            sendAck(s);
        }
        else{
            sendError(s);
        }
    }
    private void loginRequest(String message){
        String s = "" + (char)0;
        s += (char)2;
        if (db.connectionIDInSystem(connectionId)){
            sendError(s);
            return;
        }
        String username = "", password = "";
        byte captcha = (byte)message.charAt(message.length()-1);
        int counter = 0;
        for (int i=0; i<message.length()-2; i++){
            if (message.charAt(i) == 0){
                counter++;
                continue;
            }
            if (counter == 0) {
                username += message.charAt(i);
            }
            else{
                password += message.charAt(i);
            }
        }
        boolean successful = false;
        if (captcha == 1){
            successful = db.loginUser(username,password,connectionId);
        }
        if (successful){
            sendAck(s);
            List<String> awaitingMessages = db.getAwaitingMessages(username);
            for (String msg : awaitingMessages){
                sendNotification(connectionId, msg);
            }
        }
        else{
            sendError(s);
        }
    }
    private void logoutRequest(){
        boolean successful = db.logoutUser(connectionId);
        String s = "" + (char)0;
        s += (char)3;
        if (successful){
            sendAck(s);
            shouldTerminate = true;
            connections.disconnect(connectionId);
        }
        else{
            sendError(s);
        }
    }
    private void brutalLogoutRequest(){
        boolean successful = db.logoutUser(connectionId);
        if (successful){
            connections.disconnect(connectionId);
        }
    }
    private void followRequest(String message){
        String s = "" + (char)0;
        s += (char)4;
        byte type = (byte)message.charAt(0);
        String username = message.substring(1);
        List<String> blockedBy = db.blockedBy(connectionId);
        if (type == '0'){
            if (blockedBy.contains(username)){
                sendError(s);
                return;
            }
        }
        boolean successful = db.followUser(connectionId, username, type);
        if (successful){
            sendAck(s + type + username + (char)0);
        }
        else{
            sendError(s);
        }
    }
    private void postRequest(String message){
        String s = "" + (char)0;
        s += (char)5;
        String content = message.substring(0,message.length()-1);
        ArrayList<String> usersMentioned = new ArrayList<>();
        for (int i=0; i<content.length(); i++){
            if (content.charAt(i) == '@'){
                String username = "";
                i++;
                while (i < content.length() && content.charAt(i) != ' '){
                    username += content.charAt(i);
                    i++;
                }
                usersMentioned.add(username);
            }
        }

        Set<Integer> loggedIn = db.getLoggedInId();
        boolean successful = db.postMessage(connectionId, content, usersMentioned);
        if (!successful){
            sendError(s);
        }
        else{
            List<Integer> sendTo = db.getFollowers(connectionId);
            List<String> blockedBy = db.blockedBy(connectionId);
            for (String user : usersMentioned){
                if (!blockedBy.contains(user)){
                    Integer ID = db.getID(user);
                    if (ID != null && !sendTo.contains(ID)) sendTo.add(ID);
                }
            }
            String msg = 0+db.getRequestingUsername(connectionId)+(char)0+content+(char)0;
            sendAck(s);
            for (Integer id : sendTo){
                if (loggedIn.contains(id)){
                    sendNotification(id, msg);
                }
            }
        }
    }
    private void pmRequest(String message){
        String s = "" + (char)0;
        s += (char)6;
        String username = "", content = "", sendingDT = "";
        int counter = 0;
        for (int i=0; i<message.length()-1; i++){
            if (message.charAt(i) == 0){
                counter++;
                continue;
            }
            if (counter == 0) {
                username += message.charAt(i);
            }
            else if (counter == 1){
                content += message.charAt(i);
            }
            else{
                sendingDT += message.charAt(i);
            }
        }

        List<String> words = new ArrayList<>();
        String word = "";
        for (int i=0; i<content.length(); i++){
            if (content.charAt(i) == ' '){
                words.add(word);
                word = "";
            }
            else{
                word += content.charAt(i);
            }
        }
        words.add(word);

        content = "";
        for (String w : words){
            if (forbidden.contains(w)){
                w = "<filtered>";
            }
            content += w + " ";
        }
        content = content.substring(0,content.length()-1);

        List<String> blockedBy = db.blockedBy(connectionId);
        if (blockedBy.contains(username)){
            sendError(s);
        }
        else {
            boolean successful = db.pmMessage(connectionId, username, content);
            if (!successful) {
                sendError(s);
            } else if (db.isLoggedIn(username)){
                String msg = 1+db.getRequestingUsername(connectionId)+(char)0+content+(char)0;
                sendAck(s);
                sendNotification(db.getID(username), msg);
            }
        }
    }
    private void loggedInStatsRequests(){
        String s = "" + (char)0;
        s += (char)7;
        List<String> replies = db.logStats(connectionId);
        if (replies == null){
            sendError(s);
        }
        else{
            for (String reply:replies){
                sendAck(s + reply);
            }
        }
    }
    private void statsRequests(String message){
        String s = "" + (char)0;
        s += (char)8;
        List<String> usernames = new ArrayList<>();
        for (int i=0; i<message.length()-1; i++){
            String username = "";
            while (message.charAt(i) != '|' && i<message.length()-1){
                username += message.charAt(i);
                i++;
            }
            usernames.add(username);
        }
        List<String> blockedBy = db.blockedBy(connectionId);
        for (String blockedUser : blockedBy){
            if (usernames.contains(blockedUser)){
                sendError(s);
                return;
            }
        }

        List<String> blocks = db.blocks(connectionId);
        for (String blocksUser : blocks){
            if (usernames.contains(blocksUser)){
                sendError(s);
                return;
            }
        }

        List<String> stats = db.stat(connectionId, usernames);
        if (stats == null){
            sendError(s);
        }
        else{
            for (String stat:stats){
                sendAck(s + stat);
            }
        }
    }

    private void BLOCK(String message){
        String s = "" + (char)1;
        s += (char)2;
        String username = message.substring(0,message.length()-1);
        boolean successful = db.block(connectionId, username);
        if (!successful){
            sendError(s);
        }
        else{
            sendAck(s);
        }
    }


    @Override
    public void process(Message message) {
        if (message == null){
            brutalLogoutRequest();
            return;
        }
        String command = message.getMessage();
        switch (message.getType()){
            case 1:
                registerRequest(command);
                break;
            case 2:
                loginRequest(command);
                break;
            case 3:
                logoutRequest();
                break;
            case 4:
                followRequest(command);
                break;
            case 5:
                postRequest(command);
                break;
            case 6:
                pmRequest(command);
                break;
            case 7:
                loggedInStatsRequests();
                break;
            case 8:
                statsRequests(command);
                break;
            case 12:
                BLOCK(command);
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
