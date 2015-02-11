#UC3 with floodlight

This readme helps the user to run  the UC3 network, with a Mininet topology over floodlight.

## Installation

###Quick-start

```
1) Start Mininet: sh mininetStart.sh
2) Start Floodlight: sh floodlight.sh
3) Start traffic in Mininet to verify the setup:
	a) > source Topology/UC3conf.cli
	b) > pingall 
	ICMP PING to/from all
		h1 -> h2 h3 h4 h5 h6
		h2 -> h1 h3 h4 h5 h6
		h3 -> h1 h2 h4 h5 h6
		h4 -> h1 h2 h3 h5 h6  
        h5 -> h1 h2 h3 h4 h6
        h6 -> h1 h2 h3 h4 h5
		*** Results: 0% dropped (30/30 received)
```

Check Switch rules
```
s1 dpctl dump-flows tcp:localhost:6633
s1 ovs-ofctl dump-flows tcp:127.0.0.1:6633
```

What is missing:

The core feature is the **monitoring** of the links.
We need to use a utility like _tc_ to change the links' behaviour.



##Use

To use the application, you can do the following: 

The Monitor can be accessed through its REST API. Is will respond with data in the JSON format. The path to the servlet is :
http://flooodlight_IP:REST_Port/wm/monitor/
(usually http://localhost:8080/wm/monitor/ works if on the same machine as the controller)

The different calls currently available are :

*getAllData*
This will get all of the data currently available in Monitor in an organized format.

*getSwitchData?id=X*
This will get all the data about the switch with the id *X*.

*getPortData?id=X&port=Y*
This will get all the data about the *Y*th port of the switch *X*.

*getRouteData?id1=X&id2=Y*
This will get the data on the shortest route between *X* and *Y*.

If the query is invalid (typo, non existing switch or port,...) a pertinent error message will be sent.


###Optional 

Import the Monitor module to Floodlight (from source)


If you want to undestand the whole process of adding a new module, e.g. a new app, 
on top of Floodlight, follow this procedure:

0) Download Floodlight (version 0.90) from the repo
0a) Import the project in Eclipse, or modify the src code using an editor (the following steps refer to the second solution)
1) Add a new Package: mkdir <floodlight_folder>/src/main/java/net/floodlightcontroller/monitor
2) Copy the java classes (form Monitor sources) in the new folder
2a) Inspect the classes to understand how a module is done
3) Modify the floodlight properties files:
	- META-INF
		- vim <floodlight_folder>/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
		- add the line: "net.floodlightcontroller.Monitor"
	- floodlightdefault.properties
		- vim <floodlight_folder>/src/main/resources/floodlightdefault.properties
		- add the line: "net.floodlightcontroller.Monitor,\" before "net.floodlightcontroller.perfmon.PktInProcessingTime,\"
		- add these lines at the end
			net.floodlightcontroller.monitor.internal.Monitor.controllerid = 42	(required, the ID can be any Long)
			net.floodlightcontroller.monitor.internal.Monitor.update = 1000		(optional)
			net.floodlightcontroller.monitor.internal.Monitor.latency = 1000	(optional)
			net.floodlightcontroller.monitor.internal.Monitor.history = 10		(optional)
4) Compile the code:
	- cd <floodlight_folder>
	- ant
5) Run the jar
	- java -jar <floodlight_folder>/target/floodlight.jar