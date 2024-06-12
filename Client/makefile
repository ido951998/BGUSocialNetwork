# All Targets
all: BGSclient


BGSclient: bin/connectionHandler.o bin/BGSEncDec.o bin/BGSClient.o
	g++ -o bin/BGSclient -pthread bin/connectionHandler.o bin/BGSEncDec.o bin/BGSClient.o


# Depends on the source and header files
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ -g -Wall -std=c++11 -pthread -c -Iinclude -o bin/connectionHandler.o src/connectionHandler.cpp
    
bin/BGSEncDec.o: src/BGSEncDec.cpp
	g++ -g -Wall -std=c++11 -pthread -c -Iinclude -o bin/BGSEncDec.o src/BGSEncDec.cpp

bin/BGSClient.o: src/BGSClient.cpp
	g++ -g -Wall -std=c++11 -pthread -c -Iinclude -o bin/BGSClient.o src/BGSClient.cpp


#Clean the build directory
clean:
	rm -f bin/*
