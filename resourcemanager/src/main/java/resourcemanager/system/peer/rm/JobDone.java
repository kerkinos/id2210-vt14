package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class JobDone extends Timeout{

	private int numCpus;
	private int amountMem;
	
	protected JobDone(ScheduleTimeout request, int numCpus, int amountMem) {
		super(request);
		this.numCpus = numCpus;
		this.amountMem = amountMem;
	}

	public int getNumCpus() {
		return numCpus;
	}

	public int getAmountMem() {
		return amountMem;
	}
	
	

}
