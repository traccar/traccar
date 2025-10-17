import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import net.minidev.json.*;



public class HowenExp extends Thread {
	public static String filaname;
	public static final int PORT = 33000;
	public static final int HOWEN_HEAD_LEN = 8;
	public static final byte HOWEN_FLAG = 0x48;
	public static final byte HOWEV_ERSION = 0x01;

	
	Socket socket;

	public HowenExp(Socket cSocket) {
		super();
		this.socket = cSocket;
		//try{
			File dirPath = new File("test.h264");
			filaname = dirPath.getAbsolutePath();
		//}catch(FileNotFoundException e){
		//	e.printStackTrace();
		//}
		
		
	}
	public static byte[] shortToBytes(short val){
		byte[] bytes = new byte[2];
		bytes[1] = (byte)(0xff & (val >> 8));
		bytes[0] = (byte)(0xff & val);
		return bytes;
	}

	public static byte[] intToBytes_LE(int val){
		byte[] ret = new byte[4];
		ret[0] = (byte)(val & 0xff);
		ret[1] = (byte)((val & 0xff00) >> 8);
		ret[2] = (byte)((val & 0xff0000) >> 16);
		ret[3] = (byte)((val & 0xff000000) >> 24);
		return ret;
	}

	public static byte[] byteMerger(byte[] bt1, byte[] bt2){
		byte[] ret = new byte[bt1.length + bt2.length];
		System.arraycopy(bt1, 0, ret, 0, bt1.length);
		System.arraycopy(bt2, 0, ret, bt1.length, bt2.length);
		return ret;
	}

	public static void sendDataToClient(Socket socket, short msgID, String payloadData){
		try{
			// System.out.println("this is sendDataToClient");
			OutputStream os = socket.getOutputStream();
			byte[] header = new byte[8];
			byte[] headerTmp = new byte[2];
			headerTmp[0] = HOWEN_FLAG;
			headerTmp[1] = HOWEV_ERSION;
			header = byteMerger(byteMerger(headerTmp, shortToBytes(msgID)), intToBytes_LE(payloadData.length()));

			// System.out.println(Arrays.toString(header));

			byte[] ret = new byte[8 + payloadData.length()];
			byte[] endFlag = new byte[2];
			ret = byteMerger(header, payloadData.getBytes());
	
			// System.out.println(Arrays.toString(ret));

			os.write(ret);
			os.flush();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public static String doRecv0x1001(String payload){
		JSONObject obj = (JSONObject)JSONValue.parse(payload);
		String sesieon = (String)obj.get("ss");
		
		JSONObject sendObj = new JSONObject();
		sendObj.put("ss", sesieon);
		sendObj.put("err", 0);
		return sendObj.toString();
	}

	public static void createFileAddWrite(String path, byte[] content, boolean Appendable){
		try{
			FileOutputStream fos = new FileOutputStream(path, Appendable);
			fos.write(content);
			fos.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}

	}

	public static void doRecv0x0011(String payload, int payloadLen){
		try {
			byte[] sb = payload.getBytes("ISO-8859-1");
			short frameType = (short)(((sb[1] << 8) | sb[0] & 0xff));
			short frameChannel = (short)(((sb[3] << 8) | sb[2] & 0xff));
			long t1 = ((sb[11] & 0xff) << 24) | ((sb[10] & 0xff) << 16) | ((sb[9] & 0xff) << 8) | ((sb[8] & 0xff));
			long t2 =  ((sb[7] & 0xff) << 24) | ((sb[6] & 0xff) << 16) | ((sb[5] & 0xff) << 8) | ((sb[4] & 0xff) << 0);
			long frameTimeStamp = t2/1000 + (t1 << 32)/1000;
			System.out.println("frameType=" + frameType + " frameChannel=" + frameChannel + " frameTimeStamp=" + frameTimeStamp);
			if(payloadLen <=12) {
				System.out.println("data over");
				return;
			}
			if (frameType == 0x0001 || frameType == 0x0002) {
				byte[] frameData = Arrays.copyOfRange(sb, 12, sb.length);
				createFileAddWrite(filaname, frameData, true);
				// video frame, can save to .h264
			} else if (frameType == 0x0003) {
				// audio frame
			} else if (frameType == 0x0006) {
				// status frame
			}
		} catch(Exception e){
			e.printStackTrace();			
		}
	}

	public static String doPutToDev0x4040(){
		JSONObject sendObj = new JSONObject();
		sendObj.put("ss", UUID.randomUUID().toString());
		sendObj.put("ct", 15);
		return sendObj.toString();
	}

	public static String doPutToDev0x4050(){
		JSONObject sendObj = new JSONObject();
		sendObj.put("ss", UUID.randomUUID().toString());
		sendObj.put("ct", 45);
		return sendObj.toString();
	}

	public static String doPutToDev0x4060(){
		JSONObject sendObj = new JSONObject();
		sendObj.put("ss", UUID.randomUUID().toString());
		sendObj.put("st", "2020-03-10 00:00:00");
		sendObj.put("et", "2020-03-10 01:26:00");
		sendObj.put("chl", "1;2;3");
		sendObj.put("ft", 1);
		return sendObj.toString();
	}

	public static String doPutToDev0x4070(){
		JSONObject sendObj = new JSONObject();
		sendObj.put("ss", UUID.randomUUID().toString());
		sendObj.put("chl", "1");
		sendObj.put("srv", "172.16.50.8:33000");
		sendObj.put("st", "2020-04-07 14:26:00");
		sendObj.put("et", "2020-04-07 14:30:00");
		sendObj.put("fl", "1;2;3");
		return sendObj.toString();
	}

	public static String doRecv0x1002(String payload){
		JSONObject obj = (JSONObject)JSONValue.parse(payload);
		String sesieon = (String)obj.get("ss");
		
		JSONObject sendObj = new JSONObject();
		sendObj.put("ss", sesieon);
		sendObj.put("err", 0);
		return sendObj.toString();
	}

	public static void doRecv0x1041(String payload, int payloadLen){

	}

	public static void doRecv0x1051(String payload, int payloadLen){

	}

	public static void doRecv0x1060(String payload){
		System.out.println(payload);
	}

	public static void doRecv0x1070(String payload){
		System.out.println(payload);
	}

	public void run() {
		try{
			InputStream inputStream = socket.getInputStream();
			byte[] bytes = new byte[2048];
			int len;
			StringBuffer sb = new StringBuffer();
			while((len = inputStream.read(bytes)) != -1){
				sb.append(new String(bytes, 0, len, "ISO-8859-1"));
				for(;;) {
					len = sb.length();
					if(len < HOWEN_HEAD_LEN){
						// System.out.println("data len is small");
						break;
					}
					byte[] hby = sb.substring(0, 8).getBytes("ISO-8859-1");
					short msgID = (short)(((hby[3] << 8) | hby[2] & 0xff));
					int msgLen = (int)((((hby[7] & 0xff) << 24) | ((hby[6] & 0xff) << 16) | ((hby[5] & 0xff) << 8) | ((hby[4] & 0xff) << 0)));
					if(len < (msgLen + HOWEN_HEAD_LEN)){
						// System.out.println("data len was error");
						break;
					}
					System.out.printf("recvID 0x%04x byteLen %d\n", msgID, msgLen);
					String payload = sb.substring(HOWEN_HEAD_LEN, HOWEN_HEAD_LEN + msgLen);
					switch(msgID){
						case 0x0001:
							sendDataToClient(socket, (short)0x0001, "");
							// for 4060 test
							// sendDataToClient(socket, (short)0x4060, doPutToDev0x4060());
							// for 4070 test
							sendDataToClient(socket, (short)0x4070, doPutToDev0x4070());
							break;
						case 0x0011:
							doRecv0x0011(payload, msgLen);
							break;
						case 0x1001:
							sendDataToClient(socket, (short)0x4001, doRecv0x1001(payload));
							sendDataToClient(socket, (short)0x4040, doPutToDev0x4040());
							sendDataToClient(socket, (short)0x4050, doPutToDev0x4050());
							break;
						case 0x1002:
							sendDataToClient(socket, (short)0x4002, doRecv0x1002(payload));
							break;
						case 0x1040:
							System.out.println(payload);
							break;
						case 0x1050:
							System.out.println(payload);
							break;
						case 0x1041:
							doRecv0x1041(payload, msgLen);
							sendDataToClient(socket, (short)0x4041, "");
							break;
						case 0x1051:
							doRecv0x1051(payload, msgLen);
							break;
						case 0x1060:
							doRecv0x1060(payload);
							break;
						case 0x1070:
							doRecv0x1070(payload);
							break;
					}
					sb = sb.delete(0, HOWEN_HEAD_LEN + msgLen);
				}
			}	
		}catch(Exception e){
			e.printStackTrace();			
		}
	}
 
	public static void main(String[] args) throws Exception{
		ServerSocket server = new ServerSocket(PORT);
		System.out.println("server is readly");
		while(true) {
			Socket socket = server.accept();	
			new HowenExp(socket).start();
		}
	}
}
