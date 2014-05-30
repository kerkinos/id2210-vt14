package resourcemanager.system.peer.rm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import tman.system.peer.tman.ComparatorByResources;


public class RequestResources {

	private int numCpus;
	private int amountMem;
	private int time;
	int pendingResponses;
	Response bestResponse = null;
	ArrayList<Response> responses = new ArrayList<Response>();

	public RequestResources(int numCpus, int amountMem, int time, int pendingResponses) {
		this.numCpus = numCpus;
		this.amountMem = amountMem;
		this.time = time;
		this.pendingResponses = pendingResponses;
	}

	public int getNumCpus() {
		return numCpus;
	}

	public int getAmountMem() {
		return amountMem;
	}

	public int getTime() {
		return time;
	}

	public ArrayList<Response> getResponses() {
		return responses;
	}

	public Response findBestResponse(Response res) {
		if (bestResponse == null) {
			bestResponse = res;
		} else if (!bestResponse.getSuccess() && res.getSuccess()) {
			bestResponse = res;
		} else if (res.getQueueSize() < bestResponse.getQueueSize()) {
			bestResponse = res;
		}

		return bestResponse;
	}
	
	public void collectResponses(Response res) {
		responses.add(res);
	}
	
	public void sortResponses() {
		Collections.sort(responses, new ComparatorQueueSize());
	}

	public static class RequestTimeout extends Timeout {
		private final Address destination;

		RequestTimeout(ScheduleTimeout st, Address destination) {
			super(st);
			this.destination = destination;
		}

		public Address getDestination() {
			return destination;
		}
	}
}
