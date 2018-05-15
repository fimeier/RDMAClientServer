package eth.fimeier.assignment8.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.ibm.disni.examples.ReadClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


import org.apache.commons.io.IOUtils;





public class Proxy {

	private String PROXYSITE = "null";

	private HttpServer server;

	private String rdmaServerIp = "192.168.170.30"; 
	private String httpServerURI = "http://192.168.170.30:8080";
	//private int rdmaServerPort = 1919;

	//Todo Threads... damit Parallele Anfragen auch im Proxy

	public Proxy(int port, String PROXYSITE) throws IOException {

		this.PROXYSITE = PROXYSITE;

		server = HttpServer.create(new InetSocketAddress(port), 0);
		//Todo
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {

			String reqMethod = t.getRequestMethod();
			String reqURI = t.getRequestURI().getHost() +  t.getRequestURI().getPath().toString();

			/* Todo
			 * Abort if !reqURI.equals("www.rdmawebpage.com/")
			 */
			if (!reqURI.equals("www.rdmawebpage.com/") || !reqMethod.equals("GET")){
				byte[] resp = new byte[0];
				t.sendResponseHeaders(404, resp.length);
				OutputStream os = t.getResponseBody();
				os.write(resp);
				os.close();
				System.out.println("Proxy: Send 404 HTTP Response.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				return;
			}

			/*
			 * 
			 * 
			 * otherwise forward request to server
			 * 
			 * 
			 * 
			 * 
			 * */
			System.out.println("Proxy: Forward response to server.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

			URL url = null;
			long addr = 0;
			int length = 0;
			int lkey = 0;
			try
			{
				url = new URL(httpServerURI);

				HttpURLConnection remoteConnection = (HttpURLConnection) url.openConnection();

				remoteConnection.setRequestMethod("GET");
				remoteConnection.setRequestProperty("Content-Type", "text/plain");			
				remoteConnection.setRequestProperty("URI",reqURI);
				//OutputStream oss = remoteConnection.getOutputStream();

				//os.write(requestHeader.getInput().getBytes());
				//oss.flush();

				InputStream retS = remoteConnection.getInputStream();

				int cl = remoteConnection.getContentLength();
				//byte[] respServer = new byte[cl]; 



				/*for (int i = 1; i<= cl; i++) {
					respServer = retS.read();

				}*/
				//System.out.println("cl="+cl + " retS"+retS);

				int respCode = remoteConnection.getResponseCode();
				if (respCode==200) {
					System.out.println("Server signaled ok for RDMA request: remoteConnection.getResponseCode()="+remoteConnection.getResponseCode());
				}

				byte[] respServer = IOUtils.toByteArray(retS);
				String respServerString = new String(respServer, Charset.defaultCharset());
				//System.out.println("respServerString="+respServerString);
				List<String> rdmaData = getMetaDataRDMA(respServerString);
				//String rdmaObject = rdmaData.get(0);
				addr = Long.parseLong(rdmaData.get(0), 10);
				length = Integer.parseInt(rdmaData.get(1));
				lkey = Integer.parseInt(rdmaData.get(2));
				
				System.out.println("Proxy::receiving rdma information over HTTP, addr " + addr + ", length " + length + ", key " + lkey);

			}
			catch(Exception e) {
				/*
				 * if e == "Connection refused (Connection refused)" ... 504.....
				 */
				System.out.println(e.getMessage());
			}


			System.out.println("Proxy: Prepare response for client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
			/*
			 * todo
			 */

			System.out.println("Return response to client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

			String response = "test response from proxy; Proxy::receiving rdma information over HTTP, addr " + addr + ", length " + length + ", key " + lkey;
			byte[] resp = response.getBytes();
			t.sendResponseHeaders(200, resp.length);
			OutputStream os = t.getResponseBody();
			os.write(resp);
			os.close();
			System.out.println("\n");
		}

	}

	private List<String> getMetaDataRDMA(String jsonString) {

		List<String> result = new ArrayList<String>();

		final JsonParser parser = Json.createParser(new StringReader(jsonString));
		
		String key = null;
		String value = null;
		while (parser.hasNext()) {
			final Event event = parser.next();
			switch (event) {
			case KEY_NAME:
				//key = parser.getString();
				//result.add(key);
				//System.out.println(key);
				break;
			case VALUE_STRING:
				String string = parser.getString();
				result.add(string);
				//System.out.println(string);
				break;
			}
		}
		parser.close();

		return result;
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
