package joingroup;

import client.HyperZMQ;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DisbandGroupTest {

    @Test
    public void test() throws InterruptedException {
        HyperZMQ c1 = new HyperZMQ.Builder("client1", "sdfsdfs").build();
        HyperZMQ c2 = new HyperZMQ.Builder("client2", "sdfsdfs").build();
        String group= "testgroup";
        c1.createGroup(group);
        c2.addGroup(group, c1.getKeyForGroup(group));

        Assert.assertTrue(c2.isGroupAvailable(group));

        c1.requestDisbandGroup(group);
        Thread.sleep(4000);

        assertFalse(c2.isGroupAvailable(group));
        assertFalse(c1.isGroupAvailable(group));
    }
}
