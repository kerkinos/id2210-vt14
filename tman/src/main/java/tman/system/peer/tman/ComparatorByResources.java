package tman.system.peer.tman;

import java.util.Comparator;

import cyclon.system.peer.cyclon.PeerDescriptor;

/**
 * at first we ranked the peers according to this equation from the paper "Converging an Overlay Network to a Gradient Topology"
 * U(a) ≥ U(i) > U(b) or 
 * |U(a) − U(i)| < |U(b) − U(i)|
 * But then we saw that we get better results if we take account the queue size of each peer and compare with that.
 * Furthermore, we don't take into account our self utility now.
 */
public class ComparatorByResources implements Comparator<PeerDescriptor> {
	

	private PeerDescriptor self;

	public ComparatorByResources(PeerDescriptor self) {
		this.self = self;
	}

	@Override
	public int compare(PeerDescriptor peer1, PeerDescriptor peer2) {
		
		
		int peer1Res = peer1.getAv().getFreeMemInMbs() * peer1.getAv().getNumFreeCpus();
		int peer2Res = peer2.getAv().getFreeMemInMbs() * peer2.getAv().getNumFreeCpus();
//		int myRes = self.getAv().getFreeMemInMbs() * self.getAv().getNumFreeCpus();
//
//		if (peer1Res < myRes && peer2Res > myRes) {
//			return 1;
//		} else if (peer1Res > myRes && peer2Res < myRes) {
//			return -1;
//		} else if (Math.abs(peer1Res - myRes) < Math.abs(peer2Res - myRes)) {
//			return -1;
//		} else if (Math.abs(peer1Res - myRes) > Math.abs(peer2Res - myRes)) {
//			return 1;
//		}
		
		if (peer1.getAv().getQueueSize() == 0 && peer2.getAv().getQueueSize() == 0) {
			if (peer1Res < peer2Res) {
				return 1;
			}
			else if (peer2Res < peer1Res) {
				return -1;
			}
		}
		else {
			if(peer1.getAv().getQueueSize() > peer2.getAv().getQueueSize()) {
				return 1;
			}
			else if (peer2.getAv().getQueueSize() > peer1.getAv().getQueueSize()) {
				return -1;
			}
		}
		return 0;
	}

}
