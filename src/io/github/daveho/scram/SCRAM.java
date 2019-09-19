package io.github.daveho.scram;

import java.util.StringTokenizer;

public class SCRAM {
	private boolean halted;
	private int[] mem;
	private int pc;
	private int accum;
	
	public SCRAM() {
		halted = false;
		mem = new int[16];
		pc = 0;
		accum = 0;
	}
	
	public int getMem(int addr) {
		if (addr < 0 || addr >= 16) {
			throw new IllegalArgumentException("Bad address: " + addr);
		}
		return mem[addr];
	}
	
	public void setMem(int addr, int val) {
		if (addr < 0 || addr >= 16) {
			throw new IllegalArgumentException("Bad address: " + addr);
		}
		if (val < 0 || val >= 256) {
			throw new IllegalArgumentException("Bad value: " + val);
		}
		mem[addr] = val;
	}
	
	public int getAccum() {
		return accum;
	}
	
	public int getPc() {
		return pc;
	}
	
	public boolean isHalted() {
		return halted;
	}
	
	public void executeNextInstruction() {
		if (halted) {
			throw new IllegalStateException("SCRAM is halted");
		}
		int opcode = mem[pc];
		pc++;
		int data = opcode & 0xF;
		int code = (opcode >> 4) & 0xF;
		int tmpval;
		
		switch (code) {
		case 0: // HLT
			halted = true;
			break;
		case 1: // LDA
			accum = mem[data];
			break;
		case 2: // LDI
			tmpval = mem[data] & 0xF;
			accum = mem[tmpval];
			break;
		case 3: // STA
			mem[data] = accum;
			break;
		case 4: // STI
			tmpval = mem[data] & 0xF;
			mem[tmpval] = accum;
			break;
		case 5: // ADD
			accum += mem[data];
			accum &= 0xFF;
			break;
		case 6: // SUB
			accum -= mem[data];
			accum &= 0xFF;
			break;
		case 7: // JMP
			pc = data;
			break;
		case 8: // JMZ
			if (accum == 0) {
				pc = data;
			}
			break;
		default:
			throw new IllegalStateException("Illegal instruction code: " + code);
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java [-DmaxCycles=N] -jar SCRAM.jar <mem contents>");
			System.out.println("  Set the maxCycles property to limit number of cycles executed");
			System.exit(1);
		}
		
		int maxCycles = Integer.parseInt(System.getProperty("maxCycles", String.valueOf(Integer.MAX_VALUE)));
		
		SCRAM scram = new SCRAM();
		
		StringTokenizer st = new StringTokenizer(args[0]);
		int addr = 0;
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			int val = Integer.parseInt(tok, 16);
			scram.setMem(addr, val);
			addr++;
		}
		
		int cycle = 0;
		printState("Start", scram);
		boolean interrupted = false;
		try {
			while (!scram.isHalted() && cycle < maxCycles) {
				scram.executeNextInstruction();
				printState(String.format("%04d ", cycle), scram);
				cycle++;
			}
		} catch (Exception e) {
			System.out.printf("Runtime error: %s\n", e.getMessage());
			interrupted = true;
		}
		
		if (interrupted) {
			System.out.printf("Interrupted after %d cycles\n", cycle);
		} else if (scram.isHalted()) {
			System.out.printf("Halted after %d cycles\n", cycle);
		} else {
			System.out.printf("Reached cycle limit after %d cycles\n", cycle);
		}
	}

	private static void printState(String label, SCRAM scram) {
		System.out.printf("%s:", label);
		for (int i = 0; i < 16; i++) {
			System.out.printf(" %02x", scram.getMem(i));
		}
		System.out.printf(" A=%02x PC=%x\n", scram.getAccum(), scram.getPc());
	}
}
