package subgrouping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This implementation selects a subgroup by hashing the input generating a stream of unsigned integer values i0, i1, ...iX for X = input.size()
 * Of this stream the first (subgroupSize) ints which fall in the range of the input size are used as index for the subgroup
 */
public class HashSubgroupSelector implements ISubgroupSelector {

    public HashSubgroupSelector() {
    }

    @Override
    public List<String> selectSubgroup(List<String> input, int subgroupSize) {
        MessageDigest md;
        try {
            // TODO Hash until there are enough ints first
            md = MessageDigest.getInstance("SHA-1");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        byte[] digestOutput = md.digest(input.toString().getBytes(StandardCharsets.UTF_8));

        List<Integer> ints = new ArrayList<>();
        for (byte b : digestOutput) {
            ints.add((b & 0xFF)); // remove sign
        }

        List<Integer> ints2 = ints.stream().filter(i -> i <= input.size()).distinct().collect(Collectors.toList());

        List<String> subgroup = new ArrayList<>();
        int count = 0;


        /* is = IntStream.range(0, digestOutput.length).map(i -> digestOutput[i]);
        System.out.println(is.collect(StringBuilder::new,
                StringBuilder::appendCodePoint,
                StringBuilder::append)
                .toString()); */
       /* ByteArrayInputStream inputStream = new ByteArrayInputStream(digestOutput);
        IntStream is2 = IntStream.generate(inputStream::read).limit(inputStream.available());

        */
        return Collections.emptyList();
    }
}
