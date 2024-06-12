package bgu.spl.net.impl.BGSServer;

public class MessageImpl implements Message{
    String msg;
    short type;

    public MessageImpl(short type, String msg){
        this.type = type;
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return msg;
    }

    @Override
    public short getType() {
        return type;
    }
}
