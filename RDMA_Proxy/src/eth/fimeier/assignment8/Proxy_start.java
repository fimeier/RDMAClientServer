package eth.fimeier.assignment8;

import eth.fimeier.assignment8.proxy.Proxy;

public class Proxy_start {

	/*
	 * arg: proxyPort rdmaServerIP
	 */
	public static void main(String[] args) throws Exception {


		/*
		 * default Settings
		 */
		int proxyPort = 8000;
		String rdmaServerIP = "10.80.51.30";
		String proxysite = "www.rdmawebpage.com";
		
		if (args.length==2) {
			proxyPort=Integer.parseInt(args[0]);
			rdmaServerIP=args[1];
		} else {
			System.out.println("Start system with default parameters...");
		}


		System.out.println("Start proxy on port="+proxyPort + " RDMAserverIP=" +rdmaServerIP);
		Proxy proxy = new Proxy(proxyPort, rdmaServerIP, proxysite);
		System.out.println("Proxy started...");
	}
}


