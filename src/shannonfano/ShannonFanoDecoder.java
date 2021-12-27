package shannonfano;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ShannonFanoDecoder {

    final int BYTE_SIZE = 256;
    private final String[] hCodes = new String[BYTE_SIZE];
    private String fileName, outputFilename;


    public ShannonFanoDecoder(String txt, String txt2) {
        loadFile(txt, txt2);
    }

    public void loadFile(String txt, String txt2) {
        fileName = txt;
        outputFilename = txt2;
    }
    private long openFile(InputBitStream fin) throws Exception {
        long i;
        long length = Long.parseLong(fin.getBits(32), 2);
        int distinctChars = Integer.parseInt(fin.getBits(8), 2) + 1;
        for (i = 0; i < distinctChars; i++) {

            int ch = Integer.parseInt(fin.getBits(8), 2);
            int len = Integer.parseInt(completeByte(fin.getBits(5)), 2);
            hCodes[ch] = fin.getBits(len);
        }
        return length;
    }

    public void decodeFile() throws Exception {
        if (fileName.length() == 0)
            return;
        for (int i = 0; i < BYTE_SIZE; i++)
            hCodes[i] = "";
        InputBitStream fin = new InputBitStream(fileName);
        long fileSize = openFile(fin);

        String buf;
        FileOutputStream fileOutputStream =
                new FileOutputStream(outputFilename);
        long i = 0;
        int k;
        int ch;
        while (i < fileSize) {
            buf = "";
            for (k = 0; k < 32; k++) {
                buf += fin.getBit();
                ch = findByCode(buf);
                if (ch > -1) {
                    fileOutputStream.write((char) ch);
                    i++;
                    break;
                }
            }
        }
        fileOutputStream.close();
    }

    int findByCode(String code) {
        int ret = -1;
        for (int i = 0; i < BYTE_SIZE; i++) {
            if (!hCodes[i].equals("") && code.equals(hCodes[i])) {
                ret = i;
                break;
            }
        }
        return ret;
    }


    String completeByte(String txt) {
        StringBuilder txtBuilder = new StringBuilder(txt);
        while (txtBuilder.length() < 8)
            txtBuilder.insert(0, "0");
        txt = txtBuilder.toString();
        return txt;
    }


    public static class InputBitStream {
        private String fileName;
        private BufferedInputStream in;
        private String currentByte;


        public InputBitStream(String txt) throws Exception {
            fileName = txt;
            loadFile(fileName);

        }

        public void loadFile(String txt) throws Exception {
            fileName = txt;

            File inputFile = new File(fileName);
            FileInputStream fin = new FileInputStream(inputFile);
            in = new BufferedInputStream(fin);
            currentByte = "";
        }

        String completeByte(String txt) {
            StringBuilder txtBuilder = new StringBuilder(txt);
            while (txtBuilder.length() < 8)
                txtBuilder.insert(0, "0");
            txt = txtBuilder.toString();
            return txt;
        }

        public String getBit() throws Exception {
            if (currentByte.length() == 0 && in.available() >= 1) {
                int k = in.read();
                currentByte = Integer.toString(k, 2);
                currentByte = completeByte(currentByte);
            }
            if (currentByte.length() > 0) {
                String ret = currentByte.substring(0, 1);
                currentByte = currentByte.substring(1);
                return ret;
            }
            return "";
        }

        public String getBits(int n) throws Exception {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < n; i++) {
                ret.append(getBit());
            }
            return ret.toString();
        }

        public String getBytes(int n) throws Exception {
            StringBuilder ret = new StringBuilder();
            String temp;
            for (int i = 0; i < n; i++) {
                temp = getBits(8);
                char k = (char) Integer.parseInt(temp, 2);
                ret.append(k);
            }
            return ret.toString();
        }

        public long available() throws Exception {
            return in.available();

        }


    }
}


