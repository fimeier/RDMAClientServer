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
1. Proxy_start.class calls a simple Proxy.class based on com.sun.net.httpserver
1. Proxy.class checks the validity of the client request, basically

```java
if (!(reqURI.equals("www.rdmawebpage.com/") || reqURI.equals("www.rdmawebpage.com/network.png")) || !reqMethod.equals("GET"))
```

1. then... Proxy.class checks the validity of the client request, basically





