package eth.fimeier.assignment8.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

//import com.ibm.disni.examples.ReadClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;





public class Proxy {

	private String PROXYSITE = "null";

	private HttpServer server;

	private int proxyPort = 8000;
	private String rdmaServerIP = "10.80.51.30";
	private String proxysite = "www.rdmawebpage.com";

	public Proxy(int port, String rdmaServerIP, String PROXYSITE) throws IOException {

		this.PROXYSITE = PROXYSITE;
		this.proxyPort = port;
		this.rdmaServerIP = rdmaServerIP;

		server = HttpServer.create(new InetSocketAddress(port), 0);
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
				//byte[] resp = new byte[0];
				t.sendResponseHeaders(404, 0);
				OutputStream os = t.getResponseBody();
				//os.write(resp);
				os.close();
				System.out.println("Proxy: Send 404 HTTP Response.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				return;
			}


			String[] args2 = {"-a", rdmaServerIP};
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

				/*
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
				 */


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

}
