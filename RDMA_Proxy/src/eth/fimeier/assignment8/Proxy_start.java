package eth.fimeier.assignment8;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;

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




