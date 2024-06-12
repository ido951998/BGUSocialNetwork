package bgu.spl.net.srv;

import bgu.spl.net.api.bidi.Connections;
import java.util.HashMap;


public class ConnectionsImpl<T> implements Connections<T> {
    HashMap<Integer,ConnectionHandler<T>> map;
    Integer nextID;

    public ConnectionsImpl(){
        map = new HashMap<>();
        nextID = 0;
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (!map.containsKey(connectionId)) return false;
        map.get(connectionId).send(msg);
        return true;
    }

    @Override
    public void broadcast(T msg) {
        for (ConnectionHandler<T> connectionHandler : map.values()){
            connectionHandler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        map.remove(connectionId);
    }

    public int connect(ConnectionHandler<T> connectionHandler){
        map.put(nextID, connectionHandler);
        nextID++;
        return nextID-1;
    }
}
