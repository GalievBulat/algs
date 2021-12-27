package haming;

import main.CommandCenter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCenterHamming {

    public static void main(String[] args) {
            List<String> list = Arrays.stream(args).collect(Collectors.toList());
            list.add(0, "hamming");
            CommandCenter.main(list.toArray(new String[args.length+1]));
    }
}
