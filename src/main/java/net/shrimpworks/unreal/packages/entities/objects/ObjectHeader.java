package net.shrimpworks.unreal.packages.entities.objects;

public class ObjectHeader {

	private final int node;
	private final int stateNode;
	private final long probeMask;
	private final int latentAction;
	private final int offset;

	public ObjectHeader(int node, int stateNode, long probeMask, int latentAction, int offset) {
		this.node = node;
		this.stateNode = stateNode;
		this.probeMask = probeMask;
		this.latentAction = latentAction;
		this.offset = offset;
	}

	@Override
	public String toString() {
		return String.format("UnrealObjectHeader [node=%s, stateNode=%s, probeMask=%s, latentAction=%s, offset=%s]",
							 node, stateNode, probeMask, latentAction, offset);
	}
}
