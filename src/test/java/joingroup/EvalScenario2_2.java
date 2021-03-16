package joingroup;

import client.HyperZMQ;
import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import txprocessor.CombinedTP;
import voting.*;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class EvalScenario2_2 {

    /**
     * The goal of this test is to measure the time it takes to create X groups with Y participants each and then disband the groups
     * This Test creates the HyperZMQ clients iteratively as opposed to the other which creates all clients upfront
     */
    @Test
    public void test() throws InterruptedException {
        //+----------------------------------------------------------------------------------------
        //                      PARAMETERS
        //-----------------------------------------------------------------------------------------
        int numOfGroups = 5;
        int numOfAgentsInGroup = 8;
        int numOfAgents = numOfAgentsInGroup * numOfGroups;

        int numOfRuns = 1;
        int numOfWarmups = 0;
        int threadDelayMS = 1000;

        String groupName = "testgroup";

        CombinedTP.main(null);
        //CombinedTP.main(new String[]{"tcp://192.168.178.55:4004"});
        //CombinedTP.main(new String[]{"tcp://192.168.178.61:4004"});

        Thread.sleep(1500);

        //+----------------------------------------------------------------------------------------
        //                      TEST SETUP
        //-----------------------------------------------------------------------------------------
        HyperZMQ h = new HyperZMQ.Builder("sdlfsdf", "sdfsd").build();
        h.isVolatile = true;

        // Clear the receipts
        for (int i = 0; i < numOfGroups; i++) {
            h.debugClearGroupMembers(groupName + String.valueOf(i + 1));
        }

        Thread.sleep(1000);

        //+----------------------------------------------------------------------------------------
        //                      ACTUAL TEST RUNS
        //-----------------------------------------------------------------------------------------
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < numOfRuns + numOfWarmups; i++) {
            // Prepare the threads
            int dhbasePort = 60001; // + (i * 100);
            int zmqbasePort = 5558;
            //List<GroupBuildingRunnable> gbrs = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            for (int y = 0; y < numOfGroups; y++) {
                GroupBuildingRunnable2 gbr = new GroupBuildingRunnable2(
                        numOfAgentsInGroup,
                        groupName + String.valueOf(y + 1),
                        dhbasePort + (y * 50),
                        zmqbasePort + y);
                //gbrs.add(gbr);
                threads.add(new Thread(gbr));
            }

            // Run all threads, measure time
            long start = System.currentTimeMillis();

            /*
            threads.get(0).start();
            threads.get(0).join();
            */

            for (Thread thread : threads) {
                thread.start();
                Thread.sleep(threadDelayMS);
            }

            for (Thread t : threads) {
                t.join();
            }

            long elapsed = System.currentTimeMillis() - start;

            times.add(elapsed);
            System.err.println("Elapsed time: " + elapsed + "ms");
            System.err.println("Times: " + times.toString());

            Thread.sleep(1000);
            // Cleanup for next run, clear the receipts
            for (int x = 0; x < numOfGroups; x++) {
                h.debugClearGroupMembers(groupName + String.valueOf(x + 1));
            }

            // If the group is not disbanded in the GroupBuildingRunnable, remove the group there so it is free for the next run
            //agents.forEach(HyperZMQ::debugClearGroups);

            Thread.sleep(2000);
        }

        System.err.println("Times: " + times.toString());
        OptionalDouble optAvg = times.stream().mapToLong(Long::longValue).average();
        if (optAvg.isPresent()) {
            System.err.println("Average: " + optAvg.getAsDouble());
        } else {
            System.err.println("Error while getting average from stream");
        }
    }
}
