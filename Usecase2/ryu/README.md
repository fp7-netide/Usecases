#UC2 with Ryu

This readme helps the user to run the Ryu applicationdesigned to manage the UC2 network, on top of a Mininet topology.

## Installation

Quick-start

```
1) Start ryu application: ryu-manager Application/IITS_NetManager.py --observe-links
2) Start Mininet: ./mininetStart.sh
3) Start traffic in Mininet to verify the default setup:
	a) ICMP PING to/from all
		mininet> pingall
		*** Ping: testing ping reachability
		h1 -> h2 h3 h4 h5
		h2 -> h1 h3 h4 h5
		h3 -> h1 h2 h4 h5
		h4 -> h1 h2 h3 h5
		h5 -> h1 h2 h3 h4
		*** Results: 0% dropped (20/20 received)

4) Set one link down in Mininet and start traffic to verify backup routes:
	a) Set one link down
		mininet> link s1 s2 down
	b) ICMP PING to/from all
		mininet> pingall
		*** Ping: testing ping reachability
		h1 -> h2 h3 h4 h5
		h2 -> h1 h3 h4 h5
		h3 -> h1 h2 h4 h5
		h4 -> h1 h2 h3 h5
		h5 -> h1 h2 h3 h4
		*** Results: 0% dropped (20/20 received)

```

When mininet is up:
```
2016-07-20 11:57:47,507 - NetIDE Logger - INFO - =================== Mininet Topology SetUp ===================
2016-07-20 11:57:47,512 - NetIDE Logger - INFO - NetIDE - UC2 Topology created
Unable to contact the remote controller at 127.0.0.1:6653
2016-07-20 11:57:52,357 - NetIDE Logger - INFO - ==============================================================

2016-07-20 11:57:52,361 - NetIDE Logger - INFO - ====================== IP Configuration ======================
2016-07-20 11:57:52,612 - NetIDE Logger - INFO - Successfully enforced initial ip settings to the UC topology
2016-07-20 11:57:52,613 - NetIDE Logger - INFO - ==============================================================
```

When Ryu is up (before Mininet, after Mininet start ports are updated adn the application starts receiving packets):
```
netide@netide-VirtualBox:~/NetIDE/UseCases/Usecase2/ryu$ ryu-manager Application/IITS_NetManager.py --observe-links
loading app IITS_NetManager.py
loading app ryu.topology.switches
loading app ryu.controller.ofp_handler
instantiating app ryu.topology.switches of Switches
instantiating app ryu.controller.ofp_handler of OFPHandler
instantiating app IITS_NetManager.py of IITS_NetManager
```

## TODO

* The application can handle a link down, but not a switch down.

## License

See the LICENSE file.

## ChangeLog

uc2-ryu: 2016-7-20 Wen Juan Manuel Sánchez
Maintenance by Juan Manuel Sánchez
<juanmanuel.sanchez@telcaria.com>

  * Tested with Ryu v3.23
