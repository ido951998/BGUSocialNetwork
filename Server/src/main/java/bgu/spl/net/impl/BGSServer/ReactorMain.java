package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.srv.Server;

public class ReactorMain {
    public static void main(String[] args) {
        if (args.length == 2) {
            int port = Integer.parseInt(args[0]);
            int n_threads = Integer.parseInt(args[1]);
            Server.reactor(
                    n_threads,
                    port, //port
                    BGSProtocol::new, //protocol factory
                    BGSEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
    }
}
