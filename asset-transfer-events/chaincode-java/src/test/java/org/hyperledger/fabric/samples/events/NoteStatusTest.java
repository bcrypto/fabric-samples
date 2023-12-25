// ./gradlew clean test --info

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hyperledger.fabric.samples.events.NoteStatus;

public final class NoteStatusTest {

    @Test
    public void test1() {
        NoteStatus dn = new NoteStatus("01", "A", "B", "empty");
        assertEquals(dn.getID(), "01");
        assertEquals(dn.getShipper(), "A");
        assertEquals(dn.getReciever(), "B");
        assertEquals(dn.getStatus(), "empty");
    }

    @Test
    public void test2() {
        NoteStatus dn = new NoteStatus("01", "A", "B", "empty");
        byte[] str = dn.serialize();
        System.out.println(dn.serialize("{}"));
        NoteStatus nt = NoteStatus.deserialize(str);
        assertNotNull(nt);
        assertEquals(dn, nt);
    }
}
