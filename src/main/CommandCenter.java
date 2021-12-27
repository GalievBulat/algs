package main;

import org.apache.commons.io.IOUtils;
import shannonfano.ShannonFanoDecoder;
import shannonfano.ShannonFanoEncoder;
import arithmetic.ArithmeticCoding;
import lzwbwt.BWT;
import lzwbwt.LZW;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static haming.Hamming.decodeString;
import static haming.Hamming.encode;

public class CommandCenter {

    public static void main(String[] args) {
        try {
            String algorithm;
            String mode;
            String source;
            String destination;
            String len = "";
            String dictPath;
            Mode execMode;
            if (args.length >= 4 ) {
                algorithm = args[0];
                mode = args[1];
                source = args[2];
                destination = args[3];
            } else {
                System.out.println("unknown command");
                return;
            }
            switch (mode) {
                case "-e":
                    execMode = Mode.ENCODE;
                    break;
                case "-d":
                    execMode = Mode.DECODE;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mode);
            }
            switch (algorithm){
                case "btw_lzw":
                    startBTW_LZW(execMode, source, destination);
                    break;
                case "hamming":
                    startHamming(execMode, source, destination);
                    break;
                case "shannon_fano":
                    startShannon_Fano(execMode, source, destination);
                    break;
                case "arithmetic":
                    dictPath = args[4];
                    if(execMode == Mode.DECODE)
                        len = args[5];
                    startArithmetic(execMode, source, destination, dictPath,
                            execMode == Mode.ENCODE ? 0 : Integer.parseInt(len));
                    break;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
    private static InputStream readFile(String source) throws FileNotFoundException {
        File file = new File(source);
        return new FileInputStream(file);
    }

    private static void writeFile(String outputFile, byte[] bytes) {
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void startBTW_LZW(Mode mode,
                              String source,
                              String destination) throws IOException {
        if (mode == Mode.ENCODE){
            String enc = BWT.encode(FileUtils.readFileToString(new File(source), StandardCharsets.UTF_8));
            LZW.encode(enc, new BufferedOutputStream(new FileOutputStream(destination)));
        } else if (mode == Mode.DECODE) {
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            LZW.decode(source, buff);
            String dec = BWT.decode(String.valueOf(buff));
            FileUtils.write(new File(destination), dec, StandardCharsets.UTF_8);
        }
    }
    private static void startLZW(Mode mode,
                                     String source,
                                     String destination) throws IOException {
        if (mode == Mode.ENCODE){

            LZW.encode(FileUtils.readFileToString(new File(source), StandardCharsets.UTF_8), new BufferedOutputStream(new FileOutputStream(destination)));
        } else if (mode == Mode.DECODE) {
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            LZW.decode(source, buff);
            FileUtils.write(new File(destination), String.valueOf(buff), StandardCharsets.UTF_8);
        }
    }
    private static void startHamming(Mode mode,
                                     String source,
                                     String destination) throws IOException {
        byte[] bytes = new byte[0];
        if (mode == Mode.ENCODE) {
                bytes = encode(IOUtils.toByteArray(readFile(source)));
        } else if(mode == Mode.DECODE){
                bytes = decodeString(IOUtils.toByteArray(readFile(source)));
        }
        writeFile(destination, bytes);
    }
    private static void startArithmetic(Mode mode,
                                        String source,
                                        String destination,
                                        String dictPath,
                                        int len) throws Exception {
        ArithmeticCoding arithmeticCoding = new ArithmeticCoding();

        String text = FileUtils.readFileToString(new File(source), StandardCharsets.UTF_8);
        File dest = new File(destination);
        if (mode == Mode.ENCODE) {
            BigDecimal results = arithmeticCoding.encode(text, dictPath);
            FileUtils.write(dest, results.toString(), StandardCharsets.UTF_8);
            startShannon_Fano(Mode.ENCODE, source, destination+ "2");
        } else if(mode == Mode.DECODE){
            FileUtils.write(dest, arithmeticCoding.decode(text, dictPath, len), StandardCharsets.UTF_8);
            startShannon_Fano(Mode.DECODE, source + "2", destination);
        }
    }

    private static void startShannon_Fano(Mode mode,
                                        String source,
                                        String destination) throws Exception {
        if (mode == Mode.ENCODE) {
            ShannonFanoEncoder encoder = new ShannonFanoEncoder(source, destination);
            encoder.encode();
        } else if(mode == Mode.DECODE){
            ShannonFanoDecoder encoder = new ShannonFanoDecoder(source, destination);
            encoder.decodeFile();
        }
    }
    private enum Mode{
        ENCODE, DECODE
    }
}
