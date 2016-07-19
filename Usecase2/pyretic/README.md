#UC2 with Pyretic (POX client)

This readme helps the user to run the Pyretic module designed to manage the UC2 network, on top of a Mininet topology.

## Installation

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

## TODO

* The code is in alpha version and has been tested with simple topologies (only one failed link allowed). 

## License

See the LICENSE file.

## ChangeLog

uc2-pyretic: 2014-12-12 Fri Elisa Rojas
Maintenance by Elisa Rojas
<elisa.rojas@telcaria.com>

  * Tested with the master branch of Pyretic's VM v0.2.2 + git pull master (commit 376f63a6d249c9a2e434b87f565982cab24fb6ad of Wed Aug 6 22:57:44 2014) 
