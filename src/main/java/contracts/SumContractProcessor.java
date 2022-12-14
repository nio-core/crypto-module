package contracts;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SumContractProcessor implements IContractProcessor {

    private static final String SUPPORTED_OPERATION = "sum";

    public SumContractProcessor() {
    }

    @Override
    public Object processContract(Contract contract) {
        if (!SUPPORTED_OPERATION.equals(contract.getOperation())) {
            return null;
        }
        List<Integer> ints = contract.getArgs().stream().map(x -> {
            int r;
            try {
                r = Integer.parseInt(x);
            } catch (NumberFormatException e) {
                // Just skip false inputs
                return 0;
            }
            return r;
        }).collect(Collectors.toList());

        return ints.stream().reduce(0, (a, i) -> a += i);
    }

    @Override
    public List<String> getSupportedOperations() {
        return Collections.singletonList(SUPPORTED_OPERATION);
    }
}
