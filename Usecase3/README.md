#Use Case 3 : Hybrid Environement Development

##Use Case Presentation

###Overview

A company with multiple location (like a Headquarter (HQ) and a Regional Branch (RB)) wants to lower its software development costs.
To prevent the development of multiple applications to run on different controller platforms, it will use NetIDE.

The development will follow three steps :

1. The development of the application in a controller platform, floodlight
2. The test of the application
3. The deployment of the application on the two networks (floodlight and ODL)

The operation will be declared a success if the code can run seamlessly on the two controller platforms without additional development. 

###Illustration

![UC3 illustration](https://github.com/fp7-netide/Usecases/blob/development/Usecase3/topology.png)

###Installation

This tutorial is used to run  the Use Case 3 network, with a Mininet topology with Floodlight as the HQ controller and Floodlight over OpenDaylight as the RB controller.

To launch the Floodlight controller in the HQ:

```
1) Start Floodlight: sh floodlight.sh
```

To launch the Controller in the RB:
 
```
1) Choose a shim available here: https://github.com/fp7-netide/Engine (this Use Case has been tested with the ODL shim)
2) Follow the installation step in the Readme of the shim
3) Install the Floodlight Backend following these instructions: https://github.com/fp7-netide/Engine/tree/master/floodlight-backend/v1.0
4) Install the Monitor application in Floodlight:
  a) Add a new Package: mkdir <floodlight_folder>/src/main/java/net/floodlightcontroller/monitor
  b) Copy the java classes (form Monitor sources) in the new folder
  c) Modify the floodlight properties files:
	- META-INF
		- vim <floodlight_folder>/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
		- add the line: "net.floodlightcontroller.Monitor"
	- floodlightdefault.properties
		- vim <floodlight_folder>/src/main/resources/floodlightdefault.properties
		- add the line: "net.floodlightcontroller.Monitor,\" before "net.floodlightcontroller.perfmon.PktInProcessingTime,\"
		- add these lines at the end:
			net.floodlightcontroller.monitor.internal.Monitor.controllerid = 42	(required, the ID can be any Long)
			net.floodlightcontroller.monitor.internal.Monitor.update = 1000		(optional)
			net.floodlightcontroller.monitor.internal.Monitor.latency = 1000	(optional)
			net.floodlightcontroller.monitor.internal.Monitor.history = 10		(optional)
  d) Build the code:
	- cd <floodlight_folder>
	- ant
5) Run the jar
	- java -jar <floodlight_folder>/target/floodlight.jar
```

### Testing

```
1) Start Mininet: sh mininetStart.sh
2) Start traffic in Mininet to verify the setup:
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

##What is missing:

The core feature is the **monitoring** of the links. Launching mininet without additionnal arguments means all link will have the same characteristics (bandwidth, latency, loss rate, ...).
We need to use a utility like _tc_ to change the links' behaviour dynamically (soon to be implemented in the IDE).
