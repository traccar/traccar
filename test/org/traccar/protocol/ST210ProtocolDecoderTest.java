package org.traccar.protocol;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.traccar.Server;
import org.traccar.helper.Log;
import org.traccar.server.SocketCliente;

public class ST210ProtocolDecoderTest {

	/*
	@BeforeClass
	public static void UpServer() {
		final Server service = new Server();
		String[] args = new String[1];
		args[0] = "setup\\windows\\windows.cfg";
		try {
			service.init(args);

			Log.info("starting server...");
			service.start();

			// Shutdown server properly
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					Log.info("shutting down server...");
					service.stop();
				}
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testClienteMsg() throws Exception {

		SocketCliente cliente = new SocketCliente();
		cliente.SendMSG(
				"localhost",
				5010,
				"SA200STT;317652;042;20120718;15:37:12;16d41;-15.618755;-056.083241;000.024;000.00;8;1;41548;12.17;100000;2;1979");
	}

*/
	
	@Test
	public void testDecode() throws Exception {

		ST210ProtocolDecoder decoder = new ST210ProtocolDecoder(
				new TestDataManager(), 0);

		assertNotNull(decoder
				.decode(null,
						null,
						"SA200STT;317652;042;20120718;15:37:12;16d41;-15.618755;-056.083241;000.024;000.00;8;1;41548;12.17;100000;2;1979"));
		assertNotNull(decoder
				.decode(null,
						null,
						"SA200STT;317652;042;20120721;19:04:30;16d41;-15.618743;-056.083221;000.001;000.00;12;1;41557;12.21;000000;1;3125"));
		assertNotNull(decoder
				.decode(null,
						null,
						"SA200STT;317652;042;20120722;00:24:23;16d41;-15.618767;-056.083214;000.011;000.00;11;1;41557;12.21;000000;1;3205"));
		assertNotNull(decoder
				.decode(null,
						null,
						"SA200STT;315198;042;20120808;20:37:34;42948;-15.618731;-056.083216;000.007;000.00;12;1;48;0.00;000000;1;0127"));
		assertNotNull(decoder
				.decode(null,
						null,
						"SA200STT;315198;042;20120809;13:43:34;16d41;-15.618709;-056.083223;000.025;000.00;8;1;49;12.10;100000;2;0231"));
		assertNotNull(decoder
				.decode(null,
						null,
						"SA200EMG;317652;042;20120718;15:35:41;16d41;-15.618740;-056.083252;000.034;000.00;8;1;41548;12.17;110000;1"));
	}

}
