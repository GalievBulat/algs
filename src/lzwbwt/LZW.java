package lzwbwt;

import java.io.*;

public class LZW {
    public static void encode(String text,
                              BufferedOutputStream dest) throws IOException {
        InputStream decInputStream = null;
        LZWEncoder lzwOutputStream = null;
        try {
            decInputStream = new BufferedInputStream(new ByteArrayInputStream(text.getBytes()));
            lzwOutputStream = new LZWEncoder(dest);
            copy(decInputStream, lzwOutputStream);
        } finally {
            closeStream(lzwOutputStream, decInputStream, dest);
        }
    }
    public static void decode(String source,
                              OutputStream destination) throws IOException{

        InputStream encInputStream = null;
        LZWDecoder lzwDecoder = null;
        try {
            encInputStream = new BufferedInputStream(new FileInputStream(source));
            lzwDecoder = new LZWDecoder(encInputStream);
            copy(lzwDecoder, destination);
        } finally {
            closeStream(lzwDecoder, encInputStream, destination);
        }
    }
    public static void closeStream(Closeable... closeable) {
        for (Closeable c : closeable) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int read;
        while ((read = input.read()) != -1) {
            output.write(read);
        }
        output.flush();
    }
}
