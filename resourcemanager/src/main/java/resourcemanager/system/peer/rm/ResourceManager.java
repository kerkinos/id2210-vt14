package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.JobDone;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 * 
 */
public final class ResourceManager extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceManager.class);
	Positive<RmPort> indexPort = requires(RmPort.class);
	Positive<Network> networkPort = requires(Network.class);
	Positive<Timer> timerPort = requires(Timer.class);
	Negative<Web> webPort = provides(Web.class);
	Positive<CyclonSamplePort> cyclonSamplePort = requires(CyclonSamplePort.class);
	Positive<TManSamplePort> tmanPort = requires(TManSamplePort.class);
	ArrayList<PeerDescriptor> cyclonPartners = new ArrayList<PeerDescriptor>();
	ArrayList<PeerDescriptor> tmanPartnersByRes = new ArrayList<PeerDescriptor>();
	ArrayList<PeerDescriptor> tmanPartnersByCpu = new ArrayList<PeerDescriptor>();
	ArrayList<PeerDescriptor> tmanPartnersByMem = new ArrayList<PeerDescriptor>();
	
	private Address self;
	private RmConfiguration configuration;
	private Random random;
	private AvailableResources availableResources;
	private static final int MAX_NUM_NODES = 8;
	
	private long startTime, endTime, averageTime;

	// queue where we put incoming requests for resources

	private Queue<Allocate> queue = new LinkedList<Allocate>();
	private Map<Long, RequestResources> requestResourcesMap = new HashMap<Long, RequestResources>();
	private static Map<Long, Long> timePerRequest = new HashMap<Long, Long>();

	Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
		@Override
		public int compare(PeerDescriptor t, PeerDescriptor t1) {
			if (t.getAge() > t1.getAge()) {
				return 1;
			} else {
				return -1;
			}
		}
	};

	public ResourceManager() {

		subscribe(handleInit, control);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleRequestResource, indexPort);
		subscribe(handleUpdateTimeout, timerPort);
		subscribe(handleJobDone, timerPort);
		subscribe(handleResourceAllocationRequest, networkPort);
		subscribe(handleResourceAllocationResponse, networkPort);
		subscribe(handleAllocateResources, networkPort);
//		subscribe(handleTManSample, tmanPort);
	}

	Handler<RmInit> handleInit = new Handler<RmInit>() {
		@Override
		public void handle(RmInit init) {
			self = init.getSelf();
			configuration = init.getConfiguration();
			random = new Random(init.getConfiguration().getSeed());
			availableResources = init.getAvailableResources();
			long period = configuration.getPeriod();
			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new UpdateTimeout(rst));

			trigger(rst, timerPort);

		}
	};

	Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
		@Override
		public void handle(UpdateTimeout event) {

			// pick a random neighbour to ask for index updates from.
			// You can change this policy if you want to.
			// Maybe a gradient neighbour who is closer to the leader?
			
			System.out.println(averageTime);

			if (cyclonPartners.isEmpty()) {
				return;
			}
			PeerDescriptor dest = cyclonPartners.get(random
					.nextInt(cyclonPartners.size()));

		}
	};

	Handler<Request> handleResourceAllocationRequest = new Handler<Request>() {
		@Override
		public void handle(Request event) {
			// System.out.println(self.getId() + " : Received Request for " +
			// event.getNumCpus() + " cpus and " + event.getAmountMemInMb()
			// + " memInMb" + " from peer with id " +
			// event.getSource().getId());
			// System.out.println("I have " +
			// availableResources.getNumFreeCpus() + " cpus and " +
			// availableResources.getFreeMemInMbs());

			// send a response event with a boolean value
			boolean success = availableResources.isAvailable(
					event.getNumCpus(), event.getAmountMemInMb());
			Response response = new Response(self, event.getSource(), success,
					queue.size(), event.getReqid());
			trigger(response, networkPort);

		}
	};

	Handler<Response> handleResourceAllocationResponse = new Handler<Response>() {
		@Override
		public void handle(Response event) {
			// System.out.println(self + " Got response from " +
			// event.getSource().getId());

			RequestResources rr = requestResourcesMap.get(event.getReqid());
			Response best = rr.findBestResponse(event);
			if (--rr.pendingResponses == 0) {
				endTime = System.currentTimeMillis();
				timePerRequest.put(event.getReqid(), (endTime - startTime));
				averageTime = getAverageTime();	
				Allocate al = new Allocate(self, best.getSource(),
						rr.getNumCpus(), rr.getAmountMem(), rr.getTime());
				trigger(al, networkPort);
			}

		}
	};
	
	public static long getAverageTime() {
		long sum = 0;
		for(Long l : timePerRequest.values()) {
			sum += l;
		}
		if(timePerRequest.size()==0){
			return 0;
		}else
		return sum / timePerRequest.size();
	}

	Handler<Allocate> handleAllocateResources = new Handler<Allocate>() {

		@Override
		public void handle(Allocate arg0) {
			boolean success = availableResources.isAvailable(arg0.getNumCpus(),
					arg0.getAmountMem());
			if (!success) {
				queue.add(arg0);
			} else {
				availableResources.allocate(arg0.getNumCpus(),
						arg0.getAmountMem());
				ScheduleTimeout st = new ScheduleTimeout(arg0.getTime());
				st.setTimeoutEvent(new JobDone(st, arg0.getNumCpus(), arg0
						.getAmountMem()));
				trigger(st, timerPort);
			}

		}
	};

	Handler<JobDone> handleJobDone = new Handler<JobDone>() {

		@Override
		public void handle(JobDone arg0) {
			availableResources.release(arg0.getNumCpus(), arg0.getAmountMem());

			if (!queue.isEmpty()) {

				Allocate al = queue.poll();

				boolean success = availableResources.isAvailable(
						al.getNumCpus(), al.getAmountMem());
				if (success) {
					availableResources.allocate(al.getNumCpus(),
							al.getAmountMem());
					ScheduleTimeout st = new ScheduleTimeout(al.getTime());
					st.setTimeoutEvent(new JobDone(st, al.getNumCpus(), al
							.getAmountMem()));
					trigger(st, timerPort);
				}
			}
		}
	};

	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			// System.out.println("id " + self.getId() + " received samples: " +
			// event.getSample().size());

			// receive a new list of cyclonPartners
			cyclonPartners.clear();
			cyclonPartners.addAll(event.getSample());

//			if (event.getSample().size() > 0) {
//				System.out.print("My neigbours are [ ");
//				for (PeerDescriptor peer : cyclonPartners) {
//					System.out.print(peer.getAddress().getId() + " ");
//					System.out.print("In ResourceManager " + peer.getAv().getNumFreeCpus() + " ");
//
//				}
//				System.out.print("]\n\n");
//			}

		}
	};

	Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
		@Override
		public void handle(RequestResource event) {

			// System.out.println("Allocate resources: " + event.getId() + " " +
			// event.getNumCpus() + " + " + event.getMemoryInMbs());
			// TODO: Ask for resources from cyclonPartners
			// by sending a ResourceRequest
			startTime = System.currentTimeMillis();

			System.out.println(self.getId() + " Request resource id: "
					+ event.getId());

//			if (tmanPartnersByRes.size() <= MAX_NUM_NODES) {
//				requestResourcesMap
//						.put(event.getId(),
//								new RequestResources(event.getNumCpus(), event
//										.getMemoryInMbs(), event
//										.getTimeToHoldResource(),
//										tmanPartnersByRes.size()));
//				for (PeerDescriptor dest : tmanPartnersByRes) {
//					Request req = new Request(self, dest.getAddress(),
//							event.getNumCpus(), event.getMemoryInMbs(),
//							event.getId());
//					trigger(req, networkPort);
//				}
//			} else {
//				requestResourcesMap.put(
//						event.getId(),
//						new RequestResources(event.getNumCpus(), event
//								.getMemoryInMbs(), event
//								.getTimeToHoldResource(), MAX_NUM_NODES));
//				for (int i = 0; i < MAX_NUM_NODES; i++) {
//					int index = random.nextInt(tmanPartnersByRes.size());
//					PeerDescriptor dest = tmanPartnersByRes.get(index);
//					tmanPartnersByRes.remove(index);
//					Request req = new Request(self, dest.getAddress(),
//							event.getNumCpus(), event.getMemoryInMbs(),
//							event.getId());
//					trigger(req, networkPort);
//				}
			if (cyclonPartners.size() <= MAX_NUM_NODES) {
				requestResourcesMap
						.put(event.getId(),
								new RequestResources(event.getNumCpus(), event
										.getMemoryInMbs(), event
										.getTimeToHoldResource(),
										cyclonPartners.size()));
				for (PeerDescriptor dest : cyclonPartners) {
					Request req = new Request(self, dest.getAddress(),
							event.getNumCpus(), event.getMemoryInMbs(),
							event.getId());
					trigger(req, networkPort);
				}
			} else {
				requestResourcesMap.put(
						event.getId(),
						new RequestResources(event.getNumCpus(), event
								.getMemoryInMbs(), event
								.getTimeToHoldResource(), MAX_NUM_NODES));
				for (int i = 0; i < MAX_NUM_NODES; i++) {
					int index = random.nextInt(cyclonPartners.size());
					PeerDescriptor dest = cyclonPartners.get(index);
					cyclonPartners.remove(index);
					Request req = new Request(self, dest.getAddress(),
							event.getNumCpus(), event.getMemoryInMbs(),
							event.getId());
					trigger(req, networkPort);
				}

			}
		}
	};
	
	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
			// TODO:
			tmanPartnersByRes.clear();
			tmanPartnersByCpu.clear();
			tmanPartnersByMem.clear();
			tmanPartnersByRes.addAll(event.getPartnersByRes());
			tmanPartnersByCpu.addAll(event.getPartnersByRes());
			tmanPartnersByMem.addAll(event.getPartnersByRes());
		}
	};

}