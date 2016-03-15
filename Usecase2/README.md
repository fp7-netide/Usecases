# Use Case 2 : Integrated IT System

## Use Case Presentation

### Overview

This use case describes an Integrated System (IS) which will typically be deployed within a larger data centre. Looked at from the outside, from an infrastructure management perspective, the IS behaves as one computer with high processing capabilities. Looked at from the inside the IS is a network of closely collaborating computers. The Use Case addresses the customer need to rapidly introduce highly scalable computing infrastructure within an existing data centre environment. The solution is encapsulated from sensitive network management processes in the rest of the data centre and can therefore be deployed without causing network management conflicts. Most existing server solutions expose more of their network management to the surrounding environment. It is hard to rapidly integrate many of them without disrupting the existing environment. The solution has its own internal network which has to be defined, tested, deployed and maintained. The solution benefits from the clear separation of the control and forwarding planes which underlies SDN.

### Illustration

![Alt text](usecase2.png?raw=true " ")

### Installation

Quick-start
	Attention:	Pyretic is a VM released on top of Mininet and POX version betta.

To run the following, download and install pyretic VM first (http://frenetic-lang.org/pyretic/)

```
1) Start Mininet: ./mininetStart.sh
2) Start Pyretic (you need to indicate the client, e.g. POX): ./pyreticStart.sh pox
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

4) Start traffic in Mininet and change Mininet topology to verify alternative setups:

```

When mininet is up:
```
2014-12-11 04:51:55,853 - NetIDE Logger - INFO - =================== Mininet Topology SetUp ===================
2014-12-11 04:51:55,854 - NetIDE Logger - INFO - NetIDE - UC2 Topology created
2014-12-11 04:51:57,052 - NetIDE Logger - INFO - ==============================================================

2014-12-11 04:51:57,052 - NetIDE Logger - INFO - ====================== IP Configuration ======================
2014-12-11 04:51:57,079 - NetIDE Logger - INFO - Successfully enforced initial ip settings to the UC topology
2014-12-11 04:51:57,080 - NetIDE Logger - INFO - ==============================================================
```

When Pyretic is up (before Mininet, after Mininet start policies are updated):
```
mininet@mininet-vm:~/NetIDE/UC2-pyretic-pox-of_client$ ./pyreticStart.sh pox
========================================= NetIDE Pyretic =========================================
========================================================
===== Pyretic Repository is successfully updated
========================================================

========================================================
Listing /home/mininet/pyretic/pyretic/modules/ ...
Compiling /home/mininet/pyretic/pyretic/modules/Commons.py ...
Compiling /home/mininet/pyretic/pyretic/modules/Monitor.py ...
Compiling /home/mininet/pyretic/pyretic/modules/RoutingSystem.py ...
Compiling /home/mininet/pyretic/pyretic/modules/UC2_IITS_NetManager.py ...
===== Pyretic Modules are compiled
========================================================

===== Pyretic starts with POX SDN Controller
[Routing System]: __init__: [1, 2, 3, 4, 5, 6, 7, 8] [10.0.1.11, 10.0.1.12, 10.0.1.13, 10.0.1.14, 10.0.1.15]
[Routing System]: set_initial_state
[Routing System]: update_policy
  self.policy parallel:
    if
        negate:
            match: ('ethtype', 2054)
    then
        drop
    else
        drop
    sequential:
        identity
        packets
        sequential:
            LimitFilter
            identity
            FwdBucket
[Routing System]: set_network
  self.flood: drop
  self.topology: ---------------------------------------------
switch  |  switch edges    |  egress ports  |
---------------------------------------------
---------------------------------------------
[Routing System]: update_policy
  self.policy parallel:
    if
        negate:
            match: ('ethtype', 2054)
    then
        drop
    else
        drop
    sequential:
        identity
        packets
        sequential:
            LimitFilter
            identity
            FwdBucket
------------------------------ Packet Counts ------------------------------
---------------------------------------------------------------------------
POX 0.1.0 (betta) / Copyright 2011-2013 James McCauley, et al.
Connected to pyretic frontend.
INFO:core:POX 0.1.0 (betta) is up.

```

### Testing

This use case has been installed and experimented with using V0.2 of the NetIDE software. A particular focus was on the usability of the installation process and the ease of use of the topology editor, the main tool available at the time. As yet no systematic test bed or set of regression tests has been developed for this use case. Performance measurement is also an outstanding task. These topics will be addressed in the 3rd year of the NetIDE project.
