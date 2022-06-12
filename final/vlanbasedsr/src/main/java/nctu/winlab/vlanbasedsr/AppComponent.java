/*
 * Copyright 2022-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nctu.winlab.vlanbasedsr;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;


import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;

import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;

import org.onosproject.net.host.HostService;

import org.onosproject.net.topology.TopologyService;

import java.util.List;
import com.google.common.collect.Lists;
import java.util.Set;
import java.util.HashMap;
import java.util.*;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class AppComponent{

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final NameConfigListener cfgListener = new NameConfigListener();
    private final ConfigFactory factory =
        new ConfigFactory<ApplicationId, NameConfig>(
            APP_SUBJECT_FACTORY, NameConfig.class, "ConfigInfo") {
              @Override
              public NameConfig createConfig(){
                return new NameConfig();
              }
            };

    private ApplicationId appId;

    Map<DeviceId, Integer> VlanID_Table = new HashMap();
    Map<IpPrefix, DeviceId> IPSubnet_Table = new HashMap();
    Map<Integer, String> ConnectPoint_Table_temp = new HashMap();
    Map<Integer, ConnectPoint> ConnectPoint_Table = new HashMap();
    Map<Integer, MacAddress> Mac_Table = new HashMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
        protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    private vlan processor = new vlan();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nctu.winlab.vlanbasedsr");
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(factory);
        packetService.addProcessor(processor,PacketProcessor.director(2));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        packetService.removeProcessor(processor);
        log.info("Stopped");
    }

    private class NameConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)
                && event.configClass().equals(NameConfig.class)) {
                NameConfig config = cfgService.getConfig(appId, NameConfig.class);
                if (config != null) {
                    VlanID_Table.put(DeviceId.deviceId("of:0000000000000002"), 101);
                    VlanID_Table.put(DeviceId.deviceId("of:0000000000000001"), 102);
                    VlanID_Table.put(DeviceId.deviceId("of:0000000000000003"), 103);
                    IPSubnet_Table.put(IpPrefix.valueOf("10.0.2.0/24"), DeviceId.deviceId("of:0000000000000002"));
                    IPSubnet_Table.put(IpPrefix.valueOf("10.0.3.0/24"), DeviceId.deviceId("of:0000000000000003"));
                    ConnectPoint_Table.put(1, ConnectPoint.deviceConnectPoint("of:0000000000000003/1"));
                    ConnectPoint_Table.put(2, ConnectPoint.deviceConnectPoint("of:0000000000000003/2"));
                    ConnectPoint_Table.put(3, ConnectPoint.deviceConnectPoint("of:0000000000000002/1"));
                    ConnectPoint_Table.put(4, ConnectPoint.deviceConnectPoint("of:0000000000000002/2"));
                    ConnectPoint_Table.put(5, ConnectPoint.deviceConnectPoint("of:0000000000000002/3"));
                    Mac_Table.put(1, MacAddress.valueOf("ea:e9:78:fb:fd:01"));
                    Mac_Table.put(2, MacAddress.valueOf("ea:e9:78:fb:fd:02"));
                    Mac_Table.put(3, MacAddress.valueOf("ea:e9:78:fb:fd:03"));
                    Mac_Table.put(4, MacAddress.valueOf("ea:e9:78:fb:fd:04"));
                    Mac_Table.put(5, MacAddress.valueOf("ea:e9:78:fb:fd:05"));
                    //VlanID_Table = config.getVlanID();
                    //IPSubnet_Table = config.getIPSubnet();
                    //ConnectPoint_Table_temp = config.getConnectPoint(); 
                    //Mac_Table = config.getMac();

                    //for(Integer i : ConnectPoint_Table_temp.keySet())
                        //ConnectPoint_Table.put(i, ConnectPoint.deviceConnectPoint(ConnectPoint_Table_temp.get(i)));
                    
                    log.info("###########################");
                    for(DeviceId d : VlanID_Table.keySet())
                        log.info(VlanID_Table.get(d).toString());
                    for(IpPrefix d : IPSubnet_Table.keySet())
                        log.info(IPSubnet_Table.get(d).toString());
                    for(Integer d : ConnectPoint_Table.keySet())
                        log.info(ConnectPoint_Table.get(d).toString());
                    for(Integer d : Mac_Table.keySet())
                        log.info(Mac_Table.get(d).toString());

                    begin();
                }
                else
                    log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            }
        }
    }

    private class vlan implements PacketProcessor{
        @Override
        public void process(PacketContext context){
            if(context.isHandled())
                return;
        }    
    }

    private void begin() {
        for(Integer h1 : ConnectPoint_Table.keySet()){
            for(Integer h2 : ConnectPoint_Table.keySet()){
                if(h1.equals(h2))
                    continue;
                if(ConnectPoint_Table.get(h2).deviceId().equals(ConnectPoint_Table.get(h1).deviceId())){
                    TrafficSelector.Builder  selectorBuilder = DefaultTrafficSelector.builder();
                    selectorBuilder.matchEthSrc(Mac_Table.get(h1)).matchEthDst(Mac_Table.get(h2)).matchEthType(Ethernet.TYPE_IPV4);
                  
                    TrafficTreatment treatmentBuilder = DefaultTrafficTreatment.builder().setOutput(ConnectPoint_Table.get(h2).port()).build();

                    ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                        .withSelector(selectorBuilder.build())
                        .withTreatment(treatmentBuilder)
                        .withPriority(50000)
                        .withFlag(ForwardingObjective.Flag.VERSATILE)
                        .fromApp(appId)
                        .makeTemporary(2000)
                        .add();
                    flowObjectiveService.forward(ConnectPoint_Table.get(h1).deviceId(),forwardingObjective);
                }
            }
        }

        log.info("***********************************************************");
        for(Integer i : ConnectPoint_Table.keySet()){
            VlanId v = VlanId.vlanId(VlanID_Table.get(ConnectPoint_Table.get(i).deviceId()).shortValue());

            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthDst(Mac_Table.get(i)).matchEthType(Ethernet.TYPE_IPV4).matchVlanId(v);
          
            TrafficTreatment treatmentBuilder = DefaultTrafficTreatment.builder().popVlan().setOutput(ConnectPoint_Table.get(i).port()).build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder)
                .withPriority(50000)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(2000)
                .add();
            flowObjectiveService.forward(ConnectPoint_Table.get(i).deviceId(),forwardingObjective);

            //repeat(i, ConnectPoint_Table.get(i).port(), ConnectPoint_Table.get(i).deviceId());           
        }



        for(IpPrefix s1 : IPSubnet_Table.keySet()){
            log.info("3333333333333333333333333333333");
            DeviceId edgeSrcID = IPSubnet_Table.get(s1);
            //List<Device> SrcDevices = Lists.newArrayList(deviceService.getDevices());
            
            VlanId v = VlanId.vlanId(VlanID_Table.get(edgeSrcID).shortValue());

            // on the way to forward the vlanID
            for(DeviceId d : VlanID_Table.keySet()){
                if(d.equals(edgeSrcID))
                    continue;

                Set<Path> paths;
                paths = topologyService.getPaths(topologyService.currentTopology(), d, edgeSrcID);
                for(Path p : paths){
                    TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                    selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchVlanId(v);
                  
                    TrafficTreatment treatmentBuilder = DefaultTrafficTreatment.builder().setOutput(p.src().port()).build();

                    ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                        .withSelector(selectorBuilder.build())
                        .withTreatment(treatmentBuilder)
                        .withPriority(50000)
                        .withFlag(ForwardingObjective.Flag.VERSATILE)
                        .fromApp(appId)
                        .makeTemporary(2000)
                        .add();
                    flowObjectiveService.forward(d,forwardingObjective);
                    //paths.clear();
                    break;
                }
            }

            // begin to send the tagged packet with vlanID
            for(IpPrefix s2 : IPSubnet_Table.keySet()){
                log.info("1111111111111111111111111111");
                if(s1.equals(s2))
                    continue;
                log.info("22222222222222222222222222222");

                Set<Path> paths;
                paths = topologyService.getPaths(topologyService.currentTopology(), IPSubnet_Table.get(s2), edgeSrcID);
                for(Path p : paths){
                    TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                    selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(s1);
                  
                    TrafficTreatment treatmentBuilder = DefaultTrafficTreatment.builder().pushVlan().setVlanId(v).setOutput(p.src().port()).build();

                    ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                        .withSelector(selectorBuilder.build())
                        .withTreatment(treatmentBuilder)
                        .withPriority(50000)
                        .withFlag(ForwardingObjective.Flag.VERSATILE)
                        .fromApp(appId)
                        .makeTemporary(2000)
                        .add();
                    flowObjectiveService.forward(IPSubnet_Table.get(s2),forwardingObjective);
                    //paths.clear();
                    break;
                }
            }

        }
    }

    /*private void repeat(Integer i, PortNumber port, DeviceId dID){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthDst(Mac_Table.get(i)).matchEthType(Ethernet.TYPE_IPV4);
      
        TrafficTreatment treatmentBuilder = DefaultTrafficTreatment.builder().setOutput(port).build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
            .withSelector(selectorBuilder.build())
            .withTreatment(treatmentBuilder)
            .withPriority(40000)
            .withFlag(ForwardingObjective.Flag.VERSATILE)
            .fromApp(appId)
            .makeTemporary(2000)
            .add();
        flowObjectiveService.forward(dID,forwardingObjective);
    }*/
}

   
