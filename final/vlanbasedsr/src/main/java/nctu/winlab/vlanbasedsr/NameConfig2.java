/*package nctu.winlab.vlanbasedsr;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ConnectPoint;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IpPrefix;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;


public class NameConfig extends Config<ApplicationId> {

    public static final String VlanID = "VlanID";
    public static final String IPSubnet = "IPSubnet";
    public static final String ConnectPoint = "ConnectPoint";
    public static final String Mac = "Mac";

    private static ConnectPoint cp = null;

    @Override
    public boolean isValid(){
        return hasOnlyFields(VlanID, IPSubnet, ConnectPoint, Mac);
    }

    public Map<DeviceId, Integer> getVlanID(){
        Map<DeviceId, Integer> VlanID_Table = new HashMap();
        Iterator<String> it = node().get("VlanID").fieldNames();

        while(it.hasNext()){
            DeviceId key = DeviceId.deviceId(it.next());
            int value = Integer.valueOf(node().get("VlanID").get(it.next()).asText());
            VlanID_Table.put(key, value);
        }
        return VlanID_Table;
    }

    public Map<IpPrefix, DeviceId> getIPSubnet(){
        Map<IpPrefix, DeviceId> IPSubnet_Table = new HashMap();
        Iterator<String> it = node().get("IPSubnet").fieldNames();

        while(it.hasNext()){
            IpPrefix key = IpPrefix.valueOf(it.next());
            DeviceId value = DeviceId.deviceId(node().get("IPSubnet").get(it.next()).asText());
            IPSubnet_Table.put(key, value);
        }
        return IPSubnet_Table;
    }

    public Map<Integer, ConnectPoint> getConnectPoint(){
        Map<Integer, ConnectPoint> ConnectPoint_Table = new HashMap();
        JsonNode Node = node().get("ConnectPoint");
        Iterator<String> it = Node.fieldNames();

        while(it.hasNext()){
            String str = it.next();
            int key = Integer.valueOf(str);
            
            cp = ConnectPoint.deviceConnectPoint(Node.get(str).toString());
            ConnectPoint_Table.put(key, cp);
        }
        return ConnectPoint_Table;
    }

    public Map<Integer, MacAddress> getMac(){
        Map<Integer, MacAddress> Mac_Table = new HashMap();
        Iterator<String> it = node().get("Mac").fieldNames();

        while(it.hasNext()){
            int key = Integer.valueOf(it.next());
            MacAddress value = MacAddress.valueOf(node().get("Mac").get(it.next()).asText());
            Mac_Table.put(key, value);
        }
        return Mac_Table;
    }

}*/
