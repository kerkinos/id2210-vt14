package common.simulation;

import se.sics.kompics.Event;

public final class AllocateResourcesManyMachines extends Event {
    
    private final long id;
    private final int numCpus;
    private final int memoryInMbs;
    private final int numMachines;
    private final int timeToHoldResource;

    public AllocateResourcesManyMachines(long id, int numCpus, int memoryInMbs, int numMachines, int timeToHoldResource) {
        this.id = id;
        this.numCpus = numCpus;
        this.memoryInMbs = memoryInMbs;
        this.timeToHoldResource = timeToHoldResource;
        this.numMachines = numMachines;
    }

    public long getId() {
        return id;
    }

    public int getTimeToHoldResource() {
        return timeToHoldResource;
    }

    public int getMemoryInMbs() {
        return memoryInMbs;
    }

    public int getNumCpus() {
        return numCpus;
    }
    
    public int getNumMachines() {
    	return numMachines;
    }
    

}
