package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class StbProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new StbProtocolDecoder(null);

        verifyPosition(decoder, text(
                "{\"msgType\":310,\"attrList\":[{\"id\":\"01106001\",\"value\":31},{\"id\":\"01101001\",\"value\":1},{\"id\":\"01102001\",\"value\":113.826355},{\"id\":\"01103001\",\"value\":22.846399}],\"devId\":\"BT106001020JPZZ210718001\",\"txnNo\":\"1626940074000\"}"));

        verifyAttributes(decoder, text(
                "{\"attrList\":[{\"doorId\":\"4\",\"id\":\"02103001\",\"value\":\"1\"},{\"doorId\":\"2\",\"id\":\"02103001\",\"value\":\"0\"},{\"id\":\"02120001\",\"value\":\"11.37\"},{\"doorId\":\"6\",\"id\":\"02106001\",\"value\":\"\"},{\"doorId\":\"5\",\"id\":\"02103001\",\"value\":\"0\"},{\"id\":\"02105001\",\"value\":\"-150\"},{\"id\":\"02102001\",\"value\":\"1\"},{\"doorId\":\"5\",\"id\":\"02106001\",\"value\":\"\"},{\"doorId\":\"5\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"1\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"1\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"6\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"7\",\"id\":\"02103001\",\"value\":\"0\"},{\"doorId\":\"3\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"1\",\"id\":\"02106001\",\"value\":\"\"},{\"id\":\"02101001\",\"value\":\"\"},{\"id\":\"02119001\",\"value\":\"1\"},{\"doorId\":\"6\",\"id\":\"02103001\",\"value\":\"0\"},{\"doorId\":\"8\",\"id\":\"02103001\",\"value\":\"0\"},{\"doorId\":\"3\",\"id\":\"02103001\",\"value\":\"0\"},{\"doorId\":\"2\",\"id\":\"02106001\",\"value\":\"\"},{\"id\":\"02108001\",\"value\":\"0.922\"},{\"doorId\":\"2\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"7\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"4\",\"id\":\"02106001\",\"value\":\"\"},{\"doorId\":\"3\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"8\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"1\",\"id\":\"02103001\",\"value\":\"0\"},{\"doorId\":\"2\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"7\",\"id\":\"02106001\",\"value\":\"\"},{\"doorId\":\"8\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"3\",\"id\":\"02106001\",\"value\":\"\"},{\"doorId\":\"4\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"8\",\"id\":\"02106001\",\"value\":\"\"},{\"id\":\"02112001\",\"value\":\"0.0\"},{\"doorId\":\"4\",\"id\":\"02104001\",\"value\":\"0\"},{\"id\":\"02111001\",\"value\":\"0.0\"},{\"id\":\"02113001\",\"value\":\"27\"},{\"doorId\":\"5\",\"id\":\"02118001\",\"value\":\"1\"},{\"doorId\":\"7\",\"id\":\"02104001\",\"value\":\"0\"},{\"doorId\":\"6\",\"id\":\"02118001\",\"value\":\"1\"},{\"id\":\"02107001\",\"value\":\"229.7\"}],\"devId\":\"CHZD08KPD0210425046\",\"isFull\":0,\"msgType\":310,\"txnNo\":\"1626153841985\"}"));

        verifyNull(decoder, text(
                "{\"devId\":\"CHZD08KPD0210425046\",\"devType\":2,\"hardVersion\":\"HDTTVA19\",\"msgType\":110,\"protocolVersion\":\"V1\",\"softVersion\":\"3.1.8\",\"switchCabStatus\":\"1\",\"txnNo\":\"1625212741537\"}"));

    }

}
