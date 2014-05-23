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
