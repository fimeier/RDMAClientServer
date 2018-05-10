package eth.fimeier.assignment8;

import eth.fimeier.assignment8.proxy.Proxy;

public class Proxy_start {

	public static void main(String[] args) throws Exception {

			
		/*
		 * Proxy Settings
		 */
		int proxyPort = 8000;
		String proxysite = "www.rdmawebpage.com";
		
		System.out.println("Start proxy on port for "+proxyPort);
		Proxy web_bitcoin = new Proxy(proxyPort, proxysite);
		System.out.println("Proxy started...");
	}
}




