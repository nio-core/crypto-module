import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import subgrouping.HashSubgroupSelector;
import subgrouping.ISubgroupSelector;
import subgrouping.RandomSubgroupSelector;
import util.Utilities;

public class SubgroupingTest {

    @org.junit.Test
    public void testHashSubgroupSelector() {
        ISubgroupSelector selector = new HashSubgroupSelector();
        List<String> inputs = generateInputs();

        List<String> selected1 = selector.selectSubgroup(inputs, 5);
        List<String> selected2 = selector.selectSubgroup(inputs, 5);

        // Check idempotency

        Assert.assertTrue(selected1.containsAll(selected2));
    }

    @Test
    public void testRandomSelector() {
        ISubgroupSelector selector = new RandomSubgroupSelector();
        List<String> inputs = generateInputs();
        List<String> selected = selector.selectSubgroup(inputs, 5);
        printList(selected);
        Assert.assertTrue(inputs.containsAll(selected));
    }

    private List<String> generateInputs() {
        List<String> inputs = new ArrayList<>();
        // Fill input with random strings, in usage the strings represent public keys
        for (int i = 0; i < 10; i++) {
            inputs.add(Utilities.generateNonce(70));
        }
        return inputs;
    }

    private void printList(List<String> input) {
        System.out.println("[");
        for (String s : input) {
            System.out.println(s + ",");
        }
        System.out.println("]");
    }
}
