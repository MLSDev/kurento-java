Kurento Tree Server
======================

The Kurento Tree Server (KTS) is a Kurento Server component that provides a
service for creating broadcasting trees with WebRTC.

Installation instructions
-------------------------

KTS is implemented with Java 7 technology and Java 7 is the only
prerequisite. The KTS is provided as a .zip containing a Java executable archive
(.jar).

Assuming that the command 'java' points to a Java 7 JRE executable, KCS
can be executed as:

    java -jar kurento-tree-server.jar

All configuration is retrieved from file 'kurento-tree.conf.json' located in config
folder in the place you are executing java command.

{
   "ws": {
     "port": "8890",
     "path": "kurento-tree"
   },
   "kms": {
      "uris": [
         "ws://<ip-of-server1>:<port-of-server1>/kurento",
         "ws://<ip-of-server2>:<port-of-server2>/kurento",
         "ws://<ip-of-server3>:<port-of-server3>/kurento"
      ]
   }
}

Also, the configuration keys can be overridden with Java system properties. For
example, if you want to override 'ws.port', you have to execute
the following command:

    java -Dws.port=8888 -jar kurento-tree-server.jar

Configuration options
----

The meaning of general configuration properties are:

**WebSocket interface**

* **ws.port:** The http/websocket port of KTS. This port
  will be used for clients to connect to Kurento Server. If not specified, the
  value 8888 will be used.
* **ws.path:** The websocket relative path of KTS. If not
  specified, the relative path will be 'kurento-tree'.
* **kms.uris:** The list of URIs where the KMS are available


Custom location of configuration file
------------------

You can change the location and name of the configuration file with the
following command:

    java -DconfigFilePath=/opt/kurento/config.json -jar kurento-tree-server.jar