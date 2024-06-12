#include <stdlib.h>
#include "connectionHandler.h"
#include "BGSEncDec.h"
#include <atomic>
#include <thread>

class Reader {
private:
    ConnectionHandler& connectionHandler;
    BGSEncDec& encdec;
public:
    Reader(ConnectionHandler& ch, BGSEncDec& ed) : connectionHandler(ch), encdec(ed) {}
    void operator()() {
        int len;
        while(1){
            std::string answer;
            // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
            // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
            bool success = connectionHandler.getLine(answer);
            if (!success) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                return;
            }
            len=answer.length();
            // A C string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
            // we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
            answer.resize(len-1);
            answer = encdec.Decode(answer);
            std::cout << answer << std::endl;
            if (answer == "ACK03") {
                return;
            }
        }
    }
};

class Sender {
private:
    ConnectionHandler& connectionHandler;
    BGSEncDec& encdec;
public:
    Sender(ConnectionHandler& ch, BGSEncDec& ed) : connectionHandler(ch), encdec(ed) {}
    void operator()() {
        while(1){
            const short bufsize = 1024;
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            line = encdec.Encode(line);
            if (!connectionHandler.sendLine(line)) {
                return;
            }
        }
    }
};

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    BGSEncDec encdec = BGSEncDec();
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    Reader reader(connectionHandler, encdec);
    Sender sender(connectionHandler, encdec);

    std::thread th1(std::ref(reader)); // we use std::ref to avoid creating a copy of the Task object
    std::thread th2(std::ref(sender));

    th1.join();
    th2.detach();

    return 0;
}