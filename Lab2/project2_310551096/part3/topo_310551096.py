from mininet.topo import Topo

class MyTopo(Topo):
        def __init__(self):
		Topo.__init__(self)
		h1 = self.addHost('h1')
		h2 = self.addHost('h2')

		s1 = self.addSwitch('s1')
		s2 = self.addSwitch('s2')
		s3 = self.addSwitch('s3')

		self.addLink(h1, s1)
		self.addLink(h2, s2)
		self.addLink(s1, s2)
		self.addLink(s3, s2)
		self.addLink(s1, s3)

topos = {'mytopo': MyTopo}
