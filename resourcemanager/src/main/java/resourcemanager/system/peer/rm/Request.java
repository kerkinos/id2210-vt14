package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Request extends Message{
	 /**
	 * 
	 */
	 private static final long serialVersionUID = -1719815843936782731L;
	 private final int numCpus;
	 private final int amountMemInMb;
	 private final long reqid;
     private final long startTime;

     public Request(Address source, Address destination, int numCpus, int amountMemInMb, long reqid, long startTime) {
         super(source, destination);
         this.numCpus = numCpus;
         this.amountMemInMb = amountMemInMb;
         this.reqid = reqid;
         this.startTime = startTime;
     }

     public int getAmountMemInMb() {
         return amountMemInMb;
     }

     public int getNumCpus() {
         return numCpus;
     }
     
     public long getReqid() {
     	return reqid;
     }

	public long getStartTime() {
		return startTime;
	}
}
