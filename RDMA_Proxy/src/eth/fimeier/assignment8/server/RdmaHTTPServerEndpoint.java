package eth.fimeier.assignment8.server;

import com.ibm.disni.CmdLineCommon;
import com.ibm.disni.rdma.RdmaActiveEndpoint;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.RdmaServerEndpoint;
import com.ibm.disni.rdma.verbs.*;

import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

public class RdmaHTTPServerEndpoint implements RdmaEndpointFactory<RdmaHTTPServerEndpoint.CustomServerEndpoint> {
	private RdmaActiveEndpointGroup<RdmaHTTPServerEndpoint.CustomServerEndpoint> endpointGroup;
	private String ipAddress;
	private int port;

	//Todo im Constructor ? setzen ..... zudem Buffersize png siehe was effektiv eingestellt...
	private static int BUFFERSIZE_HTML = 300;//206; //100
	//private static int BUFFERSIZE_PNG = 5000; //2*2437;
	public static int html_size = 0;
	public static int png_size = 0;

	public static Boolean runForeEver = true;


	static String absolutePath = new File("").getAbsolutePath() ;
	static String pathStaticPages = absolutePath + "/src/eth/fimeier/assignment8/server/static_content/";

	public RdmaHTTPServerEndpoint.CustomServerEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
		return new RdmaHTTPServerEndpoint.CustomServerEndpoint(endpointGroup, idPriv, serverSide);
	}

	public static String get_index() {
		String path = pathStaticPages+ "index.html";
		String output = "";
		try {
			output = readFileAsString(path, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	public static String readFileAsString(String path, Charset encoding) 
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static byte[] get_png() {
		String path = pathStaticPages+ "network.png";
		try {
			byte[] output = readFileAsByteArray(path, Charset.defaultCharset());
			return output;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	public static byte[] readFileAsByteArray(String path, Charset encoding) 
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoded;
	}


	public void run() throws Exception {

		//init and connection
		//create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
		//endpointGroup = new RdmaActiveEndpointGroup<CustomServerEndpoint>(1000, false, 128, 4, 128);
		endpointGroup = new RdmaActiveEndpointGroup<CustomServerEndpoint>(10, false, 128, 4, 128);

		endpointGroup.init(this);

		while(runForeEver) {
			//for (int i = 0; i< 10; i++) {

			//create a server endpoint		
			RdmaServerEndpoint<RdmaHTTPServerEndpoint.CustomServerEndpoint> serverEndpoint = endpointGroup.createServerEndpoint();

			//we can call bind on a server endpoint, just like we do with sockets
			URI uri = URI.create("rdma://" + ipAddress + ":" + port);
			serverEndpoint.bind(uri);
			System.out.println("RdmaHTTPServerEndpoint::server bound to address" + uri.toString());


			//we can accept new connections
			System.out.println("RdmaHTTPServerEndpoint::Waiting for new client connection");
			RdmaHTTPServerEndpoint.CustomServerEndpoint endpoint = serverEndpoint.accept();
			System.out.println("RdmaHTTPServerEndpoint::connection accepted ");
			/////////////////////////////////////////////

			Boolean closeConnection = false;
			while(!closeConnection) {

				System.out.println("Server waiting for requests.....");
				IbvWC workCompEv = endpoint.getWcEvents().take();
				//IBV_WC_RECV(128)

				String requestTemp = endpoint.recvBuf.asCharBuffer().toString();
				String request = requestTemp.startsWith("getIndex.html") ? "getIndex.html": requestTemp.startsWith("getnetwork.png") ? "getnetwork.png" : "final message close connection"; 
				System.out.println("switch request="+request);


				switch(request) {

				case "getIndex.html":
				{
					///////////////send html	
					System.out.println("HTML-SERVER 1. ....request was=opcode="+ workCompEv.getOpcode() +"  "+request);

					/*closeConnection = true; //TODO REMOVE
				if (closeConnection)
					break;*/
					//add fresh recvBuf
					endpoint.recvBuf.clear();
					endpoint.postRecv(endpoint.getWrList_recv()).execute().free();

					ByteBuffer dataBuf = endpoint.getDataBuf();
					ByteBuffer sendBuf = endpoint.getSendBuf();
					IbvMr dataMr = endpoint.getDataMr();


					sendBuf.putLong(dataMr.getAddr());
					sendBuf.putInt(html_size); //mefi... her allenfalls Länger der Daten angeben....
					sendBuf.putInt(dataMr.getLkey());
					sendBuf.clear();

					//post the operation to send the message
					System.out.println("RdmaHTTPServerEndpoint::sending message for Lkey()"+dataMr.getLkey());
					endpoint.postSend(endpoint.getWrList_send()).execute(); //mefi84 .free();
					//we have to wait for the CQ event, only then we know the message has been sent out
					workCompEv = endpoint.getWcEvents().take();
					//IBV_WC_SEND(0)
					System.out.println("HTML-SERVER 2. ....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());
					
					//closeConnection = true; //TODO REMOVE


					break;
				}
				case "getnetwork.png":
				{
					///////////////send PNG

					System.out.println("PNG-SERVER 1. ....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());

					//add fresh recvBuf
					endpoint.recvBuf.clear();
					endpoint.postRecv(endpoint.getWrList_recv()).execute().free();
					ByteBuffer sendBuf = endpoint.getSendBuf();
					IbvMr dataBigMr = endpoint.getDataBigMr(); //endpoint.getDataMr();

					sendBuf.putLong(dataBigMr.getAddr());
					sendBuf.putInt(png_size);    //(dataMessage.length())*2); //mefi... her allenfalls Länger der Daten angeben....
					sendBuf.putInt(dataBigMr.getLkey());
					sendBuf.clear();

					//post the operation to send the message
					System.out.println("RdmaHTTPServerEndpoint::sending message to receive NETWORK.PNG Lkey()"+dataBigMr.getLkey());
					endpoint.postSend(endpoint.getWrList_send()).execute(); //mefi84 .free();
					//we have to wait for the CQ event, only then we know the message has been sent out
					workCompEv = endpoint.getWcEvents().take();
					//IBV_WC_SEND(0)
					System.out.println("PNG-SERVER 2. ....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());
					break;
				}
				case "final message close connection":
				{closeConnection = true;
				System.out.println("3. ....request was=opcode="+ workCompEv.getOpcode() +"  "+request);
				System.out.println("RdmaHTTPServerEndpoint::final message was="+request);
				closeConnection = true;
				break;

				}
				}

			}

			//close everything
			endpoint.deregisterMemory(endpoint.getDataMr());
			endpoint.deregisterMemory(endpoint.getDataBigMr());
			endpoint.deregisterMemory(endpoint.getSendMr());
			endpoint.deregisterMemory(endpoint.getRecvMr());

			endpoint.close();

			serverEndpoint.close();
		}
		endpointGroup.close();
	}

	public void launch(String[] args) throws Exception {
		CmdLineCommon cmdLine = new CmdLineCommon("RdmaHTTPServerEndpoint");

		try {
			cmdLine.parse(args);
		} catch (ParseException e) {
			cmdLine.printHelp();
			System.exit(-1);
		}
		ipAddress = cmdLine.getIp();
		port = cmdLine.getPort();

		this.run();
	}

	public static void main(String[] args) throws Exception {
		RdmaHTTPServerEndpoint simpleServer = new RdmaHTTPServerEndpoint();
		simpleServer.launch(args);
	}

	public static class CustomServerEndpoint extends RdmaActiveEndpoint {
		private ByteBuffer buffers[];
		private IbvMr mrlist[];
		private int buffercount = 3;
		private int buffersize = BUFFERSIZE_HTML; //100

		private ByteBuffer dataBuf;
		private IbvMr dataMr;
		private ByteBuffer sendBuf;
		private IbvMr sendMr;
		private ByteBuffer recvBuf;
		private IbvMr recvMr;

		//mefi84
		private ByteBuffer dataBigBuf;
		private IbvMr dataBigMr;

		private LinkedList<IbvSendWR> wrList_send;
		private IbvSge sgeSend;
		private LinkedList<IbvSge> sgeList;
		private IbvSendWR sendWR;

		private LinkedList<IbvRecvWR> wrList_recv;
		private IbvSge sgeRecv;
		private LinkedList<IbvSge> sgeListRecv;
		private IbvRecvWR recvWR;

		private ArrayBlockingQueue<IbvWC> wcEvents;

		public CustomServerEndpoint(RdmaActiveEndpointGroup<CustomServerEndpoint> endpointGroup, RdmaCmId idPriv, boolean serverSide) throws IOException {
			super(endpointGroup, idPriv, serverSide);
			this.buffercount = 3;
			this.buffersize = BUFFERSIZE_HTML; //100;
			buffers = new ByteBuffer[buffercount];
			this.mrlist = new IbvMr[buffercount];

			for (int i = 0; i < buffercount; i++){
				buffers[i] = ByteBuffer.allocateDirect(buffersize);
			}

			this.wrList_send = new LinkedList<IbvSendWR>();
			this.sgeSend = new IbvSge();
			this.sgeList = new LinkedList<IbvSge>();
			this.sendWR = new IbvSendWR();

			this.wrList_recv = new LinkedList<IbvRecvWR>();
			this.sgeRecv = new IbvSge();
			this.sgeListRecv = new LinkedList<IbvSge>();
			this.recvWR = new IbvRecvWR();

			this.wcEvents = new ArrayBlockingQueue<IbvWC>(10);
		}

		//important: we override the init method to prepare some buffers (memory registration, post recv, etc).
		//This guarantees that at least one recv operation will be posted at the moment this endpoint is connected.
		public void init() throws IOException{
			super.init();

			//Read Files

			for (int i = 0; i < buffercount; i++){
				mrlist[i] = registerMemory(buffers[i]).execute().free().getMr();
			}

			this.dataBuf = buffers[0];
			this.dataMr = mrlist[0];
			this.sendBuf = buffers[1];
			this.sendMr = mrlist[1];
			this.recvBuf = buffers[2];
			this.recvMr = mrlist[2];

			//mefi84 read in png size
			this.dataBigBuf = ByteBuffer.allocateDirect(2500);
			this.dataBigMr = registerMemory(this.dataBigBuf).execute().free().getMr();

			/*
			 * load index.html and network.png in buffers
			 */
			//load index.html from file system and store it in datBuf<=>dataMr
			String dataMessage = get_index();
			html_size = dataMessage.length()*2; //TODO siehe put änderung unten 3 Zeilen...
			dataBuf.clear();
			//dataBuf.asCharBuffer().put(dataMessage);
			dataBuf.put(dataMessage.getBytes());
			dataBuf.clear();
			//load network.png from file system and store it in datBigBuf<=>dataBigMr
			byte [] networkPng = get_png();
			png_size = networkPng.length;
			dataBigBuf.clear();
			dataBigBuf.put(networkPng);
			dataBigBuf.clear();

			sgeSend.setAddr(sendMr.getAddr());
			sgeSend.setLength(sendMr.getLength());
			sgeSend.setLkey(sendMr.getLkey());
			sgeList.add(sgeSend);
			sendWR.setWr_id(2000);
			sendWR.setSg_list(sgeList);
			sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
			sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
			wrList_send.add(sendWR);

			sgeRecv.setAddr(recvMr.getAddr());
			sgeRecv.setLength(recvMr.getLength());
			int lkey = recvMr.getLkey();
			sgeRecv.setLkey(lkey);
			sgeListRecv.add(sgeRecv);
			recvWR.setSg_list(sgeListRecv);
			recvWR.setWr_id(2001);
			wrList_recv.add(recvWR);

			this.postRecv(wrList_recv).execute();
		}

		public void dispatchCqEvent(IbvWC wc) throws IOException {
			wcEvents.add(wc);
		}

		public ArrayBlockingQueue<IbvWC> getWcEvents() {
			return wcEvents;
		}

		public LinkedList<IbvSendWR> getWrList_send() {
			return wrList_send;
		}

		public LinkedList<IbvRecvWR> getWrList_recv() {
			return wrList_recv;
		}

		public ByteBuffer getDataBuf() {
			return dataBuf;
		}

		//mefi84
		public ByteBuffer getDataBigBuf() {
			return dataBigBuf;
		}

		public ByteBuffer getSendBuf() {
			return sendBuf;
		}

		public ByteBuffer getRecvBuf() {
			return recvBuf;
		}

		public IbvSendWR getSendWR() {
			return sendWR;
		}

		public IbvRecvWR getRecvWR() {
			return recvWR;
		}

		public IbvMr getDataMr() {
			return dataMr;
		}

		//mefi84
		public IbvMr getDataBigMr() {
			return dataBigMr;
		}

		public IbvMr getSendMr() {
			return sendMr;
		}

		public IbvMr getRecvMr() {
			return recvMr;
		}
	}

}

