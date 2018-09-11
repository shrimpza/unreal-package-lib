package net.shrimpworks.unreal.packages.entities;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum ObjectFlag {
	Transactional(0x00000001),
	Unreachable(0x00000002),
	Public(0x00000004),
	TagImp(0x00000008),
	TagExp(0x00000010),
	SourceModified(0x00000020),
	TagGarbage(0x00000040),
	NeedLoad(0x00000200),
	HighlightedName(0x00000400),
	EliminateObject(0x00000400),
	InSingularFunc(0x00000800),
	RemappedName(0x00000800),
	Suppress(0x00001000),
	StateChanged(0x00001000),
	InEndState(0x00002000),
	Transient(0x00004000),
	PreLoading(0x00008000),
	LoadForClient(0x00010000),
	LoadForServer(0x00020000),
	LoadForEdit(0x00040000),
	Standalone(0x00080000),
	NotForClient(0x00100000),
	NotForServer(0x00200000),
	NotForEdit(0x00400000),
	Destroyed(0x00800000),
	NeedPostLoad(0x01000000),
	HasStack(0x02000000),
	Native(0x04000000),
	Marked(0x08000000),
	ErrorShutdown(0x10000000),
	DebugPostLoad(0x20000000),
	DebugSerialize(0x40000000),
	DebugDestroy(0x80000000);

	private final int flag;

	ObjectFlag(int flag) {
		this.flag = flag;
	}

	public static EnumSet<ObjectFlag> fromFlags(int flags) {
		EnumSet<ObjectFlag> objectFlags = EnumSet.noneOf(ObjectFlag.class);
		objectFlags.addAll(Arrays.stream(values()).filter(f -> (flags & f.flag) == f.flag).collect(Collectors.toSet()));
		return objectFlags;
	}
}
