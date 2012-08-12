package org.traccar.server;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class SocketCliente {

	/**
	 * @param args
	 */
	public void SendMSG(String url, int port, String msg) {

		Socket s = null;

		PrintStream ps = null;

		try {

			s = new Socket(url, port);

			ps = new PrintStream(s.getOutputStream());

			ps.println(msg);

			System.out.println("send");

		} catch (IOException e) {

			System.out
					.println("Some problem happened on send data to socket.");

		} finally {

			try {
				s.close();
			} catch (IOException e) {
			}

		}

	}

}
