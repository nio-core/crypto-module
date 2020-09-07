package joingroup;

import client.HyperZMQ;
import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import txprocessor.CombinedTP;
import voting.GroupVotingProcess;
import voting.SimpleMajorityEvaluator;
import voting.YesVoteStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.Assert.assertEquals;

public class EvalScenario2 {

    /**
     * The goal of this test is to measure the time it takes to create X groups with Y participants each and then disband the groups
     */
    @Test
    public void test() throws InterruptedException {
        //+----------------------------------------------------------------------------------------
        //                      PARAMETERS
        //-----------------------------------------------------------------------------------------
        int numOfGroups = 2;
        int numOfAgentsInGroup = 25;
        int numOfAgents = numOfAgentsInGroup * numOfGroups;
        String groupName = "testgroup";
        int numOfRuns = 1;
        int numOfWarmups = 0;

        CombinedTP.main(null);
        CombinedTP.main(null);
        CombinedTP.main(null);
        Thread.sleep(1000);

        //+----------------------------------------------------------------------------------------
        //                      TEST SETUP
        //-----------------------------------------------------------------------------------------

        List<HyperZMQ> agents = new ArrayList<>();
        List<List<HyperZMQ>> groupsList = new ArrayList<>();
        groupsList.add(new ArrayList<>());

        // Create agents, setup voting behavior,
        for (int i = 0; i < numOfAgents; i++) {
            HyperZMQ tmp = new HyperZMQ.Builder("agent" + String.valueOf(i + 1), "whatever").build();
            tmp.isVolatile = true; // Don't save any data
            tmp.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(tmp.getClientID()));
            tmp.getVoteManager().setVotingProcessGroup(new GroupVotingProcess(tmp));
            tmp.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(0));

            tmp.getVoteManager().setVotingParticipantsThreshold(5);
            tmp.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector());

            // Prepare the groups that should be formed
            if (groupsList.get(groupsList.size() - 1).size() >= numOfAgentsInGroup) {
                groupsList.add(new ArrayList<>() {{
                    add(tmp);
                }});
            } else {
                groupsList.get(groupsList.size() - 1).add(tmp);
            }

            // Add to total list for reference if needed
            agents.add(tmp);
        }

        assertEquals(numOfAgents, agents.size());
        groupsList.forEach(g -> assertEquals(numOfAgentsInGroup, g.size()));

        // Clear the receipts
        for (int i = 0; i < numOfGroups; i++) {
            agents.get(0).debugClearGroupMembers(groupName + String.valueOf(i + 1));
        }

        Thread.sleep(1000);

        //+----------------------------------------------------------------------------------------
        //                      ACTUAL TEST RUNS
        //-----------------------------------------------------------------------------------------
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < numOfRuns + numOfWarmups; i++) {
            // Prepare the threads
            int basePort = 60001; // + (i * 100);
            //List<GroupBuildingRunnable> gbrs = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            for (int y = 0; y < groupsList.size(); y++) {
                GroupBuildingRunnable gbr = new GroupBuildingRunnable(
                        groupsList.get(y),
                        groupName + String.valueOf(y + 1),
                        basePort + (y*50));
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
                Thread.sleep(500);
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
                agents.get(0).debugClearGroupMembers(groupName + String.valueOf(x + 1));
            }

            // If the group is not disbanded in the GroupBuildingRunnable, remove the group there so it is free for the next run
            agents.forEach(HyperZMQ::debugClearGroups);

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

    @Test
    public void calculateAverage() {
        String s = "";
        s = s.strip();
        List<String> ls = Arrays.asList(s.split(","));
        OptionalDouble avg = ls.stream().mapToDouble(Double::valueOf).average();
        if (avg.isPresent()) {
            System.out.println("Avg: " + avg.getAsDouble());
        }
    }
}
