package eth.fimeier.assignment8;

import eth.fimeier.assignment8.server.RdmaHTTPServerEndpoint;

public class RDMA_Server_start {

	public static void main(String[] args) throws Exception {


		/*
		 * default Settings
		 */
		String rdmaServerIP = "10.80.51.30";

		if (args.length==1) {
			rdmaServerIP=args[0];
		} else {
			System.out.println("Start system with default parameters...");
		}

		String[] args2 = {"-a", rdmaServerIP};
		RdmaHTTPServerEndpoint rdmaServer = new RdmaHTTPServerEndpoint();
		try {
			rdmaServer.launch(args2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}




