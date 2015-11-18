package parser;

import java.util.Arrays;

public class ErrorHandler {
	public static void scannnerError(Exception e) {
		System.err.println("P�i skenov�n� vznikla chyba.");
		e.printStackTrace();
		System.exit(1);
	}
	
	public static void parserError(int token) {
		System.err.println("P�i parsov�n� vznikla chyba.");
		System.err.println("O�ek�v�no token ID " + token);
		System.exit(1);
	}
	
	public static void parserError(int[] tokens) {
		System.err.println("P�i parsov�n� vznikla chyba.");
		System.err.println("O�ek�v�no token ID " + Arrays.toString(tokens));
		System.exit(1);
	}

	public static void constAssign() {
		System.err.println("P�i�azujete do konstanty.");
		System.exit(1);
	}
	
	public static void dupliciteVariable(Variable var) {
		System.err.println("Hodnota s nazvem " + var.getName() + " jiz existuje.");
		System.exit(1);
	}
	
	public static void dupliciteMethod(BlockNode child) {
		System.err.println("Metoda s nazvem " + child.getName() + " jiz existuje.");
		System.exit(1);
	}
}