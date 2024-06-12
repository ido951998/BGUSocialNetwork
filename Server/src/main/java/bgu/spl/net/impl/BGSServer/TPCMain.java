package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        if (args.length == 1) {
            int port = Integer.parseInt(args[0]);
            Server.threadPerClient(
                    port, //port
                    BGSProtocol::new, //protocol factory
                    BGSEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
    }
}
