package eth.fimeier.assignment8.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Set;

import com.ibm.disni.examples.ReadClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;



public class HTTP_server {

	private String PROXYSITE = "null";

	private HttpServer server;



	public HTTP_server(int port, String PROXYSITE) throws IOException {

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
			 * The typical life-cycle of a HttpExchange is shown in the sequence below.
			 */

			//Get the request method
			String reqMethod = t.getRequestMethod();
			
			String Uri = t.getRequestHeaders().get("Uri").get(0);
			

		
			/* Todo
			 * Abort if !reqURI.equals("www.rdmawebpage.com/")
			 */
			if (! (Uri.equals("www.rdmawebpage.com/") || Uri.equals("www.rdmawebpage.com/network.png")) || !reqMethod.equals("GET")){

				byte[] resp = new byte[0];
				t.sendResponseHeaders(404, resp.length);
				OutputStream os = t.getResponseBody();
				os.write(resp);
				os.close();
				System.out.println("Server: Send 404 HTTP Response.... reqMethod / reqURI = " + reqMethod + " / " + Uri);
				return;
			}

			System.out.println("Server: Prepare response for proxy.... reqMethod / reqURI = " + reqMethod + " / " + Uri);
			
			/*
			 * todo
			 */
			
			System.out.println("Return response for proxy.... reqMethod / reqURI = " + reqMethod + " / " + Uri);
			//String response = "test response from server.... " + getMetaDataIndexHtml();
			String response = Uri.equals("www.rdmawebpage.com/") ? getMetaDataIndexHtml():getMetaDataNetworkPNG();
			byte[] resp = response.getBytes();
			t.sendResponseHeaders(200, resp.length);
			OutputStream os = t.getResponseBody();
			os.write(resp);
			os.close();
			System.out.println("\n");
		}

	}
	
	private String getMetaDataIndexHtml() {
		long addr = 666666;
		int length = 22;
		int lkey = 123456;

		String json = "{\"result\": {\"Addr\": \""+addr+"\",\"Length\": \""+length+"\",\"Lkey\": \""+lkey+"\"}}";
		return json;
	}
	
	private String getMetaDataNetworkPNG() {
		long addr = 6666667;
		int length = 2234;
		int lkey = 1234563;

		String json = "{\"result\": {\"Addr\": \""+addr+"\",\"Length\": \""+length+"\",\"Lkey\": \""+lkey+"\"}}";
		return json;
	}

	private String backend(String user_input) {
		return "Blubs";
	}


	private String callRdmaHTTPServerEndpoint() {
		String result = "null";
		String[] args = {"-a", "10.80.51.30"};
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
