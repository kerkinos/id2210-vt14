package tman.system.peer.tman;

import java.util.Comparator;

import cyclon.system.peer.cyclon.PeerDescriptor;

public class ComparatorByNumMem implements Comparator<PeerDescriptor> {

	private PeerDescriptor self;

	public ComparatorByNumMem(PeerDescriptor self) {
		this.self = self;
	}

	@Override
	public int compare(PeerDescriptor peer1, PeerDescriptor peer2) {

		int peer1Mem = peer1.getAv().getFreeMemInMbs();
		int peer2Mem = peer2.getAv().getFreeMemInMbs();
		int myMem = self.getAv().getFreeMemInMbs();

		if (peer1Mem < myMem && peer2Mem > myMem) {
			return 1;
		} else if (peer1Mem > myMem && peer2Mem < myMem) {
			return -1;
		} else if (Math.abs(peer1Mem - myMem) < Math.abs(peer2Mem - myMem)) {
			return -1;
		} else if (Math.abs(peer1Mem - myMem) > Math.abs(peer2Mem - myMem)) {
			return 1;
		}
		return 0;
	}

}
