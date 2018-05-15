package eth.fimeier.assignment8;

import eth.fimeier.assignment8.server.RdmaHTTPServerEndpoint;

public class start_All implements Runnable {
	public static void main(String[] args) throws Exception {

		HTTP_Server_start.main(args);
		Proxy_start.main(args);
		
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
