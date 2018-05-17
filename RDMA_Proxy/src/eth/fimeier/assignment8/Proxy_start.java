package eth.fimeier.assignment8;

import eth.fimeier.assignment8.proxy.Proxy;
import eth.fimeier.assignment8.proxy.RdmaProxyEndpoint;

public class Proxy_start {

	public static void main(String[] args) throws Exception {

		/*
		String result = "null";
		String[] args2 = {"-a", "10.80.51.30"};
		RdmaProxyEndpoint simpleClient = new RdmaProxyEndpoint();
		try {
			simpleClient.launch(args2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
			
		/*
		 * Proxy Settings
		 */
		int proxyPort = 8000;
		String proxysite = "www.rdmawebpage.com";
		
		System.out.println("Start proxy on port for "+proxyPort);
		Proxy proxy = new Proxy(proxyPort, proxysite);
		System.out.println("Proxy started...");
	}
}


