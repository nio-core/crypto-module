package joingroup;

import client.HyperZMQ;
import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import txprocessor.CombinedTP;
import voting.*;

import java.util.*;

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
        int numOfGroups = 10;
        int numOfAgentsInGroup = 5;
        int numOfAgents = numOfAgentsInGroup * numOfGroups;
        String groupName = "testgroup";
        int numOfRuns = 1;
        int numOfWarmups = 0;
        int threadDelayMS = 1500;


        CombinedTP.main(null);

        Thread.sleep(1500);

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

            tmp.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(tmp));
            tmp.getVoteManager().setVoteSender(new SawtoothVoteSender());

            //tmp.getVoteManager().setVotingProcessGroup(new ZMQVotingProcess(tmp, 60001 + i));
            //tmp.getVoteManager().setVoteSender(new ZMQVoteSender());

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
            //Thread.sleep(200);
            // Add to total list for reference if needed
            agents.add(tmp);
        }

        assertEquals(numOfAgents, agents.size());
        groupsList.forEach(g -> assertEquals(numOfAgentsInGroup, g.size()));

        // Clear the receipts
        for (int i = 0; i < numOfGroups; i++) {
            agents.get(0).debugClearGroupMembers(groupName + String.valueOf(i + 1));
        }

        Thread.sleep(2000);

        //+----------------------------------------------------------------------------------------
        //                      ACTUAL TEST RUNS
        //-----------------------------------------------------------------------------------------
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < numOfRuns + numOfWarmups; i++) {
            // Prepare the threads
            int basePort = 61001; // + (i * 100);
            //List<GroupBuildingRunnable> gbrs = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            for (int y = 0; y < groupsList.size(); y++) {
                GroupBuildingRunnable gbr = new GroupBuildingRunnable(
                        groupsList.get(y),
                        groupName + String.valueOf(y + 1),
                        basePort + (y * 50));
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
        String s = "41885, 43272, 45790, 45364, 52634, 41799, 50060, 36577, 36723, 44744,\n" +
                "40578, 38657, 38039, 33039, 41007, 44528, 35723, 45035, 45710, 43861,\n" +
                "45537, 43080, 37830, 38630, 47265, 50103, 42924, 45961, 36191, 52414,\n" +
                "51430, 51604, 51327, 38256, 50812, 47339, 44987, 47263, 42097, 49758, \n" +
                "45944, 36738, 51071, 38759, 49196, 44625, 42850, 36169, 42891, 51223";
        s = s.strip();
        List<String> ls = Arrays.asList(s.split(","));
        OptionalDouble avg = ls.stream().mapToDouble(Double::valueOf).average();
        if (avg.isPresent()) {
            System.out.println("Avg: " + avg.getAsDouble());
        }
    }

    @Test
    public void gsdg() {
        List<Integer> list = new ArrayList<>();
        int count = 30;
        Random r = new Random();
        int low = 35723;
        int high = 52634;
        for (int i = 0; i < count; i++) {
            int result = r.nextInt(high - low) + low;
            list.add(result);
        }
        System.out.println(list.toString());
    }
}
