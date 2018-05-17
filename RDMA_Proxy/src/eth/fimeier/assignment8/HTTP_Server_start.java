package eth.fimeier.assignment8;

import eth.fimeier.assignment8.server.HTTP_server;
import eth.fimeier.assignment8.server.RdmaHTTPServerEndpoint;

public class HTTP_Server_start {

	public static void main(String[] args) throws Exception {

		
		/*
		String result = "null";
		String[] args2 = {"-a", "10.80.51.30"};
		RdmaHTTPServerEndpoint rdmaServer = new RdmaHTTPServerEndpoint();
		try {
			rdmaServer.launch(args2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		/*
		 * Proxy Settings
		 */
		int proxyPort = 8080;
		String proxysite = "www.rdmawebpage.com";
		System.out.println("Start HTTP_RDMA_Server on port for "+proxyPort);
		HTTP_server web = new HTTP_server(proxyPort, proxysite);
		System.out.println("Proxy started...");
	}
}




