This provides a OSGi bundle that has a component wrapping the JMX Ganglia Metric agent provided by 
https://github.com/ganglia/jmxetric.git

The XML configuration file is of the same format and the comonent takes the same command line arguments
as OSGI properties. Each time the component is reconfigured the xml is re-read.

