package subgrouping;

import java.util.List;

/**
 * Implementations of this interface implement an algorithm which selects a subgroup from the input with the given size.
 * The algorithm is required to be idempotent // TODO is it?
 */
public interface ISubgroupSelector {
    /**
     *
     * @param input inputs
     * @param subgroupSize desired output size
     * @return a list of strings or an empty list upon failure
     */
    List<String> selectSubgroup(List<String> input, int subgroupSize);
}
