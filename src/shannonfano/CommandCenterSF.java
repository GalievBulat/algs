package shannonfano;

import main.CommandCenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCenterSF {

    public static void main(String[] args) {
            List<String> list = Arrays.stream(args).collect(Collectors.toList());
            list.add(0, "shannon_fano");
            CommandCenter.main(list.toArray(new String[args.length+1]));
    }
}
