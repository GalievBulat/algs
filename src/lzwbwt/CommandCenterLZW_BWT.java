package lzwbwt;

import main.CommandCenter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCenterLZW_BWT {

    public static void main(String[] args) {
            List<String> list = Arrays.stream(args).collect(Collectors.toList());
            list.add(0, "btw_lzw");
            CommandCenter.main(list.toArray(new String[args.length+1]));
    }
}
