package eth.fimeier.assignment8.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import com.ibm.disni.examples.ReadClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;



public class Proxy2 {

	private String PROXYSITE = "null";

	private HttpServer server;
	
	private String rdmaServerIp = "192.168.170.30";
    //private int rdmaServerPort = 1919;

	//Todo Threads... damit Parallele Anfragen auch im Proxy
	
	public Proxy2(int port, String PROXYSITE) throws IOException {

		this.PROXYSITE = PROXYSITE;

		server = HttpServer.create(new InetSocketAddress(port), 0);
		//Todo
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			
			/*
			System.out.println("callRdmaProxyEndpoint()....");
			String result = callRdmaProxyEndpoint();
			System.out.println("result was=" +result);
			System.out.println("\nuser_input : "+t.getRequestURI().getQuery() +
					" t.getRequestURI()="+ t.getLocalAddress() + " " +
					t.getRemoteAddress() + " " +
					"asdsad"
					);
			Set<String> asd = t.getRequestHeaders().keySet();
			System.out.println(asd.toString());
			System.out.println(t.getRequestHeaders().get("Host"));
			String requested_url = t.getRequestHeaders().get("Host").get(0);
			System.out.println("requested_url="+requested_url);
			
			if (requested_url.equals("www.timeout.com")) {
				System.out.println("Error..... :-) Wrong site.... "+requested_url);
				System.out.println("Sleeeeeeeeeeeeeeeeeeeeeeeeeeping"+requested_url);
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Wake UP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+requested_url);
				t.sendResponseHeaders(504, 0);
				OutputStream os = t.getResponseBody();
				//os.write(resp);
				os.close();
				
			}
			
			if (! requested_url.equals(PROXYSITE)) {
				System.out.println("Error..... :-) Wrong site.... "+requested_url);
				
				t.sendResponseHeaders(404, 0);
				OutputStream os = t.getResponseBody();
				//os.write(resp);
				os.close();
				
			}
			
			*/


			String user_input = "null";
			if (t.getRequestURI().getQuery() != null)
				user_input = java.net.URLDecoder.decode(t.getRequestURI().getQuery(), "UTF-8");
			System.out.println("user_input after decoding: "+user_input);

			//System.out.println("call backend...");
			String response = "";
			try {
				response = backend(user_input);

			}
			//Todo 404 HTTP Response HTTP 504 (Gateway Time-out)
			catch(Exception e) {
				e.printStackTrace();
				response = backend("func=MAIN");
			}

			//System.out.println("returned from backend...\n");
			byte[] resp = response.getBytes();
			t.sendResponseHeaders(200, resp.length);
			OutputStream os = t.getResponseBody();
			os.write(resp);
			os.close();
		}

	}

	private String backend(String user_input) {
		return "Blubs";
	}
	
	private String callRdmaProxyEndpoint() {
		String result = "null";
		String[] args = {"-a", "192.168.170.30"};
		RdmaProxyEndpoint simpleClient = new RdmaProxyEndpoint();
		try {
			simpleClient.launch(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return result;
	}

}
