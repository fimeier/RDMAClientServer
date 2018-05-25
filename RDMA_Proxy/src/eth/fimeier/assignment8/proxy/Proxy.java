package eth.fimeier.assignment8.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import com.jcabi.aspects.Timeable;
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

	// HTTP 504 Gateway-Timeout
	private int wTime = 1000;


	public Proxy(int port, String rdmaServerIP, String PROXYSITE) throws IOException {

		this.PROXYSITE = PROXYSITE;
		this.proxyPort = port;
		this.rdmaServerIP = rdmaServerIP;

		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();

	}

	public class SimpleClientCall implements Runnable {
		public RdmaProxyEndpoint simpleClient;
		String[] args2;

		public SimpleClientCall(RdmaProxyEndpoint simpleClient_, String[] args2_) {
			this.simpleClient = simpleClient_;
			this.args2 = args2_;
		}

		@Override
		@Timeable(limit = 1500, unit = TimeUnit.MILLISECONDS)
		public void run() {
			try {
				simpleClient.launch(args2);
				if (Thread.currentThread().isInterrupted()) {
					System.out.println("IllegalStateException time out !!!!!!!!!!!!!!!!!!!!!!!!!");
					throw new IllegalStateException("time out");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	class MyHandler implements HttpHandler {

		public void handle(HttpExchange t) throws IOException {

			String reqMethod = t.getRequestMethod();
			String reqURI = t.getRequestURI().getHost() + t.getRequestURI().getPath().toString();

			/*
			 * Abort if not a target content for webserver
			 */
			if (!(reqURI.equals("www.rdmawebpage.com/") || reqURI.equals("www.rdmawebpage.com/network.png")) || !reqMethod.equals("GET")) {
				// byte[] resp = new byte[0];
				t.sendResponseHeaders(404, 0);
				OutputStream os = t.getResponseBody();
				// os.write(resp);
				os.close();
				System.out.println("Proxy: Send 404 HTTP Response.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				return;
			} else {
				System.out.println("Processing client request: initialising RdmaProxyEndpoint...");
			}

			String[] args2 = { "-a", rdmaServerIP };
			RdmaProxyEndpoint simpleClient = new RdmaProxyEndpoint();
			RdmaProxyEndpoint.getHTML = reqURI.equals("www.rdmawebpage.com/") ? true : false;
			byte[] rdmaResult = null;
			try {

				/*
				 * Just for Server-Timeouts
				 */
				SimpleClientCall sc = new SimpleClientCall(simpleClient, args2);
				Thread tt = (new Thread(sc));
				tt.start();
				System.out.println("join("+wTime+")... waiting...");
				tt.join(wTime);

				System.out.println("get result...");
				rdmaResult = simpleClient.result;
				if (rdmaResult == null) {
					System.out.println("Proxy: Prepare 504 HTTP Response....");
					simpleClient.close();
					t.sendResponseHeaders(504, 0);
					OutputStream os = t.getResponseBody();
					// os.write(resp);
					os.close();
					System.out.println("Proxy: Send 504 HTTP Response.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
					return;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (RdmaProxyEndpoint.getHTML) {

				String result = new String(rdmaResult, Charset.defaultCharset());
				System.out.println("rdmaResult as string=" + result);

				System.out.println("Proxy: Prepare response for client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				String response = result;
				byte[] resp = response.getBytes();

				System.out.println("Return response to client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				t.sendResponseHeaders(200, resp.length);
				OutputStream os = t.getResponseBody();
				os.write(resp);
				os.close();
				System.out.println("\n");

			} else {
				System.out.println("rdmaResult should be the png.....");

				System.out.println("Proxy: Prepare response for client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);
				//nothing to do here..... ;-)
				System.out.println("Return response to client.... reqMethod / reqURI = " + reqMethod + " / " + reqURI);

				Headers headers = t.getResponseHeaders();
				headers.set("Content-Type", "image/png");
				System.out.println("rdmaResult.length=" + rdmaResult.length);
				t.sendResponseHeaders(200, 0); // rdmaResult.length
				OutputStream os = t.getResponseBody();
				os.write(rdmaResult);
				os.close();
				System.out.println("\n");

			}
		}

	}

}
