package org.traccar.location;

import org.junit.Assert;
import org.junit.Test;

public class CellInfoTest {

    @Test
    public void testToString() {

        CellInfo info = new CellInfo();
        info.addCell(0, 0, 1000, 2000);
        info.addCell(400, 1, 3000, 4000);

        Assert.assertEquals("[{\"lac\":1000,\"cid\":2000},{\"mcc\":400,\"mnc\":1,\"lac\":3000,\"cid\":4000}]", info.toString());

    }

    @Test
    public void testFromString() {

        CellInfo info = CellInfo.fromString("[{\"lac\":1000,\"cid\":2000}]");

        Assert.assertEquals(1, info.getCells().size());

        CellInfo.Cell cell = info.getCells().get(0);

        Assert.assertEquals(0, cell.getMcc());
        Assert.assertEquals(0, cell.getMnc());
        Assert.assertEquals(1000, cell.getLac());
        Assert.assertEquals(2000, cell.getCid());
        Assert.assertEquals(0, cell.getSignal());

    }

}
