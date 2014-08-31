# dhcp

## General

This is a DHCP server I'm developing to play pranks on my brother. My wireless router doesn't allow me to set the DNS server via DHCP, so rather than standing up a DHCP server outside my home network, I'm going to just make my own DHCP server. Why the heck not.

## Requirements

This library uses the Simple Logging Facade For Java ([slf4j](http://www.slf4j.org/)). You will need to supply a runtime logging framework, as described [here](http://www.slf4j.org/manual.html).

* Java SDK 1.7 or above
* Maven 3.something
* Logging framework
 
## Installation

This is meant to be a library that other applications use (in coordination with the DNS shenanigans library hopefully). If you would like to test a standalone version, there is a test application in `src/test/java/org/code_revue/dhcp/TestApp.java`. The DHCP protocol uses port 67 for the server, so you will probably need to run it as a superuser or do some `iptables` or `pfctl` port mapping.
