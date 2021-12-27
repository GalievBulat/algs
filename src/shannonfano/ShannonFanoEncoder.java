
package shannonfano;

import java.io.*;

public class ShannonFanoEncoder {

    final int BYTE_SIZE = 256;
    private final CharNode[] charNodes = new CharNode[BYTE_SIZE];
    private final String[] charCodes = new String[BYTE_SIZE];
    private String source;
    private String destination;
    private int charsNum;
    private long fileLen;

    public ShannonFanoEncoder(String txt, String txt2) {
        loadFile(txt, txt2);
    }

    void init() {
        for (int i = 0; i < BYTE_SIZE; i++) {
            charNodes[i] = new CharNode((char) i, 0, "");
        }
        charsNum = 0;
        fileLen = 0;
    }

    public void loadFile(String txt, String txt2) {
        source = txt;
        destination = txt2;
        init();
    }

    private void setFrequencies(InputStream in) throws IOException {
        fileLen = in.available();
        if (fileLen == 0)
            return;
        long i = 0;
        in.mark((int) fileLen);
        charsNum = 0;
        while (i < fileLen) {
            int ch = in.read();
            i++;
            if (charNodes[ch].freq == 0)
                charsNum++;
            charNodes[ch].freq++;
        }
        in.reset();
    }
    private void sortByFrequency(){
        CharNode temp;
        boolean changed = true;
        for (int i = 0; i < BYTE_SIZE; i++) {
            if (changed) {
                changed = false;
                for (int j = 0; j < BYTE_SIZE - i - 1; j++)
                    if (charNodes[j].freq < charNodes[j + 1].freq) {
                        temp = charNodes[j];
                        charNodes[j] = charNodes[j + 1];
                        charNodes[j + 1] = temp;
                        changed = true;
                    }
            }
        }
    }
    private void writeToFile(InputStream in) throws Exception {
        FileBitWriter hFile = new FileBitWriter(destination);
        String buf;
        buf = completeByte(Long.toString(fileLen, 2), 32);
        hFile.putBits(buf);
        buf = completeByte(Integer.toString(charsNum - 1, 2), 8);
        hFile.putBits(buf);
        for (int i = 0; i < BYTE_SIZE; i++) {
            if (charCodes[i].length() != 0) {
                buf = completeByte(Integer.toString(i, 2), 8);
                hFile.putBits(buf);
                buf = completeByte(Integer.toString(charCodes[i].length(), 2), 5);
                hFile.putBits(buf);
                hFile.putBits(charCodes[i]);
            }
        }
        long bytesCount = 0;
        while (bytesCount < fileLen) {
            int ch = in.read();
            hFile.putBits(charCodes[ch]);
            bytesCount++;
        }
        hFile.closeFile();
    }
    public void encode() throws Exception {
        if (source.length() == 0)
            return;
        BufferedInputStream in;
        FileInputStream fin = new FileInputStream(source);
        in = new BufferedInputStream(fin);
        setFrequencies(in);
        sortByFrequency();
        createFanoTree(0, charsNum, "");
        for (int i = 0; i < BYTE_SIZE; i++)
            charCodes[i] = "";
        for (int i = 0; i < charsNum; i++)
            charCodes[charNodes[i].ch] = charNodes[i].sfCode;
        writeToFile(in);
    }

    void createFanoTree(int lowValue, int upperValue, String code) {
        long totalFreq = 0, sumTop, sumBot;
        if (lowValue >= upperValue)
            return;
        for (int i = lowValue; i < upperValue; i++) {
            charNodes[i].sfCode += code;
            totalFreq += charNodes[i].freq;
        }
        if (upperValue - lowValue == 1) {
            charNodes[lowValue].sfCode += "0";
            return;
        }
        if (upperValue - lowValue == 2) {
            charNodes[lowValue].sfCode += "0";
            charNodes[upperValue - 1].sfCode += "1";
            return;
        }
        int ind = upperValue - 1;
        long minDiff = totalFreq;
        int minInd = ind;
        while (ind >= lowValue) {
            sumBot = 0;
            for (int i = ind; i < upperValue; i++)
                sumBot += charNodes[i].freq;
            sumTop = totalFreq - sumBot;
            if (minDiff > Math.abs(sumBot - sumTop)) {
                minDiff = Math.abs(sumBot - sumTop);
                minInd = ind;
            }
            ind--;
        }
        createFanoTree(lowValue, minInd, "0");
        createFanoTree(minInd, upperValue, "1");
    }

    String completeByte(String txt, int n) {
        StringBuilder txtBuilder = new StringBuilder(txt);
        while (txtBuilder.length() < n)
            txtBuilder.insert(0, "0");
        txt = txtBuilder.toString();
        return txt;
    }

    public static class FileBitWriter {

        private String fileName;
        private BufferedOutputStream outf;
        private String currentByte;

        public FileBitWriter(String txt) throws Exception {
            fileName = txt;
            loadFile(fileName);
        }

        public void loadFile(String txt) throws Exception {
            fileName = txt;
            File outputFile = new File(fileName);
            FileOutputStream fout = new FileOutputStream(outputFile);
            outf = new BufferedOutputStream(fout);
            currentByte = "";
        }

        public void addBit(int bit) throws Exception {
            bit = bit % 2;
            currentByte = currentByte + bit;
            if (currentByte.length() >= 8) {
                int bytes = Integer.parseInt(currentByte.substring(0, 8), 2);
                outf.write(bytes);
                currentByte = "";
            }

        }

        public void putBits(String bits) throws Exception {

            while (bits.length() > 0) {
                int bit = Integer.parseInt(bits.substring(0, 1));
                addBit(bit);
                bits = bits.substring(1);
            }
        }

        public void closeFile() throws Exception {
            while (currentByte.length() > 0) {
                addBit(1);
            }
            outf.close();
        }
    }

    public static class CharNode {
        public char ch;
        public long freq;
        public String sfCode;

        CharNode(char c, long f, String code) {
            ch = c;
            freq = f;
            sfCode = code;
        }

    }
}

