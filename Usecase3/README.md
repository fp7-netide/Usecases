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

![UC3 illustration](https://github.com/fp7-netide/PoC/blob/development/use_cases/use_case_3/topology.png)

###Details

The *monitoring* application developed by the company will be tested on the network.
It will have to _reliably_ determine the bandwidth and latency of the network's links while traffic from the host is running.

###Future work


##Application with NetIDE

* The Use Case must be simulated within NetIDE to test the application (topology, hosts, traffic, controllers...).
* The application must work with floodlight for the HQ and ODL for the RB
* Extended tests : the application written with floodlight runs on ryu for the HQ and ODL for the RB
* [TODO]

