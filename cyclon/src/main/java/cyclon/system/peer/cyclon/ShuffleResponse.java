package cyclon.system.peer.cyclon;

import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class ShuffleResponse extends Message {
	private static final long serialVersionUID = -5022051054665787770L;
	private final UUID requestId;
	private final DescriptorBuffer randomBuffer;


	public ShuffleResponse(UUID requestId, DescriptorBuffer randomBuffer, Address source, Address destination) {
		super(source, destination);
		this.requestId = requestId;
		this.randomBuffer = randomBuffer;
	}


	public UUID getRequestId() {
		return requestId;
	}


	public DescriptorBuffer getRandomBuffer() {
		return randomBuffer;
	}
	

	public int getSize() {
		return 0;
	}
}
