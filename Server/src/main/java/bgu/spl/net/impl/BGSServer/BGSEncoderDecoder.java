package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BGSEncoderDecoder implements MessageEncoderDecoder<Message> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public Message decodeNextByte(byte nextByte) {
        if (nextByte == ';') {
            return popMessage();
        }
        pushByte(nextByte);
        return null; //not a line yet
    }

    private Message popMessage() {
        String OPCode = new String(bytes, 0, 2, StandardCharsets.UTF_8);
        short OPCode_short = (short)((int) OPCode.charAt(0)*10 + (int) OPCode.charAt(1));
        String Command = new String(bytes, 2, len-2, StandardCharsets.UTF_8);
        Message result = new MessageImpl(OPCode_short, Command);
        len = 0;
        return result;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }
    @Override
    public byte[] encode(Message message) {
        String output = message.getType() + message.getMessage() + ';';
        if (output.charAt(0) == '9') output = "0" + output;
        byte[] output_bytes = output.getBytes(StandardCharsets.UTF_8);
        output_bytes[0] -= 48;
        output_bytes[1] -= 48;
        if (output_bytes[2]>=48 && output_bytes[2]<=57) output_bytes[2] -= 48;
        if (output_bytes[3]>=48 && output_bytes[3]<=57) output_bytes[3] -= 48;
        return output_bytes;
    }
}
