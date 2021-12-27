package lzwbwt;

import java.io.*;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class LZWDecoder extends FilterInputStream {

    private final HashMap<Integer, IntBuffer> dictionary = new LinkedHashMap<>();
    private int currentChar = 0;
    private IntBuffer oldPhrase = null;
    private int code = 256;
    private final BitInputStream bis;
    private final PipedInputStream pipedInputStream = new PipedInputStream();
    private final PipedOutputStream pipedOutputStream = new PipedOutputStream();
    private final DataOutputStream dataOutputStream = new DataOutputStream(pipedOutputStream);

    public LZWDecoder(InputStream in) {
        super(in);
        bis = new BitInputStream(in);
    }

    @Override
    public int read() throws IOException {
        if (pipedInputStream.available() == 0) {
            try {
                extractBuffer();
            } catch (EOFException ex) {
                return -1;
            }
        }
        return pipedInputStream.read();
    }

    private void extractBuffer() throws IOException {
        int currCode;
        if ((currCode = readInt(bis)) == -1) {
            throw new EOFException("EOF");
        }
        if (oldPhrase == null) {
            pipedOutputStream.connect(pipedInputStream);
            currentChar = currCode;
            oldPhrase = toBuffer(currentChar);
            writeIntBuffer(dataOutputStream, oldPhrase);
            oldPhrase.rewind();
            return;
        }
        IntBuffer currIntBuffer = toBuffer(currCode);
        IntBuffer phrase;
        if (isByte(currCode)) {
            phrase = currIntBuffer;
        } else {
            if (dictionary.get(currCode) != null) {
                phrase = (IntBuffer) dictionary.get(currCode).rewind();
            } else {
                phrase = (IntBuffer) IntBuffer.allocate(oldPhrase.limit() + 1)
                        .put((IntBuffer) oldPhrase.rewind())
                        .put(currentChar).rewind();
            }
        }

        writeIntBuffer(dataOutputStream, phrase);

        currentChar = phrase.get(0);
        currIntBuffer = toBuffer(currentChar);
        IntBuffer oldPhraseWithCurrentChar = (IntBuffer) IntBuffer.allocate(oldPhrase.limit() + currIntBuffer.limit())
                .put((IntBuffer) oldPhrase.rewind())
                .put(currIntBuffer)
                .rewind();
        dictionary.put(code, oldPhraseWithCurrentChar);
        code++;
        oldPhrase = phrase;
    }

    private void writeIntBuffer(DataOutputStream output, IntBuffer buffer) throws IOException {
        buffer.rewind();
        for (int value : buffer.array()) {
            if (isByte(value)) {
                output.write(value);
            } else {
                output.writeInt(value);
            }
        }
    }

    private int readInt(BitInputStream input) throws IOException {
        int c = 0;
        int bit;
        while ((bit = input.readBit()) == 0) {
            c++;
        }
        if (bit == -1) {
            return -1;
        }
        StringBuilder bin = new StringBuilder("1");
        for (int i = 0; i < c; i++) {
            bit = input.readBit();
            if (bit == -1) {
                throw new EOFException();
            }
            bin.append(bit);
        }
        return (int) Long.parseLong(bin.toString(), 2) - 1;
    }

    private boolean isByte(int value) {
        return value == (byte) value;
    }

    private IntBuffer toBuffer(int value) {
        return (IntBuffer) IntBuffer.allocate(1).put(value).rewind();
    }

    @Override
    public void close() throws IOException {
        super.close();
        pipedInputStream.close();
        pipedOutputStream.close();
        dataOutputStream.close();
    }

    public static class BitInputStream implements Closeable {

        private InputStream input;
        private int digits;
        private int numDigits;
        private boolean isFirstByteRead;

        private static final int BYTE_SIZE = 8;

        public BitInputStream(InputStream input) {
            this.input = input;
            isFirstByteRead = false;
        }

        public int readBit() throws IOException {
            if (!isFirstByteRead) {
                nextByte();
            }
            if (digits == -1) {
                return -1;
            }
            int result = digits % 2;
            digits /= 2;
            numDigits++;
            if (numDigits == BYTE_SIZE) {
                nextByte();
            }
            return result;
        }

        private void nextByte() throws IOException {
            digits = input.read();
            numDigits = 0;
            isFirstByteRead = true;
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        protected void finalize() throws IOException {
            close();
        }
    }
}
