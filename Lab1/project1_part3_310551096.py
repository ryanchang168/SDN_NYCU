from mininet.topo import Topo

class Project1_Topo_310551096(Topo):
        def __init__(self):
		Topo.__init__(self)
		h1 = self.addHost('h1', ip='192.168.0.1/27')
		h2 = self.addHost('h2', ip='192.168.0.2/27')
		h3 = self.addHost('h3', ip='192.168.0.3/27')

		s1 = self.addSwitch('s1')
		s2 = self.addSwitch('s2')
		s3 = self.addSwitch('s3')
		s4 = self.addSwitch('s4')

		self.addLink(h1, s1)
		self.addLink(h2, s2)
		self.addLink(h3, s3)
		self.addLink(s4, s1)
		self.addLink(s4, s2)
		self.addLink(s4, s3)

topos = {'topo_part3_310551096': Project1_Topo_310551096}
