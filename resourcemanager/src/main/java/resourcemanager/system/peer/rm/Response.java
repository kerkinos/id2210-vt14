package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Response extends Message{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2871499945624179185L;
	private final boolean success;
    private final long reqid;
    private final int queueSize;
    private final long startTime;
    
    public Response(Address source, Address destination, boolean success, int queueSize, long reqid, long startTime) {
        super(source, destination);
        this.success = success;
        this.reqid = reqid;
        this.queueSize = queueSize;
        this.startTime = startTime;
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
	
	public long getStartTime() {
		return startTime;
	}
	
}
