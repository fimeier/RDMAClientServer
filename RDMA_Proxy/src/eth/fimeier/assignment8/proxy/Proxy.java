package eth.fimeier.assignment8.proxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
			if (!(reqURI.equals("www.rdmawebpage.com/") || reqURI.equals("www.rdmawebpage.com/network.png")) || !reqMethod.equals("GET")){
				byte[] resp = new byte[0];
				t.sendResponseHeaders(404, resp.length);
				OutputStream os = t.getResponseBody();
				os.write(resp);
				os.close();
				System.out.println("Proxy: Send 404 HTTP Response.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				return;
			}

			//remove !!!!!!!!!
			if (reqURI.equals("www.rdmawebpage.com/")) {

				String response ="<html><body><h1>Success!</h1><br/><img src=\"network.png\" alt=\"RDMA REad Image Missing!\"/></body></html>";
				byte[] resp = response.getBytes();

				System.out.println("Return response to client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

				t.sendResponseHeaders(200, resp.length);
				OutputStream os = t.getResponseBody();
				os.write(resp);
				os.close();
				System.out.println("\n");
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

			/*
			 * Implementation with HTTP
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
				System.out.println(e.getMessage());
			}
			 */

			String[] args2 = {"-a", "192.168.170.30"};
			RdmaProxyEndpoint simpleClient = new RdmaProxyEndpoint();
			RdmaProxyEndpoint.getHTML = reqURI.equals("www.rdmawebpage.com/") ? true : false;
			byte[] rdmaResult = null;
			try {
				simpleClient.launch(args2);
				rdmaResult=simpleClient.result;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (RdmaProxyEndpoint.getHTML) {
				String result = new String(rdmaResult, Charset.defaultCharset());
				System.out.println("rdmaResult as string="+result);

				System.out.println("Proxy: Prepare response for client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

				String response = result;
				//response ="<html><body><h1>Success!</h1><br/><img src=\"network.png\" alt=\"RDMA REad Image Missing!\"/></body></html>";
				byte[] resp = response.getBytes();

				System.out.println("Return response to client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

				t.sendResponseHeaders(200, resp.length);
				OutputStream os = t.getResponseBody();
				os.write(resp);
				os.close();
				System.out.println("\n");
			}
			else {
				System.out.println("rdmaResult should be the png.....");

				//test write png
				String absolutePath = new File("").getAbsolutePath() ;
				String pathStaticPages = absolutePath + "/src/eth/fimeier/assignment8/proxy/temp/";
				String path = pathStaticPages+ "PROXYnetwork.png";
				try {
					Files.write(Paths.get(path), rdmaResult);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


				System.out.println("Proxy: Prepare response for client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

				//png in rdmaResult
				String response ="Dummyresponse... replace with png";
				//byte[] resp = response.getBytes();

				System.out.println("Return response to client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

				Headers headers = t.getResponseHeaders();
				headers.set("Content-Type", "image/png");
				//System.out.println("headers.keySet().toString()="+headers.keySet().toString());
				System.out.println("rdmaResult.length="+rdmaResult.length);
				t.sendResponseHeaders(200, 0); //rdmaResult.length
				OutputStream os = t.getResponseBody();
				os.write(rdmaResult);
				os.close();
				System.out.println("\n");

			}






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
