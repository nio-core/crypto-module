package contracts;

import java.util.List;

public interface IContractProcessor {
    Object processContract(Contract contract);

    List<String> getSupportedOperations();
}
