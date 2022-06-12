/*
 * Copyright 2020-present Open Networking Foundation
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
package nctu.winlab.unicastdhcp;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.Ethernet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
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
import org.onosproject.net.topology.PathService;
import java.util.Set;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sample Network Configuration Service Application */
@Component(immediate = true)
public class AppComponent {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final NameConfigListener cfgListener = new NameConfigListener();
  private final ConfigFactory factory =
      new ConfigFactory<ApplicationId, NameConfig>(
          APP_SUBJECT_FACTORY, NameConfig.class, "whoami") {
        @Override
        public NameConfig createConfig() {
          return new NameConfig();
        }
      };

  private ApplicationId appId;
  private DHCP processor = new DHCP();

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  protected NetworkConfigRegistry cfgService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  protected CoreService coreService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;


  @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PathService pathService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

  private static ConnectPoint server_name = null;

  @Activate
  protected void activate() {
    appId = coreService.registerApplication("nctu.winlab.unicastdhcp");
    cfgService.addListener(cfgListener);
    cfgService.registerConfigFactory(factory);
    packetService.addProcessor(processor,PacketProcessor.director(2));
    
    TrafficSelector.Builder selector =  DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_IPV4)
        .matchIPProtocol(IPv4.PROTOCOL_UDP)
        .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
        .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
    packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);

    selector =  DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_IPV4)
        .matchIPProtocol(IPv4.PROTOCOL_UDP)
        .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
        .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
    packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);

    log.info("Started");
  }

  @Deactivate
  protected void deactivate() {
    cfgService.removeListener(cfgListener);
    cfgService.unregisterConfigFactory(factory);
    packetService.removeProcessor(processor);
    processor = null;

    TrafficSelector.Builder selector =  DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_IPV4)
        .matchIPProtocol(IPv4.PROTOCOL_UDP)
        .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
        .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
    packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);

    selector =  DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_IPV4)
        .matchIPProtocol(IPv4.PROTOCOL_UDP)
        .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
        .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
    packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
    log.info("Stopped");
  }

  private class NameConfigListener implements NetworkConfigListener {
    @Override
    public void event(NetworkConfigEvent event) {
      if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)
          && event.configClass().equals(NameConfig.class)) {
        NameConfig config = cfgService.getConfig(appId, NameConfig.class);
        if (config != null) {
          log.info("DHCP server is at {}", config.name());
          server_name = ConnectPoint.deviceConnectPoint(config.name());
        }
      }
    }
  }

  private class DHCP implements PacketProcessor{
    @Override
    public void process(PacketContext context){
        if(context.isHandled())
                return;
            
        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if(ethPkt == null)
            return;

        Set<Path> paths;
        Host h = hostService.getHost(HostId.hostId(ethPkt.getDestinationMAC()));
        if(ethPkt.isBroadcast()){
            if(pkt.receivedFrom().deviceId().equals(server_name.deviceId())){
                installRule(context,server_name.port());
                return;
            }
            else
               paths = pathService.getPaths(pkt.receivedFrom().deviceId(), server_name.deviceId()); 
        }
        else{
            if(pkt.receivedFrom().deviceId().equals(h.location().deviceId())){
                if(!context.inPacket().receivedFrom().port().equals(h.location().port()))
                    installRule(context, h.location().port());
                return;
            }
            else
               paths = pathService.getPaths(pkt.receivedFrom().deviceId(), h.location().deviceId()); 
        }

        Path p = null;
        for(Path path : paths){
            if(!path.src().port().equals(pkt.receivedFrom().port())){
                p = path;
                break;
            }
        }
        installRule(context, p.src().port());
    }
  }

  private void installRule(PacketContext context, PortNumber port_number){
    //use to create matching field
    TrafficSelector.Builder  selector = DefaultTrafficSelector.builder();
    TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(port_number).build();

    InboundPacket pkt = context.inPacket();
    Ethernet ethPkt = pkt.parsed();

    if(ethPkt.isBroadcast()){
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
    }
    else{
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(ethPkt.getDestinationMAC())
                .matchEthSrc(ethPkt.getSourceMAC());
    }
    
    ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                                            .withSelector(selector.build())
                                            .withTreatment(treatment)
                                            .withPriority(50000)
                                            .withFlag(ForwardingObjective.Flag.VERSATILE)
                                            .fromApp(appId)
                                            .makeTemporary(30)
					    .add();

    flowObjectiveService.forward(pkt.receivedFrom().deviceId(),forwardingObjective);
    context.treatmentBuilder().setOutput(port_number);
    context.send();
  }
}

