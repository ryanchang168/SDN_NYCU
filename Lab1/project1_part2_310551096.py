from mininet.topo import Topo

class Project1_Topo_310551096(Topo):
        def __init__(self):
		Topo.__init__(self)
		h1 = self.addHost('h1')
		h2 = self.addHost('h2')
		h3 = self.addHost('h3')

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

topos = {'topo_part2_310551096': Project1_Topo_310551096}
