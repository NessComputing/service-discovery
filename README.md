Ness Computing Discovery Service
================================

This is the service discovery system for the ness computing
platform. It generally is deployed as a Galaxy tarball and provides
service discovery for the rest of the platform.

Configuration
-------------

In galaxy, a three level (environment, deployment, service)
configuration is used. The environment configuration is used to locate
all the discovery servers and configure platform wide defaults.


environment configuration for development
-----------------------------------------

For development, a single discovery server can be used. 

    ness.zookeeper.clientPort=21052
    ness.zookeeper.clientPortAddress=127.0.0.1
    #
    ness.zookeeper.server.1=127.0.0.1:21050:21051
    #
    # Discovery Service
    #
    ness.discovery.enabled=true
    ness.discovery.root=/ness/srvc/development
    ness.discovery.scan-interval=120s
    ness.discovery.tick-interval=100ms
    ness.discovery.zookeeper-timeout=500ms
    ness.discovery.penalty-time=600s


environment configuration for production
----------------------------------------

For a real environment, at least three servers should be present.

    # ########################################################################
    #
    # Discovery configuration. Do not modify.
    #
    ness.zookeeper.clientPort=18700
    #
    ness.zookeeper.server.1=10.1.1.1:28990:28991
    ness.zookeeper.server.2=10.2.2.2:28990:28991
    ness.zookeeper.server.3=10.3.3.3:28990:28991
    #
    # Discovery Service
    #
    ness.discovery.enabled=true
    ness.discovery.root=/ness/srvc/production
    ness.discovery.scan-interval=120s
    ness.discovery.tick-interval=100ms
    ness.discovery.zookeeper-timeout=500ms
    ness.discovery.penalty-time=600s
    ness.discovery.http-port=8090


service specific (discovery service) configuration
--------------------------------------------------

This is the service specific configuration for a discovery server. 

ness.zookeeper.dataDir=/home/henning/galaxy/deploy/persist/#{env.agent_group}/#{env.agent_id}/discovery
ness.zookeeper.tickTime=15000
ness.zookeeper.initLimit=5
ness.zookeeper.syncLimit=2
ness.zookeeper.maxClientCnxns=100

# Quartz Configuration
org.quartz.scheduler.instanceName = DiscoveryServiceScheduler
org.quartz.threadPool.threadCount = 5

# enable cleanup job
ness.job.zookeeper-cleanup.enabled=true




----
Copyright (C) 2012 Ness Computing, Inc.
