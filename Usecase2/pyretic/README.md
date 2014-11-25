#UC1 with Pyretic (POX client)

This readme helps the user to run Pyretic module designed to manage the UC1 network, on top of a Mininet topology.

## Installation

Quick-start
	Attention:	Pyretic is a VM released on top of Mininet and POX version betta.

To run the following, download and install pyretic VM first (http://frenetic-lang.org/pyretic/)

```
1) Start Mininet: ./mininetStart.sh
2) Start Pyretic (you need to indicate the client, e.g. POX): ./pyreticStart.sh pox
3) Start traffic in Mininet to verify the setup:
	a) ICMP PING to/from all
		h1 -> X X X
		h2 -> X h3 X
		h3 -> X h2 X
		h4 -> X X X   (h4 should work but right now it is stateless)
		*** Results: 83% dropped (2/12 received)
	b) TCP/UDP to/from FW1
		- xterm h2 h3                          # Open 2 shells for each server
		- tcpdump -i h2-eth0                   # To the shell of h2. To be able to see what packets come in
		- tcpdump -i h3-eth0                   # To the shell of h3. To be able to see what packets come in
		- h2 iperf -s -u -p 53 &               # DNS server side. Start background
		  h1 iperf -c 10.0.1.17 -u -p 53 -t 5  # UDP/DNS form ExternalHost to DNS   (captured by tcpdump)
		- h3 iperf -s -p 80 &                  # Web server side. Start background
		  h1 iperf -c 10.0.1.18 -p 80 -t 5     # TCP/HTTP form ExternalHost to Web  (captured by tcpdump)
		- All other traffic from h1/h2 to h3/h4 does not work
			- h3 iperf -s -p 90 &
			  h1 iperf -c 10.0.1.18 -p 90 -t 5       <-- BLOCKED
			- h2 iperf -s -u -p 60 &
			  h1 iperf -c 10.0.1.17 -u -p 60 -t 5    <-- BLOCKED
	
	c) LoadBalancer (Round-robin, per-flow)
		[Load Balancer]: Mapping c:10.0.1.17 to s:10.0.0.1
		[Load Balancer]: Mapping c:10.0.1.18 to s:10.0.0.2
		[Load Balancer]: Mapping c:10.0.1.32 to s:10.0.0.1
```

To check switch rules
```
s1 dpctl dump-flows tcp:localhost:6633
s1 ovs-ofctl dump-flows tcp:127.0.0.1:6633
```

When mininet is up:
```
2014-06-17 03:32:40,119 - NetIDE Logger - INFO - =================== Mininet Topology SetUp ===================
2014-06-17 03:32:40,119 - NetIDE Logger - INFO - NetIDE - UC1 Topology created
2014-06-17 03:32:41,766 - NetIDE Logger - INFO - ==============================================================

2014-06-17 03:32:41,766 - NetIDE Logger - INFO - ====================== IP Configuration ======================
2014-06-17 03:32:41,799 - NetIDE Logger - INFO - Successfully enforced initial ip settings to the UC topology
2014-06-17 03:32:41,800 - NetIDE Logger - INFO - ==============================================================
```

When Pyretic is up
```
========================================= NetIDE Pyretic =========================================
========================================================
===== Pyretic Repository is successfully updated
========================================================

========================================================
Listing /home/mininet/pyretic/pyretic/modules/ ...
Compiling /home/mininet/pyretic/pyretic/modules/Commons.py ...
Compiling /home/mininet/pyretic/pyretic/modules/Firewall.py ...
Compiling /home/mininet/pyretic/pyretic/modules/LoadBalancer.py ...
Compiling /home/mininet/pyretic/pyretic/modules/Monitor.py ...
Compiling /home/mininet/pyretic/pyretic/modules/UC1_DC_NetManager.py ...
===== Pyretic Modules are compiled
========================================================

if
    union:
        intersection:
            match: ('srcip', 10.0.0.0/24) ('switch', 9) ('dstip', 10.0.1.17) ('protocol', 17) ('ethtype', 2048)
            negate:
                match: ('dstport', 53)
        intersection:
            match: ('srcip', 10.0.1.17) ('switch', 9) ('dstip', 10.0.0.0/24) ('protocol', 17) ('ethtype', 2048)
            negate:
                match: ('srcport', 53)
        match: ('srcip', 10.0.0.0/24) ('switch', 9) ('dstip', 10.0.1.17) ('protocol', 1) ('ethtype', 2048)
        match: ('srcip', 10.0.0.0/24) ('switch', 9) ('dstip', 10.0.1.17) ('protocol', 6) ('ethtype', 2048)
        intersection:
            match: ('srcip', 10.0.0.0/24) ('switch', 9) ('dstip', 10.0.1.18) ('protocol', 6) ('ethtype', 2048)
            negate:
                match: ('dstport', 80)
        intersection:
            match: ('srcip', 10.0.1.18) ('switch', 9) ('dstip', 10.0.0.0/24) ('protocol', 6) ('ethtype', 2048)
            negate:
                match: ('srcport', 80)
        match: ('srcip', 10.0.0.0/24) ('switch', 9) ('dstip', 10.0.1.18) ('protocol', 1) ('ethtype', 2048)
        match: ('srcip', 10.0.0.0/24) ('switch', 9) ('dstip', 10.0.1.18) ('protocol', 17) ('ethtype', 2048)
        match: ('switch', 10) ('dstip', 10.0.1.32) ('ethtype', 2048)
then
    drop
else
    [DynamicPolicy]
    parallel:
        flood on:
        None
        packets
        sequential:
            LimitFilter
            identity
            FwdBucket
POX 0.1.0 (betta) / Copyright 2011-2013 James McCauley, et al.
Connected to pyretic frontend.
INFO:core:POX 0.1.0 (betta) is up.

```

## TODO

* The code is in alpha version and has been tested with simple topologies. 

## License

See the LICENSE file.

## ChangeLog

uc1-pyretic: 2014-06-02 Mon Georgios Katsikas
Maintenance by Elisa Rojas
<elisa.rojas@imdea.org>

  * Tested with the master branch of Pyretic's VM v0.2.2 + git pull master (commit 376f63a6d249c9a2e434b87f565982cab24fb6ad of Wed Aug 6 22:57:44 2014) 