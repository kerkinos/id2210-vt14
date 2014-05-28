package resourcemanager.system.peer.rm;

import java.util.Comparator;

public class ComparatorQueueSize implements Comparator<Response> {

	@Override
	public int compare(Response arg0, Response arg1) {
		if (arg0.getQueueSize() > arg1.getQueueSize()) {
			return 1;
		}
		else if (arg0.getQueueSize() < arg1.getQueueSize()) {
			return -1;
		}
		return 0;
	}

}
