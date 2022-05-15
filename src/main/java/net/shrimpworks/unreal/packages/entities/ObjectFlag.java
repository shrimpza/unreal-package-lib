package net.shrimpworks.unreal.packages.entities;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ObjectFlag {
	Transactional(0x0000000100000000L),
	Unreachable(0x0000000200000000L),
	Public(0x0000000400000000L),
	TagImp(0x0000000800000000L),
	TagExp(0x0000001000000000L),
	SourceModified(0x0000002000000000L),
	TagGarbage(0x0000004000000000L),
	NeedLoad(0x0000020000000000L),
	HighlightedName(0x0000040000000000L),
	EliminateObject(0x0000040000000000L),
	InSingularFunc(0x0000080000000000L),
	RemappedName(0x0000080000000000L),
	Suppress(0x0000100000000000L),
	StateChanged(0x0000100000000000L),
	InEndState(0x0000200000000000L),
	Transient(0x0000400000000000L),
	PreLoading(0x0000800000000000L),
	LoadForClient(0x0001000000000000L),
	LoadForServer(0x0002000000000000L),
	LoadForEdit(0x0004000000000000L),
	Standalone(0x0008000000000000L),
	NotForClient(0x0010000000000000L),
	NotForServer(0x0020000000000000L),
	NotForEdit(0x0040000000000000L),
	Destroyed(0x0080000000000000L),
	NeedPostLoad(0x0100000000000000L),
	HasStack(0x0200000000000000L),
	Native(0x0400000000000000L),
	Marked(0x0800000000000000L),
	ErrorShutdown(0x1000000000000000L),
	DebugPostLoad(0x2000000000000000L),
	DebugSerialize(0x4000000000000000L),
	DebugDestroy(0x8000000000000000L);

	private final long flag;

	ObjectFlag(long flag) {
		this.flag = flag;
	}

	public static Set<ObjectFlag> fromFlags(long flags) {
		Set<ObjectFlag> objectFlags = EnumSet.noneOf(ObjectFlag.class);
		// support for both 32-bit (UE 1/2 - high) and 64bit (UE3 - low) flags
		objectFlags.addAll(Arrays.stream(values()).filter(f -> (flags & f.flag) == f.flag).collect(Collectors.toSet()));
		objectFlags.addAll(Arrays.stream(values()).filter(f -> (flags << 32 & f.flag) == f.flag).collect(Collectors.toSet()));
		return objectFlags;
	}
}
