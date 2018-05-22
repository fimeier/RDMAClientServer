package eth.fimeier.assignment8.proxy;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.cli.ParseException;

import com.ibm.disni.CmdLineCommon;
import com.ibm.disni.rdma.RdmaActiveEndpoint;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCPostSend;

public class RdmaProxyEndpoint implements RdmaEndpointFactory<RdmaProxyEndpoint.CustomClientEndpoint> {

	private RdmaActiveEndpointGroup<RdmaProxyEndpoint.CustomClientEndpoint> endpointGroup;
	private String ipAddress;
	private int port;

	private static int BUFFERSIZE_HTML = 300;//208;
	//private static int BUFFERSIZE_PNG = 2*2437;
	public static int html_size = 0;
	public static int png_size = 0;

	public static  Boolean getHTML = true; //if false => getPNG
	public byte[] result = null;

	static String absolutePath = new File("").getAbsolutePath() ;
	static String pathStaticPages = absolutePath + "/src/eth/fimeier/assignment8/proxy/temp/";

	public void write_png(byte[] png) {
		String path = pathStaticPages+ "network.png";
		try {
			Files.write(Paths.get(path), png);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public RdmaProxyEndpoint.CustomClientEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
		return new RdmaProxyEndpoint.CustomClientEndpoint(endpointGroup, idPriv, serverSide);
	}

	public void run() throws Exception {
		//endpointGroup = new RdmaActiveEndpointGroup<RdmaProxyEndpoint.CustomClientEndpoint>(1000, false, 128, 4, 128);
		endpointGroup = new RdmaActiveEndpointGroup<RdmaProxyEndpoint.CustomClientEndpoint>(10, false, 128, 4, 128);
		endpointGroup.init(this);
		RdmaProxyEndpoint.CustomClientEndpoint endpoint = endpointGroup.createEndpoint();

		//connect to the server
		endpoint.connect(URI.create("rdma://" + ipAddress + ":" + port));
		//die init methode wird wärend des obigen calls irgendwann auch aufgerufen...
		InetSocketAddress _addr = (InetSocketAddress) endpoint.getDstAddr();
		System.out.println("RdmaProxyEndpoint::client connected, address " + _addr.toString());

		//hier oben definiert anstatt in getHTML
		IbvSendWR sendWR = null;
		IbvSge sgeSend = null;
		ByteBuffer sendBuf = endpoint.getSendBuf();
		IbvWC workCompEv = null;
		ByteBuffer recvBuf = null;
		long addr = 0;
		int length = 0;
		int lkey = 0;
		SVCPostSend postSend = null;

		Boolean bigBuf = false;

		if (getHTML) {

			////////html////////////////////////////////////////////////////////////
			System.out.println("ask server for html...");
			sendBuf.clear();
			sendBuf.asCharBuffer().put("getIndex.html");
			sendBuf.clear();
			endpoint.postSend(endpoint.getWrList_send()).execute();

			workCompEv = endpoint.getWcEvents().take();
			//IBV_WC_SEND(0) wird "empfangen"
			System.out.println("1....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());


			workCompEv = endpoint.getWcEvents().take();
			// IBV_WC_RECV(128)
			System.out.println("2....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());

			recvBuf = endpoint.getRecvBuf();
			//the message has been received in this buffer
			//it contains some RDMA information sent by the server
			recvBuf.clear();
			addr = recvBuf.getLong();
			length = recvBuf.getInt();
			lkey = recvBuf.getInt();
			recvBuf.clear();
			
			/*
			if (2+2==4) {
				return;
			}*/

			endpoint.postRecv(endpoint.getWrList_recv()).execute().free(); //correct??? //mefi84

			System.out.println("RdmaProxyEndpoint::receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
			System.out.println("RdmaProxyEndpoint::preparing read operation...");

			//issue a one-sided RDMA read opeation to fetch the content from that buffer
			IbvSendWR rdmaReadWR = endpoint.getSendWR();
			sgeSend = new IbvSge();
			sgeSend.setAddr(endpoint.dataMr.getAddr()); //mefi.... sendet die Daten zurück die empfangen wurden!!!
			sgeSend.setLength(endpoint.dataMr.getLength());
			sgeSend.setLkey(endpoint.dataMr.getLkey());
			endpoint.sgeList = new LinkedList<IbvSge>();
			endpoint.sgeList.add(sgeSend);
			rdmaReadWR.setSg_list(endpoint.sgeList);
			rdmaReadWR.setWr_id(1001);
			rdmaReadWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
			rdmaReadWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
			rdmaReadWR.getRdma().setRemote_addr(addr);
			rdmaReadWR.getRdma().setRkey(lkey);

			//post the operation on the endpoint
			//System.out.println("endpoint.getWrList_send().size()="+endpoint.getWrList_send().size());
			//endpoint.getWrList_send().add(rdmaReadWR);
			LinkedList<IbvSendWR> temp = new LinkedList<>();
			temp.add(rdmaReadWR);
			//postSend = endpoint.postSend(endpoint.getWrList_send());
			postSend = endpoint.postSend(temp);


			postSend.getWrMod(0).getSgeMod(0).setLength(length);
			postSend.execute();
			//wait until the operation has completed


			//sendBuf
			workCompEv = endpoint.getWcEvents().take();
			//IBV_WC_RDMA_READ(2)
			System.out.println("3....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());

			//we should have the content of the remote buffer in our own local buffer now
			ByteBuffer dataBuf = endpoint.getDataBuf();//mefi84 endpoint.getDataBuf();
			//dataBuf.clear();
			//System.out.println("RdmaProxyEndpoint::read memory from server: " + dataBuf.asCharBuffer().toString());
			//dataBuf.clear();

			//store result
			result = new byte[length];
			//char[] resultChar = new char[length];
			try {
				dataBuf.get(result, 0, length);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}


			String tr = new String(result);
			System.out.println("$$$RdmaProxyEndpoint$$$ rdmaResult as string="+tr);

		}

		else {
			////////png/

			System.out.println("ask server for png...");

			sendBuf.clear();
			sendBuf.asCharBuffer().put("getnetwork.png");
			sendBuf.clear();
			//post that operation
			endpoint.postSend(endpoint.getWrList_send()).execute().free();


			workCompEv = endpoint.getWcEvents().take();
			//IBV_WC_SEND(0) wird "empfangen"
			System.out.println("PNG 1....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());


			//in our custom endpoints we make sure CQ events get stored in a queue, we now query that queue for new CQ events.
			//in this case a new CQ event means we have received some data, i.e., a message from the server
			workCompEv = endpoint.getWcEvents().take();
			// IBV_WC_RECV(128)
			System.out.println("PNG 2....request was=opcode="+ workCompEv.getOpcode() +"  "+endpoint.recvBuf.asCharBuffer().toString());

			recvBuf = endpoint.getRecvBuf();
			//the message has been received in this buffer
			//it contains some RDMA information sent by the server
			recvBuf.clear();
			addr = recvBuf.getLong();
			length = recvBuf.getInt();
			lkey = recvBuf.getInt();
			recvBuf.clear();

			endpoint.postRecv(endpoint.getWrList_recv()).execute().free(); //correct??? //mefi84

			System.out.println("RdmaProxyEndpoint::receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
			System.out.println("RdmaProxyEndpoint::preparing read operation...");

			IbvSendWR rdmaReadWR = endpoint.getSendWR();
			//sendWR = endpoint.getSendWR();
			sgeSend = new IbvSge();

			//ByteBuffer dataBigBuf = null;

			//IbvMr dataBigMr;
			bigBuf = false;
			ByteBuffer dataBuf = endpoint.getDataBuf();
			if (length> dataBuf.capacity()) {
				System.out.println("bigger dataBuf needed...");
				//mefi84 read in png size
				endpoint.dataBigBuf = ByteBuffer.allocateDirect(2500);
				endpoint.dataBigMr = endpoint.registerMemory(endpoint.dataBigBuf).execute().free().getMr();
				sgeSend.setAddr(endpoint.dataBigMr.getAddr()); 
				sgeSend.setLength(endpoint.dataBigMr.getLength());
				sgeSend.setLkey(endpoint.dataBigMr.getLkey());
				bigBuf = true;
			} else {
				System.out.println("normal dataBuf enough...");
				sgeSend.setAddr(endpoint.dataMr.getAddr()); 
				sgeSend.setLength(endpoint.dataMr.getLength());
				sgeSend.setLkey(endpoint.dataMr.getLkey());
			}

			endpoint.sgeList = new LinkedList<IbvSge>();
			endpoint.sgeList.add(sgeSend);
			rdmaReadWR.setSg_list(endpoint.sgeList);
			rdmaReadWR.setWr_id(1001);
			rdmaReadWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
			rdmaReadWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
			rdmaReadWR.getRdma().setRemote_addr(addr);
			rdmaReadWR.getRdma().setRkey(lkey);

			LinkedList<IbvSendWR> temp = new LinkedList<>();
			temp.add(rdmaReadWR);
			//postSend = endpoint.postSend(endpoint.getWrList_send());
			postSend = endpoint.postSend(temp);

			postSend.getWrMod(0).getSgeMod(0).setLength(length);
			postSend.execute();
			//wait until the operation has completed


			//sendBuf
			workCompEv = endpoint.getWcEvents().take();
			//IBV_WC_RDMA_READ(2)
			System.out.println("PNG 3....request was=opcode="+ workCompEv.getOpcode()+" png should now be in buffer");

			//we should have the content of the remote buffer in our own local buffer now
			if (bigBuf)
				dataBuf = endpoint.dataBigBuf;
			else
				dataBuf = endpoint.getDataBuf();//mefi84 endpoint.getDataBuf();
			dataBuf.clear();
			//System.out.println("RdmaProxyEndpoint::read memory from server: " + dataBuf.asCharBuffer().toString());
			byte[] pngPicture = new byte[length];
			System.out.println("dataBuf.array().length hasArray="+dataBuf.hasArray());
			for (int i=0; i<length;i++) {
				pngPicture[i] = dataBuf.get(i);
			}
			System.out.println("pngPicture.length="+pngPicture.length);

			System.out.println("write png to "+ pathStaticPages+ "network.png");

			//store result
			result = pngPicture.clone();

			//to temp... not needed...
			//write_png(pngPicture);

		}

		//prepare final message

		sendWR = endpoint.getSendWR();
		sgeSend = new IbvSge();
		sgeSend.setAddr(endpoint.sendMr.getAddr()); //mefi.... sendet die Daten zurück die empfangen wurden!!!
		sgeSend.setLength(endpoint.sendMr.getLength());
		sgeSend.setLkey(endpoint.sendMr.getLkey());
		endpoint.sgeList = new LinkedList<IbvSge>();
		endpoint.sgeList.add(sgeSend);
		sendWR.setSg_list(endpoint.sgeList);
		sendWR.setWr_id(1002);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);		

		sendBuf.clear();
		sendBuf.asCharBuffer().put("final message close connection");
		sendBuf.clear();

		//post that operation
		endpoint.postSend(endpoint.getWrList_send()).execute().free();

		//close everything
		System.out.println("closing endpoint");
		//delete buffers
		endpoint.deregisterMemory(endpoint.dataMr);
		if (bigBuf) {
			endpoint.deregisterMemory(endpoint.dataBigMr);
		}
		endpoint.deregisterMemory(endpoint.sendMr);
		endpoint.deregisterMemory(endpoint.recvMr);

		endpoint.close();
		System.out.println("closing endpoint, done");
		endpointGroup.close();
	}

	public void launch(String[] args) throws Exception {
		CmdLineCommon cmdLine = new CmdLineCommon("RdmaProxyEndpoint");

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

	public static class CustomClientEndpoint extends RdmaActiveEndpoint {
		private ByteBuffer buffers[];
		private IbvMr mrlist[];
		private int buffercount = 3;
		private int buffersize = BUFFERSIZE_HTML; //100;

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

		public CustomClientEndpoint(RdmaActiveEndpointGroup<? extends CustomClientEndpoint> endpointGroup, RdmaCmId idPriv, boolean isServerSide) throws IOException {
			super(endpointGroup, idPriv, isServerSide);
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

			for (int i = 0; i < buffercount; i++){
				mrlist[i] = registerMemory(buffers[i]).execute().free().getMr();
			}

			this.dataBuf = buffers[0];
			this.dataMr = mrlist[0];
			this.sendBuf = buffers[1];
			this.sendMr = mrlist[1];
			this.recvBuf = buffers[2];
			this.recvMr = mrlist[2];

			dataBuf.clear();
			sendBuf.clear();

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

			System.out.println("RdmaProxyEndpoint::initiated recv");
			this.postRecv(wrList_recv).execute().free();
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
	}




}
