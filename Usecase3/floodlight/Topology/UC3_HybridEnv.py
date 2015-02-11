#!/usr/bin/python

"""
NetIDE UC3 mininet file
"""

from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info


def UC3Net():

    "Create an network and add nodes to it."

    net = Mininet(autoSetMacs=True)

    print "*** Creating controllers"
    #cF is the floodlight controller
    #cO is the ODL controller
    cF = net.addController( 'cF', controller=RemoteController, ip="10.1.1.183", port=6633 )
    cO = net.addController( 'cO', controller=RemoteController, ip="10.1.1.11", port=6633 )

    print "*** Creating hosts"
    h1 = net.addHost( 'h1', mac="00:00:00:00:00:0A", ip='10.0.1.10' )
    h2 = net.addHost( 'h2', mac="00:00:00:00:09:14", ip='10.0.1.20' )
    h3 = net.addHost( 'h3', mac="00:00:00:00:00:1E", ip='10.0.1.30' )
    h4 = net.addHost( 'h4', mac="00:00:00:00:00:28", ip='10.0.1.40' )
    h5 = net.addHost( 'h5', mac="00:00:00:00:00:32", ip='10.0.1.50' )
    h6 = net.addHost( 'h6', mac="00:00:00:00:00:3C", ip='10.0.1.60' )

    print "*** Creating networks"

    s1 = net.addSwitch( 's1' , cls=OVSSwitch, failMode="open", dpid="0000000000000001")
    s2 = net.addSwitch( 's2' , cls=OVSSwitch, failMode="open", dpid="0000000000000002")
    s3 = net.addSwitch( 's3' , cls=OVSSwitch, failMode="open", dpid="0000000000000003")
    s4 = net.addSwitch( 's4' , cls=OVSSwitch, failMode="open", dpid="0000000000000004")
    s5 = net.addSwitch( 's5' , cls=OVSSwitch, failMode="open", dpid="0000000000000005")
    s6 = net.addSwitch( 's6' , cls=OVSSwitch, failMode="open", dpid="0000000000000006")
    s7 = net.addSwitch( 's7' , cls=OVSSwitch, failMode="open", dpid="0000000000000007")
    
    
    s1.linkTo( s2 )
    s1.linkTo( s3 )
    s2.linkTo( s4 )
    s3.linkTo( s4 )
    # we can represent the ISP as a simple link (for now)
    s3.linkTo( s5 )
    s5.linkTo( s6 )
    s5.linkTo( s7 )
    s6.linkTo( s7 )

    "Provider->Client links"
    h1.linkTo( s1 )
    h2.linkTo( s1 )
    h3.linkTo( s2 )
    h4.linkTo( s5 )
    h5.linkTo( s7 )
    h6.linkTo( s7 )

    info( '*** Starting networks\n')
    net.build()
    s1.start( [ cF ] )
    s2.start( [ cF ] )
    s3.start( [ cF ] )
    s4.start( [ cF ] )
    s5.start( [ cO ] )
    s6.start( [ cO ] )
    s7.start( [ cO ] )

    info( '*** Running CLI\n' )
    CLI( net )

    info( '*** Stopping network' )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    UC3Net()
