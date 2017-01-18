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


# Configuration files

src/main/conf/jmxetric.xml contains configuration for a standard Sling JVM running Oak TarMK. 

# Test Ganglia install

If you dont have access to a full Ganglia instance with a UI and dont have time to install you can use this monitoing bundle in standalone mode.
You will need gmond and gmetad. Setup a gmond instance to receive the data over unicast UDP. Make it mute (mute=yes) but not deaf (deaf=no). 
That done configure a gmetad instance to query the gmond instance and build rrd files as the data comes in. Once you have rrd files you can
use one of the many rrd viewing tools or use rrdtool itself to generate graphs. For instance on OSX

    brew install ganglia

Run the daemons in foreground

    sudo gmond -f &
    sudo gmetad -f &

run the java app with the monitor pointing the reporter to send data to UDP 127.0.0.1:8649

Some time later, view the rrd as a graph.

    rrdtool  graph image.png "DEF:segment=/usr/local/var/lib/ganglia/rrds/unspecified/localhost/jvm_SegmentWrites_m15r.rrd:sum:AVERAGE" "LINE1:segment#ff0000:Segment Writes"
    open image.png


gmond.conf  listen on UDP 8649, and TCP 8649

    globals {
      daemonize = yes
      setuid = yes
      user = nobody
      debug_level = 0
      max_udp_msg_len = 1472
      mute = yes
      deaf = no
      allow_extra_data = yes
      host_dmax = 86400 /*secs. Expires (removes from web interface) hosts in 1 day */
      host_tmax = 20 /*secs */
      cleanup_threshold = 300 /*secs */
      gexec = no
      send_metadata_interval = 30 /*secs */
    }
    cluster {
      name = "development"
    }
    udp_recv_channel {
      port = 8649
      bind = 127.0.0.1
      retry_bind = true
    }
    tcp_accept_channel {
      port = 8649
      gzip_output = no
    }

gmetad.conf  retrive data from localhost 8649

    data_source "my cluster" 127.0.0.1:8649
    case_sensitive_hostnames 0

