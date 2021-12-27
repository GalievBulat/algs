package arithmetic;

import com.sun.deploy.util.ArrayUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ArithmeticCoding {
    public BigDecimal curIntervalStart;
    public BigDecimal curIntervalFinish;
    public BigDecimal curIntervalSize;
    private Map<Byte, Interval> characterIntervalMap  = new HashMap<>();;
    private static final BigDecimal ZERO = new BigDecimal(0);
    private final Map<Byte, Integer> frequencies = new HashMap<>();

    public void setProbabilitiesByDictionary(Map<Byte, BigDecimal> dictionary) {
        characterIntervalMap = new HashMap<>();
        List<BigDecimal> cumulative = new ArrayList<>();

        for (Byte c : dictionary.keySet()) {
            if (cumulative.size() == 0){
                cumulative.add(dictionary.get(c));
                if (dictionary.get(c).compareTo(ZERO) != 0) {
                    characterIntervalMap.put(c,
                            new Interval(ZERO ,dictionary.get(c)));
                }
            } else {
                cumulative.add(cumulative.get(cumulative.size() - 1).add(dictionary.get(c)));
                if (dictionary.get(c).compareTo(ZERO) != 0) {
                    characterIntervalMap.put(c,
                            new Interval(cumulative.get(cumulative.size() - 2) , cumulative.get(cumulative.size() - 1)));
                }
            }
        }
    }

    private Map<Byte, BigDecimal> generateProbabilities(String data) {
        final TreeMap<Byte, BigDecimal> probes = new TreeMap<>();
        byte[] bytes = data.getBytes();
        System.out.println(bytes.length);
        for (byte c : bytes) {
            int fr = 0;
            if (frequencies.containsKey(c)){
                fr = frequencies.get(c);
            }
            frequencies.put(c, ++fr);
        }
        for (Byte c : frequencies.keySet()) {
            probes.put(c,  new BigDecimal(frequencies.get(c))
                                .divide(new BigDecimal(data.length()),MathContext.DECIMAL128));
        }
        return probes;
    }

    public BigDecimal encode(String data, String dictPath) throws IOException {
        init();
        if (characterIntervalMap == null || characterIntervalMap.isEmpty())
            setProbabilitiesByDictionary(generateProbabilities(data));
        System.out.println(characterIntervalMap.size());
        byte[] bytes = data.getBytes();
        for (byte current : bytes) {
            curIntervalFinish = curIntervalStart
                    .add(curIntervalSize.
                            multiply(getHighRange( current)));
            curIntervalStart = curIntervalStart
                    .add(curIntervalSize.multiply(getLowRange(current)));
            curIntervalSize = curIntervalFinish
                    .subtract(curIntervalStart);
        }
        saveDictionary(dictPath);
        return curIntervalStart
                .add(curIntervalFinish)
                .divide(new BigDecimal(2), MathContext.DECIMAL128);
    }
    private void parseDictionary(String path) throws IOException {
        InputStreamReader fs =new InputStreamReader(new BufferedInputStream(new FileInputStream(path)));
        List<String> lines = IOUtils.readLines(fs);
        for (String line : lines){
            String[] inter = line.split("\\|");
            String ch = inter[0];
            String[] nums = inter[1].split(";");
            characterIntervalMap.put((byte) Byte.parseByte(ch), new Interval(new BigDecimal(nums[0]), new BigDecimal(nums[1])));
        }
    }
    private void saveDictionary(String path) throws IOException {
        String res = characterIntervalMap.entrySet().stream().map((Map.Entry<Byte, Interval> e)->
                 ( e.getKey() )+ "|" + e.getValue().start.toString() + ";" + e.getValue().end.toString())
                .collect(Collectors.joining("\n"));
        FileUtils.write(new File(path), res, StandardCharsets.UTF_8);
    }


    public String decode(String data, String dictPath, int numberOfSteps) throws Exception {
        init();
        parseDictionary(dictPath);
        BigDecimal value = new  BigDecimal(data);
        return decodeDecimal(value, numberOfSteps);
    }

    public BigDecimal getLowRange(byte s) {
        return characterIntervalMap.get(s).start;
    }

    public BigDecimal getHighRange(byte s) {
        return characterIntervalMap.get(s).end;
    }

    public static String DecimalToFloat(BigDecimal low, BigDecimal high) {
        int idx = 1;
        BigDecimal num = new BigDecimal("0");
        StringBuilder ret = new StringBuilder();
        while (true) {
            int lowCmp = low.compareTo(num);
            int highCmp = high.compareTo(num);
            BigDecimal divide = BigDecimal.ONE.divide(new BigDecimal("2").pow(idx));
            if (lowCmp > 0) {
                num = num.add(divide);
                ret.append(1);
            } else if (highCmp < 0) {
                num = num.subtract(divide);
                ret.replace(ret.length() - 1, ret.length(), "01");
            } else {
                return ret.toString();
            }
            ++idx;
        }
    }

    public Map.Entry<Byte, Interval> findByValue(BigDecimal val) throws Exception {
        for (Map.Entry<Byte, Interval> curr : characterIntervalMap.entrySet()) {
            Interval values = curr.getValue();
            int lowCmp = val.compareTo(values.start);
            int highCmp = val.compareTo(values.end);
            if (lowCmp >= 0 && highCmp <= 0) return curr;
        }
        throw new Exception("Character is not found");
    }

    private void init() {
        curIntervalStart = new BigDecimal(0);
        curIntervalFinish = new BigDecimal(1);
        curIntervalSize = new BigDecimal(1);
    }
    List<Byte> bytes = new ArrayList<>();
    private String decodeDecimal(BigDecimal value, int numberOfSteps) throws Exception {
        int ren = numberOfSteps;
        //?!
        while ((numberOfSteps-=1) >= 0) {
            Map.Entry<Byte, Interval> curr = findByValue(value);
            bytes.add(curr.getKey());
            curIntervalStart = curr.getValue().start;
            curIntervalFinish = curr.getValue().end;
            curIntervalSize = curIntervalFinish
                    .subtract(curIntervalStart);
            value = value.subtract(curIntervalStart)
                    .divide(curIntervalSize, MathContext.DECIMAL128);
        }
        byte[] bs = new byte[ren];
        for (int i = 0; i < bytes.size(); i++) {
            bs[i] = bytes.get(i);
        }
            return new String(bs, StandardCharsets.UTF_8);
    }
    public static class Interval{
        BigDecimal start;
        BigDecimal end;
        public Interval(BigDecimal int1, BigDecimal int2) {
            start = int1;
            end = int2;
        }
    }
}
