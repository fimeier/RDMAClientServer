Folders in this repository
================================
1. RDMAClientProxy the client as runnable jar
2. RDMAServer the server as runnable jar and static content
3. RDMA_Proxy contains the eclipse project


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
/*
 * Just for Server-Timeouts
*/
SimpleClientCall sc = new SimpleClientCall(simpleClient, args2);
Thread tt = (new Thread(sc));
tt.start();
System.out.println("join("+wTime+")... waiting...");
tt.join(wTime);
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
```

* RdmaProxyEndpoint.class is based on the ReadClient example
  * I added some bigger buffers and some helper methods
  * the run() method fetches the index.html or the network.png file and then closes the connection
  * like in the ReadCLient example, the client asks for the metadata by RDMA send/recv and then issues a one-sided RDMA read opeation to fetch the content from that remote buffer

```java
public void run() throws Exception {
  //..
  if (getHTML) {
    ////////html////////////////////////////////////////////////////////////
    System.out.println("ask server for html...");

    //get buffer metadata to fetch index.html and then get the file
    //...

    //store result
    result = new byte[length];
    //...
  }
  else {
    ////////png/
    System.out.println("ask server for png...");

    //get buffer metadata to fetch network.png and then get the file
    //...

    //store result
    result = pngPicture.clone();
  }
  //prepare final message
  //signal server to close connection

  //close everything
  close();

  /*
   * return to caller; the Proxy
   * result contains the data
  */
}
```

How the server works
================================
* RDMA_Server_Start.class calls RdmaHTTPServerEndpoint.class
* RdmaHTTPServerEndpoint.class is based on the ReadServer example
* the run() method serves one client after another (compare listing)
* Hint: A client requests one of the files and then closes the connection (compare RdmaProxyEndpoint::run() if..else


```java
public void run() throws Exception {
  //init...

  while(runForeEver) {
    //accepting a new client
    System.out.println("RdmaHTTPServerEndpoint::Waiting for new client connection");
    RdmaHTTPServerEndpoint.CustomServerEndpoint endpoint = serverEndpoint.accept(); 

    //the accept() call prepares some buffers for the client and also loads the files from disk into them

    //the client asks for some metadata with a request RDMA send/recv
    switch(request) {

    case "getIndex.html":
      //return metadata for index.html

    case "getnetwork.png":
      //return metadata for index.html

    case "final message close connection":
      //close everything and jump back to while(runForeEver)
}


