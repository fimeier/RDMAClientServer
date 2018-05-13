package eth.fimeier.assignment8.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import com.ibm.disni.examples.ReadClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;



public class HTTP_server2 {

	private String PROXYSITE = "null";

	private HttpServer server;
	
	private String rdmaServerIp = "192.168.170.30";
    //private int rdmaServerPort = 1919;

	//Todo Threads... damit Parallele Anfragen auch im Proxy
	
	public HTTP_server2(int port, String PROXYSITE) throws IOException {

		this.PROXYSITE = PROXYSITE;

		server = HttpServer.create(new InetSocketAddress(port), 0);
		//Todo
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			
			System.out.println("call HTTP_server");

			String user_input = "null";
			if (t.getRequestURI().getQuery() != null)
				user_input = java.net.URLDecoder.decode(t.getRequestURI().getQuery(), "UTF-8");
			System.out.println("user_input after decoding: "+user_input);

			//System.out.println("call backend...");
			String response = "";
			try {
				//response = backend(user_input);
				System.out.println("callRdmaHTTPServerEndpoint()..");
				response = callRdmaHTTPServerEndpoint();

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
	
	private String callRdmaHTTPServerEndpoint() {
		String result = "null";
		String[] args = {"-a", "192.168.170.30"};
		RdmaHTTPServerEndpoint rdmaServer = new RdmaHTTPServerEndpoint();
		try {
			rdmaServer.launch(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return result;
	}

}
