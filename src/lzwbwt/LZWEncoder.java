package lzwbwt;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class LZWEncoder extends FilterOutputStream {

    private final HashMap<ByteBuffer, Integer> dictionary = new LinkedHashMap<>();
    private ByteBuffer phrase = null;
    private int code = 256;
    private boolean isFlushed = false;

    private int digits;
    private int numDigits;

    private static final int BYTE_SIZE = 8;


    public LZWEncoder(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        if (phrase == null) {
            phrase = (ByteBuffer) ByteBuffer.allocate(1).put((byte) b).rewind();
            return;
        }
        phrase.rewind();
        ByteBuffer currentChar =
                (ByteBuffer) ByteBuffer.allocate(1).put((byte) b).rewind();
        ByteBuffer phraseWithCurrentChar =
                (ByteBuffer) ByteBuffer.allocate(phrase.limit() + currentChar.limit())
                        .put(phrase).put(currentChar).rewind();
        if (dictionary.get(phraseWithCurrentChar) != null) {
            phrase = phraseWithCurrentChar;
        } else {
            if (phrase.limit() > 1) {
                writeInt(dictionary.get(phrase.rewind()));
            } else {
                writeInt(phrase.get(0));
            }
            dictionary.put((ByteBuffer) phraseWithCurrentChar.rewind(), code);
            code++;
            phrase = currentChar;
        }
        isFlushed = false;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (!isFlushed) {
            if (phrase.limit() > 1) {
                writeInt(dictionary.get(phrase));
            } else {
                writeInt(phrase.get(0));
            }
        }
        isFlushed = true;
        super.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        if (numDigits > 0) {
            flushBits();
        }
        out.close();
    }

    private void writeInt(int value) throws IOException {
        String bin = Integer.toBinaryString(value + 1);
        int c = bin.length() - 1;
        for (int i = 0; i < c; i++) {
            writeBit(0);
        }
        for (int i = 0; i <= c; i++) {
            writeBit(bin.charAt(i) == '1' ? 1 : 0);
        }
    }

    private void writeBit(int bit) throws IOException {
        if (bit < 0 || bit > 1) {
            throw new IllegalArgumentException("Illegal bit: " + bit);
        }
        digits += bit << numDigits;
        numDigits++;
        if (numDigits == BYTE_SIZE) {
            flushBits();
        }
    }

    private void flushBits() throws IOException {
        out.write(digits);
        digits = 0;
        numDigits = 0;
    }
}
