package resourcemanager.system.peer.rm;

import java.util.Comparator;

import cyclon.system.peer.cyclon.PeerDescriptor;

public class ComparatorQueueSizeRM implements Comparator<PeerDescriptor> {

	@Override
	public int compare(PeerDescriptor arg0, PeerDescriptor arg1) {
		if (arg0.getAv().getQueueSize() > arg1.getAv().getQueueSize()) {
			return 1;
		}
		else if (arg0.getAv().getQueueSize() < arg1.getAv().getQueueSize()) {
			return -1;
		}
		return 0;
	}

}