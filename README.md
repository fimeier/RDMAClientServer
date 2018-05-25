How to start the RDMAClientProxy
================================
* Option 1) use eclipse and execute Proxy_start
  * Launcher Program arguments: proxyPort rdmaServerIP
  * (optional) Launcher VM arguments: -Djava.library.path=/usr/local/lib
* Option 2) $ java -jar RDMAClientProxy.jar proxyPort rdmaServerIP
  * optional) -Djava.library.path=/usr/local/lib


How to start the RDMAServer
================================
* Option 1) use eclipse and execute RDMA_Server_start
  * Launcher Program arguments: rdmaServerIP
  * (optional) Launcher VM arguments: -Djava.library.path=/usr/local/lib
* Option 2) $ java -jar RDMAServer.jar rdmaServerIP
  * optional) -Djava.library.path=/usr/local/lib

How the proxy works
================================
* Proxy_start.class calls a simple Proxy.class based on com.sun.net.httpserver
* Proxy.class checks the validity of the client request, basically...

```java
if (!(reqURI.equals("www.rdmawebpage.com/") || reqURI.equals("www.rdmawebpage.com/network.png")) || !reqMethod.equals("GET"))
```

* ...and returns HTTP 404 (for "wrong" requests) or HTTP 200 and the content by calling RdmaProxyEndpoint.class
  * if the server doesn't respond, a HTTP 504 will be sent to the client; compare:

```java
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
```

* RdmaProxyEndpoint.class is based





