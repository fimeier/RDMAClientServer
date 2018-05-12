package eth.fimeier.assignment8.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
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

public class RdmaProxyEndpoint2 implements RdmaEndpointFactory<RdmaProxyEndpoint2.CustomClientEndpoint> {
	
	private RdmaActiveEndpointGroup<RdmaProxyEndpoint2.CustomClientEndpoint> endpointGroup;
	private String ipAddress;
	private int port;
	
	private static int BUFFERSIZE_HTML = 300;//208;
	private static int BUFFERSIZE_PNG = 2*2437;

	public RdmaProxyEndpoint2.CustomClientEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
		return new RdmaProxyEndpoint2.CustomClientEndpoint(endpointGroup, idPriv, serverSide);
	}
	
	public void run() throws Exception {
		//create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
		endpointGroup = new RdmaActiveEndpointGroup<RdmaProxyEndpoint2.CustomClientEndpoint>(1000, false, 128, 4, 128);
		endpointGroup.init(this);
		//we have passed our own endpoint factory to the group, therefore new endpoints will be of type CustomClientEndpoint
		//let's create a new client endpoint
		RdmaProxyEndpoint2.CustomClientEndpoint endpoint = endpointGroup.createEndpoint();

		//connect to the server
		endpoint.connect(URI.create("rdma://" + ipAddress + ":" + port));
		//die init methode wird wärend des obigen calls irgendwann auch aufgerufen...
		InetSocketAddress _addr = (InetSocketAddress) endpoint.getDstAddr();
		System.out.println("RdmaProxyEndpoint::client connected, address " + _addr.toString());

		//in our custom endpoints we make sure CQ events get stored in a queue, we now query that queue for new CQ events.
		//in this case a new CQ event means we have received some data, i.e., a message from the server
		endpoint.getWcEvents().take();
		ByteBuffer recvBuf = endpoint.getRecvBuf();
		//the message has been received in this buffer
		//it contains some RDMA information sent by the server
		recvBuf.clear();
		long addr = recvBuf.getLong();
		int length = recvBuf.getInt();
		int lkey = recvBuf.getInt();
		recvBuf.clear();
		System.out.println("RdmaProxyEndpoint::receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
		System.out.println("RdmaProxyEndpoint::preparing read operation...");

		//the RDMA information above identifies a RDMA buffer at the server side
		//let's issue a one-sided RDMA read opeation to fetch the content from that buffer
		IbvSendWR sendWR = endpoint.getSendWR();
		sendWR.setWr_id(1001);
		sendWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);

		//post the operation on the endpoint
		SVCPostSend postSend = endpoint.postSend(endpoint.getWrList_send());
		
		postSend.getWrMod(0).getSgeMod(0).setLength(length);
		postSend.execute();
		//wait until the operation has completed
		endpoint.getWcEvents().take();
		//we should have the content of the remote buffer in our own local buffer now
		ByteBuffer dataBuf = endpoint.getDataBuf();
		dataBuf.clear();
		System.out.println("RdmaProxyEndpoint::read memory from server: " + dataBuf.asCharBuffer().toString());
		
		/*for (int i = 10; i <= 100; ){
			postSend.getWrMod(0).getSgeMod(0).setLength(i);
			postSend.execute();
			//wait until the operation has completed
			endpoint.getWcEvents().take();

			//we should have the content of the remote buffer in our own local buffer now
			ByteBuffer dataBuf = endpoint.getDataBuf();
			dataBuf.clear();
			System.out.println("RdmaProxyEndpoint::read memory from server: " + dataBuf.asCharBuffer().toString());
			i += 10;
		}*/

		//dataBuf == dataMr enthält nun die Daten
		//let's prepare a final message to signal everything went fine
		//versuche Daten zu löschen....
		//dataBuf.asCharBuffer().put("a final message to signal everything went fine jojo");
		sendWR.setWr_id(1002);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);

		//post that operation
		endpoint.postSend(endpoint.getWrList_send()).execute().free();

		//close everything
		System.out.println("closing endpoint");
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
			this.recvBuf = buffers[2];
			this.recvMr = mrlist[2];

			dataBuf.clear();
			sendBuf.clear();

			sgeSend.setAddr(dataMr.getAddr()); //mefi.... sendet die Daten zurück die empfangen wurden!!!
			sgeSend.setLength(dataMr.getLength());
			sgeSend.setLkey(dataMr.getLkey());
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
