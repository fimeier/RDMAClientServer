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
	
	//mefi84 change path.. auto buff size??
	private static int BUFFERSIZE_HTML = 300;//208; //100
	private static int BUFFERSIZE_PNG = 2*2437;

	static String absolutePath = new File("").getAbsolutePath() ;
	static String pathStaticPages = absolutePath + "/src/eth/fimeier/assignment8/server/static_content/";

	public RdmaHTTPServerEndpoint.CustomServerEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
		return new RdmaHTTPServerEndpoint.CustomServerEndpoint(endpointGroup, idPriv, serverSide);
	}
	
	public String get_index() {
		String path = pathStaticPages+ "index.html";
		String output = "";
		try {
			output = readFileAsString(path, Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}
	
	String readFileAsString(String path, Charset encoding) 
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		System.out.println("byte[] encoded.length="+encoded.length);
		String temp = new String(encoded, encoding);
		Files.write(Paths.get(path+"out"), encoded);
		byte[] decoded = temp.getBytes(encoding);
		System.out.println("byte[] decoded.length="+decoded.length);

		return new String(encoded, encoding);
	}
	
	public byte[] get_png() {
		String path = pathStaticPages+ "network.png";
		try {
			byte[] output = readFileAsByteArray(path, Charset.defaultCharset());
			return output;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
	
	byte[] readFileAsByteArray(String path, Charset encoding) 
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));

		return encoded;
	}
	
	

	public void run() throws Exception {
		//create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
		endpointGroup = new RdmaActiveEndpointGroup<CustomServerEndpoint>(1000, false, 128, 4, 128);
		endpointGroup.init(this);
		//create a server endpoint
		RdmaServerEndpoint<RdmaHTTPServerEndpoint.CustomServerEndpoint> serverEndpoint = endpointGroup.createServerEndpoint();

		//we can call bind on a server endpoint, just like we do with sockets
		URI uri = URI.create("rdma://" + ipAddress + ":" + port);
		serverEndpoint.bind(uri);
		System.out.println("RdmaHTTPServerEndpoint::server bound to address" + uri.toString());

		//we can accept new connections
		//while (true) {
		RdmaHTTPServerEndpoint.CustomServerEndpoint endpoint = serverEndpoint.accept();
		System.out.println("RdmaHTTPServerEndpoint::connection accepted ");

		//let's prepare a message to be sent to the client
		//in the message we include the RDMA information of a local buffer which we allow the client to read using a one-sided RDMA operation
		ByteBuffer dataBuf = endpoint.getDataBuf();
		ByteBuffer sendBuf = endpoint.getSendBuf();
		IbvMr dataMr = endpoint.getDataMr();
		
		//read index.html from filesystem
		String dataMessage = get_index(); //"<html><body><h1>Success!</h1><br/><img src=\"network.png\" alt=\"RDMA Read Image Missing!\"/></body></html>";
		//beachte String +1 Problem....
		System.out.println("dataBuf.capacity()="+dataBuf.capacity() + " and dataMessage.length()=" + dataMessage.length());
		

		dataBuf.asCharBuffer().put(dataMessage);
		//dataBuf.asCharBuffer().put("This is a RDMA/read on stag " + dataMr.getLkey() + " !");
		
		
		dataBuf.clear();
		sendBuf.putLong(dataMr.getAddr());
		sendBuf.putInt(dataMr.getLength());
		sendBuf.putInt(dataMr.getLkey());
		sendBuf.clear();

		//post the operation to send the message
		System.out.println("RdmaHTTPServerEndpoint::sending message for Lkey()"+dataMr.getLkey());
		endpoint.postSend(endpoint.getWrList_send()).execute(); //mefi84 .free();
		//we have to wait for the CQ event, only then we know the message has been sent out
		IbvWC workCompEvent = endpoint.getWcEvents().take();
		
		//read Image
		byte [] networkPng = get_png();
		System.out.println("networkPng.length()="+networkPng.length);
				
		//let's wait for the final message to be received. We don't need to check the message itself, just the CQ event is enough.
		workCompEvent = endpoint.getWcEvents().take();
		
		System.out.println("RdmaHTTPServerEndpoint::final message was="+endpoint.recvBuf.asCharBuffer().toString());

		//mefi84
		
		//close everything
		endpoint.close();
		serverEndpoint.close();
		endpointGroup.close();
		
		//}
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

		public IbvMr getSendMr() {
			return sendMr;
		}

		public IbvMr getRecvMr() {
			return recvMr;
		}
	}

}

