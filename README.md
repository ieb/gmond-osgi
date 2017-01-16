This provides a OSGi bundle that has a component wrapping the JMX Ganglia Metric agent provided by 
https://github.com/ganglia/jmxetric.git


[![Build Status](https://travis-ci.org/ieb/gmond-osgi.svg?branch=master)](https://travis-ci.org/ieb/gmond-osgi)


The XML configuration file is of the same format and the comonent takes the same command line arguments
as OSGI properties. Each time the component is reconfigured the xml is re-read.


# OSGi Properties for JMXetricComponent

## host (default: 127.0.0.1)

### unicast 

This is the IP or DNS of the host to send to. It must be a IPv4 hostname as the sender doesnt currently support IPv6. If using localhost take care 
to ensure that localhost is IPv4 as on OSX only 127.0.0.1 is IPv4 and the UDP packets wont be sent. The host used should be one of the hosts running 
gmond from which the gmetad collects metrics. Do not try to send metrics to an intermediate gmond daemon (ie on localhost) as the metrics will
be received but not forwarded.

### multicast

This is the multicast address. Take care not to use an address shared by another cluster. In a multicast environment all host share the same 
multicast fabric and one host or more hosts are queried by gmetad

## port (default: 8649)

The UDP port to send metrics and metrics metadata to. In multicast mode this represents the multicast group.

## wireformat31x (default: true)

If you are running a gmond instances that does not understand the 3.1.x wire format, set this to false to use the older wire format, otherwise 
use the 3.1.x format. A 3.1.x or later gmond wont understand the older wire format and metrics will be dropped.

## spoof (default: none)

Spoof the hostname from which metrics are sent.

## process (default:jvm)

The name of the process pre-pended to any metric names.

## config (default: ./jmxetric.xml)

The path from the working directory to the jmxetric.xml file, normally and absolute path to avoid confusion. This file will be watched.
If touched, the configuration will be reloaded.

