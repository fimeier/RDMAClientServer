================================
How to start the RDMAClientProxy
================================
Option 1) use eclipse and execute Proxy_start
	a) Launcher Program arguments: proxyPort rdmaServerIP
	b) (optional) Launcher VM arguments: -Djava.library.path=/usr/local/lib
Option 2) java -jar RDMAClientProxy.jar proxyPort rdmaServerIP
	optional) -Djava.library.path=/usr/local/lib


================================
How to start the RDMAServer
================================
Option 1) use eclipse and execute RDMA_Server_start
	a) Launcher Program arguments: rdmaServerIP
	b) (optional) Launcher VM arguments: -Djava.library.path=/usr/local/lib
Option 2) java -jar RDMAServer.jar rdmaServerIP
	optional) -Djava.library.path=/usr/local/lib



