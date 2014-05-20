package resourcemanager.system.peer.rm;

import java.util.List;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * User: jdowling
 */
public class RequestResources  {
	
	private int numCpus;
	private int amountMem;
	private int time;
	int pendingResponses;
	Response bestResponse = null;
	
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



	public static class Request extends Message {

        private final int numCpus;
        private final int amountMemInMb;
        private final long reqid;

        public Request(Address source, Address destination, int numCpus, int amountMemInMb, long reqid) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.reqid = reqid;
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
        

    }
    
    public static class Response extends Message {

        private final boolean success;
        private final long reqid;
        private final int queueSize;
        
        public Response(Address source, Address destination, boolean success, int queueSize, long reqid) {
            super(source, destination);
            this.success = success;
            this.reqid = reqid;
            this.queueSize = queueSize;
        }
        
        public long getReqid() {
        	return reqid;
        }

		public int getQueueSize() {
			return queueSize;
		}
		
		public boolean getSuccess() {
			return success;
		}
    }
    
    public Response findBestResponse(Response res) {
    	if(bestResponse == null) {
    		bestResponse = res;
    	}
    	else if(!bestResponse.getSuccess() && res.getSuccess()) {
    		bestResponse = res;
    	}
    	else if(res.getQueueSize() < bestResponse.getQueueSize()) {
    		bestResponse = res;
    	}
    	
    	return bestResponse;
    }
    
    public static class Allocate extends Message {
    	private final int numCpus;
    	private final int amountMem;
    	private final int time;
    	
    	public Allocate(Address source, Address destination, int numCpus, int amountMem, int time) {
    		super(source, destination);
    		this.numCpus = numCpus;
    		this.amountMem = amountMem;
    		this.time = time;
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
