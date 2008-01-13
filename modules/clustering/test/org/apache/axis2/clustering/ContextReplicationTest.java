/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.apache.axis2.clustering;

import junit.framework.TestCase;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.configuration.ConfigurationManager;
import org.apache.axis2.clustering.configuration.DefaultConfigurationManager;
import org.apache.axis2.clustering.configuration.DefaultConfigurationManagerListener;
import org.apache.axis2.clustering.context.ContextManager;
import org.apache.axis2.clustering.context.DefaultContextManager;
import org.apache.axis2.clustering.context.DefaultContextManagerListener;
import org.apache.axis2.clustering.tribes.TribesClusterManager;
import org.apache.axis2.clustering.tribes.MembershipManager;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.server.HttpUtils;

import java.util.ArrayList;
import java.util.List;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.IOException;

/**
 * Tests the replication of properties placed the ConfigurationContext, ServiceGroupContext &
 * ServiceContext
 */
public class ContextReplicationTest extends TestCase {

    private static final String TEST_SERVICE_NAME = "testService";

    private static final Parameter domainParam =
            new Parameter(ClusteringConstants.DOMAIN, "axis2.domain." + UUIDGenerator.getUUID());

    // --------------- Cluster-1 ------------------------------------------------------
    private ClusterManager clusterManager1;
    private ContextManager ctxMan1;
    private ConfigurationManager configMan1;
    private ConfigurationContext configurationContext1;
    private AxisServiceGroup serviceGroup1;
    private AxisService service1;
    //---------------------------------------------------------------------------------

    // --------------- Cluster-2 ------------------------------------------------------
    private ClusterManager clusterManager2;
    private ContextManager ctxMan2;
    private ConfigurationManager configMan2;
    private ConfigurationContext configurationContext2;
    private AxisServiceGroup serviceGroup2;
    private AxisService service2;
    //---------------------------------------------------------------------------------

    private static boolean canMulticast;

    private int getPort(int portStart, int retries) throws IOException {
        InetSocketAddress addr = null;
        ServerSocket socket = new ServerSocket();
        int port = -1;
        while (retries > 0) {
            try {
                addr = new InetSocketAddress(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()),
                                             portStart);
                socket.bind(addr);
                port = portStart;
                System.out.println("Can bind Server Socket to:" + addr);
                socket.close();
                break;
            } catch (IOException x) {
                retries--;
                if (retries <= 0) {
                    System.out.println("Unable to bind server socket to:" + addr +
                                       " throwing error.");
                    throw x;
                }
                portStart++;
            }
        }
        return port;
    }

    private void checkMulticast() {

        // Which port should we listen to
        final int port;
        try {
            port = getPort(4000, 1000);
        } catch (IOException e) {
            e.printStackTrace();
            canMulticast = false;
            return;
        }

        // Which address
        final String group = "225.4.5.6";

        Thread receiver = new Thread() {
            public void run() {
                try {
                    MulticastSocket s = new MulticastSocket(port);
                    s.joinGroup(InetAddress.getByName(group));

                    // Create a DatagramPacket and do a receive
                    byte buf[] = new byte[1024];
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    s.receive(pack);

                    System.out.println("Received data from: " + pack.getAddress().toString() +
                                       ":" + pack.getPort() + " with length: " +
                                       pack.getLength());
                    s.leaveGroup(InetAddress.getByName(group));
                    s.close();
                    canMulticast = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    canMulticast = false;
                }
            }
        };
        receiver.start();

        Thread sender = new Thread() {
            public void run() {
                try {
                    MulticastSocket s = new MulticastSocket();
                    byte buf[] = new byte[10];
                    for (int i = 0; i < buf.length; i++) {
                        buf[i] = (byte) i;
                    }
                    DatagramPacket pack = new DatagramPacket(buf, buf.length,
                                                             InetAddress.getByName(group), port);
                    s.setTimeToLive(2);
                    s.send(pack);
                    System.out.println("Sent test data");
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    canMulticast = false;
                }
            }
        };
        sender.start();
    }

    public static void main(String[] args) {
        new ContextReplicationTest().checkMulticast();
    }

    protected void setUp() throws Exception {
        checkMulticast();
        if (!canMulticast) {
            System.out.println("[WARNING] Aborting clustering test since multicasting cannot be carried out");
            return;
        }

        System.setProperty(ClusteringConstants.LOCAL_IP_ADDRESS, HttpUtils.getIpAddress());

        // First cluster
        configurationContext1 =
                ConfigurationContextFactory.createDefaultConfigurationContext();
        serviceGroup1 = createAxisServiceGroup(configurationContext1);
        service1 = createAxisService(serviceGroup1);
        ctxMan1 = getContextManager();
        configMan1 = getConfigurationManager();
        clusterManager1 = getClusterManager(configurationContext1, ctxMan1, configMan1);
        clusterManager1.addParameter(domainParam);
        clusterManager1.init();
        System.out.println("---------- ClusterManager-1 successfully initialized -----------");

        // Second cluster
        configurationContext2 =
                ConfigurationContextFactory.createDefaultConfigurationContext();
        serviceGroup2 = createAxisServiceGroup(configurationContext2);
        service2 = createAxisService(serviceGroup2);
        ctxMan2 = getContextManager();
        configMan2 = getConfigurationManager();
        clusterManager2 = getClusterManager(configurationContext2, ctxMan2, configMan2);
        clusterManager2.addParameter(domainParam);
        clusterManager2.init();
        System.out.println("---------- ClusterManager-2 successfully initialized -----------");
    }

    protected ClusterManager getClusterManager(ConfigurationContext configCtx,
                                               ContextManager contextManager,
                                               ConfigurationManager configManager)
            throws AxisFault {
        ClusterManager clusterManager = new TribesClusterManager();
        configCtx.getAxisConfiguration().setClusterManager(clusterManager);

        configManager.
                setConfigurationManagerListener(new DefaultConfigurationManagerListener());
        clusterManager.setConfigurationManager(configManager);

        contextManager.setContextManagerListener(new DefaultContextManagerListener());
        clusterManager.setContextManager(contextManager);

        clusterManager.setConfigurationContext(configCtx);

        return clusterManager;
    }

    protected AxisServiceGroup createAxisServiceGroup(ConfigurationContext configCtx)
            throws AxisFault {
        AxisConfiguration axisConfig = configCtx.getAxisConfiguration();
        AxisServiceGroup serviceGroup = new AxisServiceGroup(axisConfig);
        axisConfig.addServiceGroup(serviceGroup);
        return serviceGroup;
    }

    protected AxisService createAxisService(AxisServiceGroup serviceGroup) throws AxisFault {
        AxisService service = new AxisService(TEST_SERVICE_NAME);
        serviceGroup.addService(service);
        return service;
    }

    protected ContextManager getContextManager() throws AxisFault {
        ContextManager contextManager = new DefaultContextManager();
        contextManager.setContextManagerListener(new DefaultContextManagerListener());
        return contextManager;
    }

    protected ConfigurationManager getConfigurationManager() throws AxisFault {
        ConfigurationManager contextManager = new DefaultConfigurationManager();
        contextManager.setConfigurationManagerListener(new DefaultConfigurationManagerListener());
        return contextManager;
    }

    public void testSetPropertyInConfigurationContext() throws Exception {
        if (!canMulticast) {
            return;
        }

        {
            String key1 = "configCtxKey";
            String val1 = "configCtxVal1";
            configurationContext1.setProperty(key1, val1);
            ctxMan1.updateContext(configurationContext1);
            String value = (String) configurationContext2.getProperty(key1);
            assertEquals(val1, value);
        }

        {
            String key2 = "configCtxKey2";
            String val2 = "configCtxVal1";
            configurationContext2.setProperty(key2, val2);
            ctxMan2.updateContext(configurationContext2);
            String value = (String) configurationContext1.getProperty(key2);
            assertEquals(val2, value);
        }
    }

    public void testRemovePropertyFromConfigurationContext() throws Exception {
        if (!canMulticast) {
            return;
        }

        String key1 = "configCtxKey";
        String val1 = "configCtxVal1";

        // First set the property on a cluster 1 and replicate it
        {
            configurationContext1.setProperty(key1, val1);
            ctxMan1.updateContext(configurationContext1);
            String value = (String) configurationContext2.getProperty(key1);
            assertEquals(val1, value);
        }

        // Next remove this property from cluster 2, replicate it, and check that it is unavailable in cluster 1
        configurationContext2.removeProperty(key1);
        ctxMan2.updateContext(configurationContext2);
        String value = (String) configurationContext1.getProperty(key1);
        assertNull(configurationContext2.getProperty(key1));
        assertNull(value);
    }

    public void testSetPropertyInServiceGroupContext() throws Exception {
        if (!canMulticast) {
            return;
        }

        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(TEST_SERVICE_NAME);
        configurationContext1.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(TEST_SERVICE_NAME);
        configurationContext2.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceGroupContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceGroupContext1);
        assertEquals(val1, serviceGroupContext2.getProperty(key1));
    }

    public void testRemovePropertyFromServiceGroupContext() throws Exception {
        if (!canMulticast) {
            return;
        }

        // Add the property
        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(TEST_SERVICE_NAME);
        configurationContext1.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(TEST_SERVICE_NAME);
        configurationContext2.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceGroupContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceGroupContext1);
        assertEquals(val1, serviceGroupContext2.getProperty(key1));

        // Remove the property
        serviceGroupContext2.removeProperty(key1);
        assertNull(serviceGroupContext2.getProperty(key1));
        ctxMan1.updateContext(serviceGroupContext2);
        assertNull(serviceGroupContext1.getProperty(key1));
    }

    public void testSetPropertyInServiceGroupContext2() throws Exception {
        if (!canMulticast) {
            return;
        }

        String sgcID = UUIDGenerator.getUUID();

        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(sgcID);
        configurationContext1.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(sgcID);
        configurationContext2.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceGroupContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceGroupContext1);

        assertEquals(val1, serviceGroupContext2.getProperty(key1));
    }

    public void testRemovePropertyFromServiceGroupContext2() throws Exception {
        if (!canMulticast) {
            return;
        }

        // Add the property
        String sgcID = UUIDGenerator.getUUID();

        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(sgcID);
        configurationContext1.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(sgcID);
        configurationContext2.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceGroupContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceGroupContext1);

        assertEquals(val1, serviceGroupContext2.getProperty(key1));

        // Remove the property
        serviceGroupContext2.removeProperty(key1);
        assertNull(serviceGroupContext2.getProperty(key1));
        ctxMan1.updateContext(serviceGroupContext2);
        assertNull(serviceGroupContext1.getProperty(key1));
    }

    public void testSetPropertyInServiceContext() throws Exception {
        if (!canMulticast) {
            return;
        }

        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext1 = serviceGroupContext1.getServiceContext(service1);
        configurationContext1.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);
        assertNotNull(serviceContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext2 = serviceGroupContext2.getServiceContext(service2);
        configurationContext2.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);
        assertNotNull(serviceContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceContext1);

        assertEquals(val1, serviceContext2.getProperty(key1));
    }

    public void testSetPropertyInServiceContext2() throws Exception {
        if (!canMulticast) {
            return;
        }

        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext1 = serviceGroupContext1.getServiceContext(service1);
        configurationContext1.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);
        assertNotNull(serviceContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext2 = serviceGroupContext2.getServiceContext(service2);
        configurationContext2.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);
        assertNotNull(serviceContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceContext1);

        assertEquals(val1, serviceContext2.getProperty(key1));
    }

    public void testRemovePropertyFromServiceContext() throws Exception {
        if (!canMulticast) {
            return;
        }

        // Add the property
        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext1 = serviceGroupContext1.getServiceContext(service1);
        configurationContext1.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);
        assertNotNull(serviceContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext2 = serviceGroupContext2.getServiceContext(service2);
        configurationContext2.addServiceGroupContextIntoApplicationScopeTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);
        assertNotNull(serviceContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceContext1);

        assertEquals(val1, serviceContext2.getProperty(key1));

        // Remove the property
        serviceContext2.removeProperty(key1);
        assertNull(serviceContext2.getProperty(key1));
        ctxMan1.updateContext(serviceContext2);
        assertNull(serviceContext1.getProperty(key1));
    }

    public void testRemovePropertyFromServiceContext2() throws Exception {
        if (!canMulticast) {
            return;
        }

        // Add the property
        ServiceGroupContext serviceGroupContext1 =
                configurationContext1.createServiceGroupContext(serviceGroup1);
        serviceGroupContext1.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext1 = serviceGroupContext1.getServiceContext(service1);
        configurationContext1.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext1);
        assertNotNull(serviceGroupContext1);
        assertNotNull(serviceContext1);

        ServiceGroupContext serviceGroupContext2 =
                configurationContext2.createServiceGroupContext(serviceGroup2);
        serviceGroupContext2.setId(TEST_SERVICE_NAME);
        ServiceContext serviceContext2 = serviceGroupContext2.getServiceContext(service2);
        configurationContext2.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext2);
        assertNotNull(serviceGroupContext2);
        assertNotNull(serviceContext2);

        String key1 = "sgCtxKey";
        String val1 = "sgCtxVal1";
        serviceContext1.setProperty(key1, val1);
        ctxMan1.updateContext(serviceContext1);

        assertEquals(val1, serviceContext2.getProperty(key1));

        // Remove the property
        serviceContext2.removeProperty(key1);
        assertNull(serviceContext2.getProperty(key1));
        ctxMan1.updateContext(serviceContext2);
        assertNull(serviceContext1.getProperty(key1));
    }

    public void testReplicationExclusion1() throws Exception {
        if (!canMulticast) {
            return;
        }

        String key1 = "local_configCtxKey";
        String val1 = "configCtxVal1";
        configurationContext1.setProperty(key1, val1);
        List exclusionPatterns = new ArrayList();
        exclusionPatterns.add("local_*");
        ctxMan1.setReplicationExcludePatterns("defaults", exclusionPatterns);
        ctxMan1.updateContext(configurationContext1);

        String value = (String) configurationContext2.getProperty(key1);
        assertNull(value); // The property should not have gotten replicated
    }

    public void testReplicationExclusion2() throws Exception {
        if (!canMulticast) {
            return;
        }

        String key1 = "local_configCtxKey";
        String val1 = "configCtxVal1";
        configurationContext1.setProperty(key1, val1);
        List exclusionPatterns = new ArrayList();
        exclusionPatterns.add("local_*");
        ctxMan1.setReplicationExcludePatterns("org.apache.axis2.context.ConfigurationContext",
                                              exclusionPatterns);
        ctxMan1.updateContext(configurationContext1);

        String value = (String) configurationContext2.getProperty(key1);
        assertNull(value); // The property should not have gotten replicated
    }

    public void testReplicationExclusion3() throws Exception {
        if (!canMulticast) {
            return;
        }

        String key1 = "local1_configCtxKey";
        String val1 = "configCtxVal1";
        configurationContext1.setProperty(key1, val1);

        String key2 = "local2_configCtxKey";
        String val2 = "configCtxVal2";
        configurationContext1.setProperty(key2, val2);

        String key3 = "local3_configCtxKey";
        String val3 = "configCtxVal3";
        configurationContext1.setProperty(key3, val3);

        List exclusionPatterns1 = new ArrayList();
        exclusionPatterns1.add("local1_*");
        List exclusionPatterns2 = new ArrayList();
        exclusionPatterns2.add("local2_*");
        ctxMan1.setReplicationExcludePatterns("org.apache.axis2.context.ConfigurationContext",
                                              exclusionPatterns1);
        ctxMan1.setReplicationExcludePatterns("defaults",
                                              exclusionPatterns2);
        ctxMan1.updateContext(configurationContext1);

        String value1 = (String) configurationContext2.getProperty(key1);
        assertNull(value1); // The property should not have gotten replicated
        String value2 = (String) configurationContext2.getProperty(key2);
        assertNull(value2); // The property should not have gotten replicated
        String value3 = (String) configurationContext2.getProperty(key3);
        assertEquals(val3, value3); // The property should have gotten replicated

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (clusterManager1 != null) {
            clusterManager1.shutdown();
            System.out.println("------ CLuster-1 shutdown complete ------");
        }
        if (clusterManager2 != null) {
            clusterManager2.shutdown();
            System.out.println("------ CLuster-2 shutdown complete ------");
        }
        MembershipManager.removeAllMembers();
        Thread.sleep(500);
    }
}
