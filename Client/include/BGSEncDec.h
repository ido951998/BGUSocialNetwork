#ifndef CLIENT3_BGSENCDEC_H
#define CLIENT3_BGSENCDEC_H
#include <string>

class BGSEncDec {
    public:
        std::string Encode(std::string);
        std::string Decode(std::string);
};


#endif //CLIENT3_BGSENCDEC_H
