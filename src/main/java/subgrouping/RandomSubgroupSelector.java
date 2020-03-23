package subgrouping;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomSubgroupSelector implements ISubgroupSelector {
    @Override
    public List<String> selectSubgroup(List<String> input, int subgroupSize) {

        Random r = new Random();
        List<Integer> ints = new ArrayList<>();
        while (ints.size() < subgroupSize) {
            int number = r.nextInt(input.size() - 1);
            if (!ints.contains(number)) {
                ints.add(number);
            }
        }

        List<String> ret = new ArrayList<>();
        for (int i : ints) {
            ret.add(input.get(i));
        }
        return ret;
    }
}
