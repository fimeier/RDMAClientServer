package eth.fimeier.assignment8;

import eth.fimeier.assignment8.server.HTTP_server;
import eth.fimeier.assignment8.server.RdmaHTTPServerEndpoint;

public class RDMA_Server_start {

	public static void main(String[] args) throws Exception {

		
		
		String result = "null";
		String[] args2 = {"-a", "10.80.51.30"};
		RdmaHTTPServerEndpoint rdmaServer = new RdmaHTTPServerEndpoint();
		try {
			rdmaServer.launch(args2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
}




