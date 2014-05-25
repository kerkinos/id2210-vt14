package tman.system.peer.tman;

import java.util.Comparator;

import cyclon.system.peer.cyclon.PeerDescriptor;

public class ComparatorByResources implements Comparator<PeerDescriptor> {
	

	private PeerDescriptor self;

	public ComparatorByResources(PeerDescriptor self) {
		this.self = self;
	}

	@Override
	public int compare(PeerDescriptor peer1, PeerDescriptor peer2) {
		
		
		int peer1Res = peer1.getAv().getFreeMemInMbs() * peer1.getAv().getNumFreeCpus();
		int peer2Res = peer2.getAv().getFreeMemInMbs() * peer2.getAv().getNumFreeCpus();
		int myRes = self.getAv().getFreeMemInMbs() * self.getAv().getNumFreeCpus();

		if (peer1Res < myRes && peer2Res > myRes) {
			return 1;
		} else if (peer1Res > myRes && peer2Res < myRes) {
			return -1;
		} else if (Math.abs(peer1Res - myRes) < Math.abs(peer2Res - myRes)) {
			return -1;
		} else if (Math.abs(peer1Res - myRes) > Math.abs(peer2Res - myRes)) {
			return 1;
		}
		return 0;
	}

}
