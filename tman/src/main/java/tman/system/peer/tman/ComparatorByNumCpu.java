package tman.system.peer.tman;

import java.util.Comparator;

import cyclon.system.peer.cyclon.PeerDescriptor;

public class ComparatorByNumCpu implements Comparator<PeerDescriptor>{

	private PeerDescriptor self;
	
	public ComparatorByNumCpu(PeerDescriptor self) {
		this.self = self;
	}
	
	@Override
	public int compare(PeerDescriptor peer1, PeerDescriptor peer2) {
		
		int peer1Cpus = peer1.getAv().getNumFreeCpus();
		int peer2Cpus = peer2.getAv().getNumFreeCpus();
//		int myCpus = self.getAv().getNumFreeCpus();
//		
//		if( peer1Cpus < myCpus && peer2Cpus >= myCpus ) {
//			return 1;
//		}
//		else if( peer1Cpus >= myCpus && peer2Cpus <  myCpus){
//			return -1;
//		}
//		else if( Math.abs(peer1Cpus - myCpus ) < Math.abs(peer2Cpus - myCpus) ) {
//			return -1;
//		}
//		else if( Math.abs(peer1Cpus - myCpus ) > Math.abs(peer2Cpus - myCpus) ) {
//			return 1;
//		}
		if (peer1Cpus < peer2Cpus) {
			return 1;
		}else if (peer2Cpus < peer1Cpus) {
			return -1;
		}
		return 0;
	}
}
