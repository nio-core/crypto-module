package joingroup;

import client.HyperZMQ;
import org.junit.Test;
import voting.GroupVotingProcess;
import voting.SimpleMajorityEvaluator;
import voting.YesVoteStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EvalScenario2 {

    /**
     * The goal of this test is to measure the time it takes to create 5 groups with 2 participants each, then disband
     * and create another 5 groups with 2 different participants
     */
    @Test
    public void test() throws InterruptedException {
        int numOfGroups = 1;
        int numOfAgentsInGroup = 2; // fixed
        int numOfAgents = numOfAgentsInGroup * numOfGroups;
        String groupName = "testgroup";

        List<HyperZMQ> creatingAgents = new ArrayList<>();
        List<HyperZMQ> joiningAgents = new ArrayList<>();

        // Create agents, setup voting behavior
        for (int i = 0; i < numOfAgents; i++) {
            HyperZMQ tmp = new HyperZMQ.Builder("agent" + String.valueOf(i + 1), "whatever").build();
            tmp.isVolatile = true; // Don't save any data
            tmp.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(tmp.getClientID()));
            tmp.getVoteManager().setVotingProcessGroup(new GroupVotingProcess(tmp));
            tmp.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(20));

            if (i % 2 == 0) {
                creatingAgents.add(tmp);
            } else {
                joiningAgents.add(tmp);
            }
        }

        assertEquals(joiningAgents.size(), creatingAgents.size());

        // Clear the receipts
        for (int i = 0; i < numOfGroups; i++) {
            creatingAgents.get(0).debugClearGroupMembers(groupName + String.valueOf(i + 1));
        }

        Thread.sleep(1000);

        // Prepare the threads
        int basePort = 5555;
        List<GroupBuildingRunnable> gbrs = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < creatingAgents.size(); i++) {
            GroupBuildingRunnable gbr = new GroupBuildingRunnable(
                    creatingAgents.get(i),
                    joiningAgents.get(i),
                    groupName + String.valueOf(i + 1),
                    basePort + i);
            gbrs.add(gbr);
            threads.add(new Thread(gbr));
        }

        //+-----------------------------------------------------------------------------------------------------------------------
        // ACTUAL TEST RUNS
        //------------------------------------------------------------------------------------------------------------------------
        int numOfRuns = 1;
        int numOfWarmups = 0;

        List<Long> times = new ArrayList<>();

        for (int i = 0; i < numOfRuns + numOfWarmups; i++) {
            // Run all threads, measure time
            long start = System.currentTimeMillis();

            /*
            threads.get(0).start();
            threads.get(0).join();
            */

            threads.forEach(Thread::start);
            for (Thread t : threads) {
                t.join();
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Elapsed time: " + elapsed + "ms");

            if (i >= numOfWarmups) {
                times.add(elapsed);
            }

            Thread.sleep(1000);
            // Cleanup for next run, clear the receipts
            for (int x = 0; x < numOfGroups; x++) {
                creatingAgents.get(0).debugClearGroupMembers(groupName + String.valueOf(x + 1));
            }

            Thread.sleep(2000);
        }

        System.out.println("Times: " + times.toString());
        OptionalDouble optAvg = times.stream().mapToLong(Long::longValue).average();
        if (optAvg.isPresent()) {
            System.out.println("Average: " + optAvg.getAsDouble());
        } else {
            System.out.println("Error while getting average from stream");
        }
    }
}
