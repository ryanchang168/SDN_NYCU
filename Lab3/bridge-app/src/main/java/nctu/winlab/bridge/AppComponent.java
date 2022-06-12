/*
 * Copyright 2021-present Open Networking Foundation
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
package nctu.winlab.bridge;

import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import com.google.common.collect.Maps;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onlab.util.Tools;
import java.util.Map;
import java.util.Optional;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent{
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService Core_Service;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService FlowRule_Service;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService Cfg_Service;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService Packet_Service;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId App_Id;
    private PacketProcessor processor;
    protected Map<DeviceId, Map<MacAddress, PortNumber>> Mac_Tables = Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        log.info("Started");
        App_Id = Core_Service.registerApplication("nctu.winlab.bridge");
        processor = new Packet_Process();

        Packet_Service.addProcessor(processor, PacketProcessor.director(3));
        Packet_Service.requestPackets(DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).build(), PacketPriority.REACTIVE, App_Id, Optional.empty());
        Packet_Service.requestPackets(DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, App_Id, Optional.empty());
    }

    @Deactivate
    protected void deactivate() {
        Packet_Service.removeProcessor(processor);
        log.info("Stopped");
    }

    /*@Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }/*

    private class SwitchPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext pc) {
            initMacTable(pc.inPacket().receivedFrom());
            actLikeSwitch(pc);

        }
    
    /** Floods packet out of all switch ports. **/

    private class Packet_Process implements PacketProcessor {
        @Override
        public void process(PacketContext pc) {
            Mac_Tables.putIfAbsent(pc.inPacket().receivedFrom().deviceId(), Maps.newConcurrentMap());
            actLikeSwitch(pc);

        }
    
        public void actLikeSwitch(PacketContext pc) {
            Short type = pc.inPacket().parsed().getEtherType();
            if (type != Ethernet.TYPE_IPV4 && type != Ethernet.TYPE_ARP) 
                return;

            Map<MacAddress, PortNumber> Mac_Table = Mac_Tables.get(pc.inPacket().receivedFrom().deviceId());
            MacAddress src = pc.inPacket().parsed().getSourceMAC();
            MacAddress dst = pc.inPacket().parsed().getDestinationMAC();

            log.info("Add MAC address ==> switch: "+pc.inPacket().receivedFrom().deviceId()+", MAC: "+src+", port: "+pc.inPacket().receivedFrom().port());
    
            Mac_Table.put(src, pc.inPacket().receivedFrom().port());
            PortNumber Out_Port = Mac_Table.get(dst);

            if (Out_Port != null) {
                log.info("MAC "+ dst+" is matched on "+pc.inPacket().receivedFrom().deviceId()+"! Install flow rule!");
                pc.treatmentBuilder().setOutput(Out_Port);
                FlowRule Flow_Rule = DefaultFlowRule.builder()
                        .withSelector(DefaultTrafficSelector.builder().matchEthDst(dst).matchEthSrc(src).build())
                        .withTreatment(DefaultTrafficTreatment.builder().setOutput(Out_Port).build())
                        .forDevice(pc.inPacket().receivedFrom().deviceId())
                        .withPriority(30)
                        .makeTemporary(30)
			.fromApp(App_Id).build();

                FlowRule_Service.applyFlowRules(Flow_Rule);
                pc.send();
            } else {
                log.info("MAC "+dst+" is missed on "+pc.inPacket().receivedFrom().deviceId()+"! Flood packet!");
                actLikeHub(pc);
            }
        }

        public void actLikeHub(PacketContext pc) {
            pc.treatmentBuilder().setOutput(PortNumber.FLOOD);
            pc.send();
        }
    }
}




