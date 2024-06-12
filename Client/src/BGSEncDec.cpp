#include "BGSEncDec.h"
#include <vector>

using std::string;

string inputParser(string& line){
    string ans;
    if (line[1] == 1){ //error
        ans = "ERROR";
        ans += line[2]+48;
        ans += line[3]+48;
    }
    else if (line[1] == 9){ //notification
        ans = "NOTIFICATION ";
        if (line[2] == 1){
            ans += "PM ";
        }
        else{
            ans += "Public ";
        }

        unsigned int i=3;
        for (; i<line.length()-1; i++){
            if (line[i] == 0){
                ans += " ";
            }
            else{
                ans += line[i];
            }
        }
    }
    else{ //ack
        ans = "ACK";
        ans += line[2]+48;
        ans += line[3]+48;
        if (line[3] == 4){
            if (line[5] == '8'){
                ans += " 0 ";
            }
            else{
                ans += " 1 ";
            }
            for (unsigned int i = 6; i < line.length()-1; i++) {
                ans += line[i];
            }
        }
        else {
            for (unsigned int i = 4; i < line.length(); i++) {
                ans += line[i];
            }
        }
    }
    return ans;
}

std::string BGSEncDec::Decode(std::string line) {
    return inputParser(line);
}

void getTime(string& ans){
    time_t now = time(0);
    tm *ltm = localtime(&now);
    string year = std::to_string(1900+ltm->tm_year);
    string month = std::to_string(1 + ltm->tm_mon);
    string day = std::to_string(ltm->tm_mday);
    string hour = std::to_string(ltm->tm_hour);
    string minute = std::to_string(ltm->tm_min);

    if (month.length() == 1){
        month = "0" + month;
    }
    if (day.length() == 1){
        day = "0" + day;
    }
    if (hour.length() == 1){
        hour = "0" + hour;
    }
    if (minute.length() == 1){
        minute = "0" + minute;
    }
    ans = day + "-" + month + "-" + year + " " + hour + ":" + minute;
}

string parseOutput(string& line){
    string ans = "";
    std::vector<std::string> keywords;
    int counter=0;
    string temp;
    for (unsigned int i=0; i<line.length(); i++){
        if (line[i] == ' '){
            counter++;
            keywords.push_back(temp);
            temp = "";
        }
        else if (line[i] == '\n') continue;
        else{
            temp += line[i];
        }
    }
    keywords.push_back(temp);
    if (keywords.at(0) == "REGISTER"){
        ans += (char)0;
        ans += (char)1;
        ans += keywords[1];
        ans += (char)0;
        ans += keywords[2];
        ans += (char)0;
        ans += keywords[3];
        ans += (char)0;
    }
    else if (keywords.at(0) == "LOGIN"){
        ans += (char)0;
        ans += (char)2;
        ans += keywords[1];
        ans += (char)0;
        ans += keywords[2];
        ans += (char)0;
        ans += keywords[3][0] - 48;

    }
    else if (keywords.at(0) == "LOGOUT"){
        ans += (char)0;
        ans += (char)3;
    }
    else if (keywords.at(0) == "FOLLOW"){
        ans += (char)0;
        ans += (char)4;
        ans += keywords[1];
        ans += keywords[2];
    }
    else if (keywords.at(0) == "POST"){
        ans += (char)0;
        ans += (char)5;
        for (unsigned int i = 1; i<keywords.size(); i++) {
            ans += keywords[i] + " ";
        }
        ans = ans.substr(0,ans.length()-1);
        ans += (char)0;
    }
    else if (keywords.at(0) == "PM"){
        string time;
        getTime(time);
        ans += (char)0;
        ans += (char)6;
        ans += keywords[1];
        ans += (char)0;
        for (unsigned int i = 2; i<keywords.size(); i++) {
            ans += keywords[i] + " ";
        }
        ans = ans.substr(0,ans.length()-1);
        ans += (char)0;
        ans += time;
        ans += (char)0;
    }
    else if (keywords.at(0) == "LOGSTAT"){
        ans += (char)0;
        ans += (char)7;
    }
    else if (keywords.at(0) == "STAT"){
        ans += (char)0;
        ans += (char)8;
        ans += keywords[1];
        ans += (char)0;
    }
    else{//this is BLOCK
        ans += (char)1;
        ans += (char)2;
        ans += keywords[1];
        ans += (char)0;
    }
    return ans;
}

std::string BGSEncDec::Encode(std::string line) {
    return parseOutput(line);
}
