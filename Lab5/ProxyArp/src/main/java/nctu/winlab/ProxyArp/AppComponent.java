package nctu.winlab.ProxyArp;

import java.nio.ByteBuffer;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.ARP;
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
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.edge.EdgePortService;
import java.util.*; 
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Dictionary;
import java.util.Properties;
import java.util.Map;
import java.util.Optional;
import static org.onlab.util.Tools.get;

/** Sample Network Configuration Service Application */
@Component(immediate = true)
public class AppComponent{

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EdgePortService edgePortService;

    private ProxyArp processor = new ProxyArp();
    protected Map<Ip4Address,MacAddress> ARP_Table = Maps.newConcurrentMap();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId App_ID;

    
    @Activate
    protected void activate() {
        App_ID = coreService.registerApplication("nctu.winlab.ProxyArp");
        packetService.addProcessor(processor, PacketProcessor.director(2));
        packetService.requestPackets(DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, App_ID, Optional.empty());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    private class ProxyArp implements PacketProcessor{
        //@Override
        public void process(PacketContext pc){
            if(pc.isHandled())
                return;
            
            if(pc.inPacket().parsed().getEtherType() == Ethernet.TYPE_ARP){
                ConnectPoint cp = pc.inPacket().receivedFrom();
                Ethernet ethPkt = pc.inPacket().parsed();
                if(ethPkt == null)
                    return;

                ARP arpPkt = (ARP)ethPkt.getPayload();
                MacAddress Src_MAC = ethPkt.getSourceMAC();
                Ip4Address Src_IP = Ip4Address.valueOf(arpPkt.getSenderProtocolAddress());
                Ip4Address Dst_IP = Ip4Address.valueOf(arpPkt.getTargetProtocolAddress());
                ARP_Table.put(Src_IP, Src_MAC);

                if(arpPkt.getOpCode() == ARP.OP_REQUEST){
                    if(ARP_Table.get(Dst_IP) == null){
                        log.info("TABLE MISS. Send request to edge ports");
                        flood_packets(ethPkt, cp);
                    }
                    else{
                        String msg = "TABLE HIT. Requested MAC = " + ARP_Table.get(Dst_IP).toString();
                        log.info(msg);
                        Ethernet reply = ARP.buildArpReply(Dst_IP, ARP_Table.get(Dst_IP), ethPkt);

                        if(reply != null){
                            TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                            builder.setOutput(cp.port());
                            packetService.emit(new DefaultOutboundPacket(cp.deviceId(),
                                builder.build(),ByteBuffer.wrap(reply.serialize())));
                        }
                    }
                }
                else if(arpPkt.getOpCode() == ARP.OP_REPLY){
                    String msg = "RECV REPLY. Request MAC = " + Src_MAC.toString();
                    log.info(msg);

                    Set<Host> Dst_host = hostService.getHostsByMac(MacAddress.valueOf(arpPkt.getTargetHardwareAddress()));
                    TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                    Host h = Dst_host.iterator().next();
                    builder.setOutput(h.location().port());
                    packetService.emit(new DefaultOutboundPacket(h.location().deviceId(),
                        builder.build(),ByteBuffer.wrap(ethPkt.serialize())));
                }
                
            }
        }

        private void flood_packets(Ethernet ethPkt, ConnectPoint cp){
            
            for(ConnectPoint p : edgePortService.getEdgePoints()){
                if(!p.equals(cp)){
                    TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                    builder.setOutput(p.port());
                    packetService.emit(new DefaultOutboundPacket(p.deviceId(),
                        builder.build(),ByteBuffer.wrap(ethPkt.serialize())));
                }
            }
        }
    }
}