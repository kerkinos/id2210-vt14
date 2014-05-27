package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario2 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{
                
		StochasticProcess process0 = new StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			raise(30, Operations.peerJoin(), 
                                uniform(1, 1024), 
                                constant(8), constant(12000)
                             );
		}};
                
		StochasticProcess process1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1000, Operations.allocateResourcesManyMachines(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2000), constant(2),
                                constant(1000*60*1) // 1 minute
                                );
		}};
                
                // TODO - not used yet
		StochasticProcess failPeersProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.peerFail, 
                                uniform(0, Integer.MAX_VALUE));
		}};
                
		StochasticProcess terminateProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		process0.start();
		process1.startAfterTerminationOf(2000, process0);
                terminateProcess.startAfterTerminationOf(100*1000, process1);
                
	}};

	// -------------------------------------------------------------------
	public Scenario2() {
		super(scenario);
	}
}