package compiler;

import java.util.List;
/**
 * Parser
 */
public class Parser {
	/**
	 * Input tokens
	 */
	private List<Token> inputTokens;
	/**
	 * Input
	 */
	private Token input;
	/**
	 * Old input
	 */
	private Token oldInput;
	/**
	 * Pivot
	 */
	private int pivot;
	/**
	 * Node
	 */
	private TokenNode node;
	/**
	 * Parse the given token list
	 * @param tokens token list
	 * @return token node (root)
	 * @throws Exception exception
	 */
	public TokenNode parse(List<Token> tokens) throws Exception {
		if (tokens == null || tokens.isEmpty()) ErrorHandler.progNotFound();
		pivot = 0;
		input = null;
		oldInput = input;
		inputTokens = tokens;
		node = new TokenNode();
		program();
		return node;
	}
	/**
	 * Init
	 * @throws Exception exception
	 */
	public void program() throws Exception {
		getInput();
		blok();
	}
	/**
	 * Assumes next is a definition of a block and tries to parse it.
	 * @throws Exception
	 */
	public void blok() throws Exception {
		if (accept(Token.CONST)) {
			/* Vrchol const, vetve prirazeni
			 *        const 
			 *   =      =      = 
			 * a   3  b   1  c   =
			 *                 d   7
			 */
			node = node.addChild(oldInput);
			defineConst();
			while(accept(Token.COMMA)) {
				defineConst();
			}
			expect(Token.SEMI);
			node = node.getParent();
		}
		if(accept(Token.VAR)) {
			/* Vrchol var, vetve identifikatory
			 *   var 
			 * a b c d
			 */
			node = node.addChild(oldInput);
			expect(Token.IDENT);
			Token jmenoPromenne = oldInput;
			node.addChild(jmenoPromenne);
			while(accept(Token.COMMA)) {
				expect(Token.IDENT);
				jmenoPromenne = oldInput;
				node.addChild(jmenoPromenne);
			}
			expect(Token.SEMI);
			node = node.getParent();
		}
		while(accept(Token.PROC)) {
			/*
			 * Vrchol proc, vlevo jmeno, pod nim argumenty, vpravo blok
			 *        proc
			 *   fce       blok()
			 * a     b
			 */
			node = node.addChild(oldInput);
			expect(Token.IDENT);
			Token jmenoMetody = oldInput;
			node = node.addChild(jmenoMetody);
			expect(Token.LBRAC);

			if(!accept(Token.RBRAC)) {
				expect(Token.IDENT);
				Token argument = oldInput;
				node.addChild(argument);
				while(accept(Token.COMMA)) {
					expect(Token.IDENT);
					argument = oldInput;
					node.addChild(argument);
				}
				expect(Token.RBRAC);
			}
			node = node.getParent();
			blok();
			node = node.getParent();
		}
		if (!accept(Token.RETURN)) {
			prikaz();
			expect(Token.RETURN);
		}
		node = node.addChild(oldInput);
		if(!accept(Token.SEMI)) {
			node.addChild(vyraz());
			expect(Token.SEMI);
		}
		node = node.getParent();
	}
	/**
	 * Assumes next is definition of command and tries to parse it.
	 * @throws Exception exception
	 */
	private void prikaz() throws Exception {
		Token vstup = getInput();
		switch (vstup.getToken()) {
		case Token.CALL:
			/*
			 * Vrchol call, pod nim jmeno fce, pod nim argumenty
			 *   call
			 *   fce  
			 * 5  13  2
			 */
			node = node.addChild(vstup);
			expect(Token.IDENT);
			Token jmenoFce = oldInput;
			TokenNode pom = node.addChild(jmenoFce);
			expect(Token.LBRAC);
			if(!accept(Token.RBRAC)) {
				pom.addChild(vyraz());
				while(accept(Token.COMMA)) {
					pom.addChild(vyraz());
				}
				expect(Token.RBRAC);
			}
			node = node.getParent();
			expect(Token.SEMI);
			break;
		case Token.BEGIN:
			/*
			 * Vice prikazu v bloku - prikazy vedle sebe
			 */
			prikaz();
			while(!accept(Token.END)) {
				prikaz();
			}
			break;
		case Token.IF:
			/*
			 * Vrchol if vlevo podminka, uprostred then, vpravo else (pokud existuje)
			 *        if
			 * podm    then      (else)
			 *        P1   P2    P3   P4
			 * 
			 */
			node = node.addChild(oldInput);
			node.addChild(podminka());
			expect(Token.THEN);
			node = node.addChild(oldInput);
			prikaz();
			node = node.getParent();
			if(accept(Token.ELSE)) {
				node = node.addChild(oldInput);
				prikaz();
				node = node.getParent();
			}
			node = node.getParent();
			break;
		case Token.WHILE:
			/*
			 * Vrchol while, vlevo podminka, vpravo prikaz
			 *     while
			 * podm     DO
			 *        P1  P2
			 */
			node = node.addChild(oldInput);
			node.addChild(podminka());
			expect(Token.DO);
			node = node.addChild(oldInput);
			prikaz();
			node = node.getParent();
			node = node.getParent();
			break;
		case Token.DO:
			/*
			 * Vrchol DO, vlevo prikaz, vpravo while
			 *         DO
			 *     DO      podm
			 *   P1  P2
			 */
			node = node.addChild(oldInput);
			node = node.addChild(oldInput);
			prikaz();
			node = node.getParent();
			expect(Token.WHILE);
			node.addChild(podminka());
			node = node.getParent();
			expect(Token.SEMI);
			break;
		case Token.SWITCH:
			/*
			 * Vrchol switch, vlevo vyraz, vpravo hodnoty, pod nimi prikazy
			 *    switch
			 * a    1      2
			 *     prik   prik
			 */
			node = node.addChild(oldInput);
			node.addChild(vyraz());
			oneCase();
			while(accept(Token.COMMA)) {
				oneCase();
			}
			node = node.getParent();
			break;
		default:
			/*
			 * Vrchol rovnitko, vlevo promenna, vpravo hodnota
			 *    =            =
			 * a     =       d   9
			 *     c   3
			 */
			Token promenna = oldInput;
			TokenNode vrchol = node;
			expect(Token.ASSIGN);
			node = node.addChild(oldInput);
			node.addChild(promenna);
			while(!accept(Token.ASSIGN)) {
				expect(Token.IDENT);
				promenna = oldInput;
				expect(Token.ASSIGN);
				node = node.addChild(oldInput);
				node.addChild(promenna);
			}
			node.addChild(vyraz());
			node = vrchol;
			expect(Token.SEMI);
			break;
		}
	}
	/**
	 * Assumes next is definition of condition and tries to parse it.
	 * @return token node
	 * @throws Exception exception
	 */
	public TokenNode podminka() throws Exception {
		TokenNode podminka = null;
		if (accept(Token.EXCL)) {
			/*
			 * Vrchol vykricnik, pod nim podminka
			 *  !
			 * pom()
			 */
			podminka = new TokenNode(oldInput);
			expect(Token.LBRAC);
			podminka.addChild(podminka());
			expect(Token.RBRAC);
		} else {
			/*
			 * Vrchol podminka, pod nim vyrazy
			 *   <
			 * a   >
			 *   b   c
			 */
			TokenNode leva = vyraz();
			int array[] = new int[] {Token.EQUAL, Token.DIFF, Token.LT, Token.GT, Token.LET, Token.GET, Token.AND, Token.OR};
			expect(array);
			podminka = new TokenNode(oldInput);
			TokenNode prava = vyraz();
			podminka.addChild(leva);
			podminka.addChild(prava);
		}
		return podminka;
	}
	/**
	 * Assumes next is definition of expression and tries to parse it.
	 * @return token node
	 * @throws Exception exception
	 */
	public TokenNode vyraz() throws Exception {
		TokenNode vyraz = null; 
		if(accept(Token.QUEST)) {
			/*
			 * Vrchol otaznik, vlevo true, vpravo false
			 *        ?
			 * a > b  3   5
			 */
			TokenNode podm = podminka();
			expect(Token.QUEST);
			vyraz = new TokenNode(oldInput);
			vyraz.addChild(podm);
			vyraz.addChild(vyraz());
			expect(Token.COLON);
			vyraz.addChild(vyraz());
		} 
		else {
			/*
			 * Vrchol znamenko, vlevo term, vpravo term nebo znamenko
			 * term - term1 + term2 - term3 =>
			 *         -
			 * term         +
			 *       term1       -
			 *             term2   term3
			 */
			TokenNode vrchol = null;
			TokenNode leva = term();
			int array[] = new int[] {Token.PLUS, Token.MINUS};
			while(accept(array)) {
				if(vyraz == null) {
					vyraz = new TokenNode(oldInput);
					vrchol = vyraz;
				}
				else vyraz = vyraz.addChild(oldInput);
				vyraz.addChild(leva);
				leva = term();
			}
			if(vyraz == null) {
				vyraz = leva;
				vrchol = vyraz;
			}
			else vyraz.addChild(leva);
			vyraz = vrchol;
		}
		return vyraz;
	}
	/**
	 * Assumes next is term and tries to parse it.
	 * @return token node
	 * @throws Exception exception
	 */
	public TokenNode term() throws Exception {
		/*
		 * Vrchol cislo, nebo znamenko a vlevo cislo a vpravo dalsi znamenka a cisla
		 * a * b / c =>
		 *    *
		 * a     /
		 *     b   c
		 */
		TokenNode vrchol = null;
		TokenNode term = null;
		TokenNode leva = faktor();
		int array[] = new int[] {Token.TIMES, Token.DIVIDE};
		while(accept(array)) {
			if(term == null) {
				term = new TokenNode(oldInput);
				vrchol = term;
			}
			else term = term.addChild(oldInput);
			term.addChild(leva);
			leva = faktor();
		}
		if(term == null) {
			term = leva;
			vrchol = term;
		}
		else term.addChild(leva);
		term = vrchol;
		return term;
	}
	/**
	 * Assumes next is definition of a factor and tries to parse it.
	 * @return token node
	 * @throws Exception exception
	 */
	public TokenNode faktor() throws Exception {
		TokenNode faktor = null;
		if (input.getToken() == Token.MINUS) {
			/*
			 * Minus cislo
			 */
			expect(Token.MINUS);
			expect(Token.INT);
			Token cislo = oldInput;
			cislo.minusNumber();
			faktor = new TokenNode(cislo);
		}
		else if (isNextNumber()) {
			/*
			 * Jen cislo
			 */
			expect(Token.INT);
			Token cislo = oldInput;
			faktor = new TokenNode(cislo);
		} else {
			Token vstup = getInput();
			switch (vstup.getToken()) {
			case Token.CALL: 
				/*
				 * Vrchol Call, pod nim jmeno fce, pod nim argumenty
				 *  call
				 *  fce
				 * 1   3
				 */
				faktor = new TokenNode(vstup);
				expect(Token.IDENT);
				Token jmenoFce = oldInput;
				TokenNode pom = faktor.addChild(jmenoFce);
				expect(Token.LBRAC);
				if(!accept(Token.RBRAC)) {
					pom.addChild(vyraz());
					while(accept(Token.COMMA)) {
						pom.addChild(vyraz());
					}
					expect(Token.RBRAC);
				}
				break;
			case Token.LBRAC:
				/*
				 * Vracime vyraz
				 */
				faktor = vyraz();
				expect(Token.RBRAC);
				break;
			default:
				/*
				 * Vracime jmeno promenne
				 */
				faktor = new TokenNode(vstup);
			}
		}
		return faktor;
	}
	/**
	 * Assumes next is definition of switch and tries to parse it.
	 * @throws Exception exception
	 */
	public void oneCase() throws Exception {
		/*
		 * Vrchol cislo, pod nim prikazy
		 *   5
		 * prikaz
		 */
		expect(Token.CASE);
		if (accept(Token.MINUS)) {
			/*
			 * Minus cislo
			 */
			expect(Token.INT);
			Token cislo = oldInput;
			cislo.minusNumber();
			node = node.addChild(cislo);
		}
		else {
			expect(Token.INT);
			Token cislo = oldInput;
			node = node.addChild(cislo);
		}
		expect(Token.COLON);
		prikaz();
		node = node.getParent();
	}
	/**
	 * Assumes next is definition of constant and tries to parse it.
	 * @throws Exception Exception
	 */
	public void defineConst() throws Exception {
		/*
		 * Vrchol prirazeni, vlevo ident, vpravo ident nebo hodnota
		 * a = c = 5 =>
		 *    =
		 * a     =
		 *     c   5
		 */
		TokenNode konstanta = node;
		expect(Token.IDENT);
		Token jmenoKonstanty = oldInput;
		if (jmenoKonstanty.getToken() != Token.IDENT) ErrorHandler.notConst(jmenoKonstanty.getLexem());
		expect(Token.ASSIGN);
		node = node.addChild(oldInput);
		node.addChild(jmenoKonstanty);
		while (!isNextNumber() && input.getToken() != Token.MINUS) {
			expect(Token.IDENT);
			jmenoKonstanty = oldInput;
			expect(Token.ASSIGN);
			node = node.addChild(oldInput);
			node.addChild(jmenoKonstanty);
		}

		if (accept(Token.MINUS)) {
			/*
			 * Minus cislo
			 */
			expect(Token.INT);
			Token cislo = oldInput;
			cislo.minusNumber();
			node.addChild(cislo);
		}
		else {
			expect(Token.INT);
			Token cislo = oldInput;
			node.addChild(cislo);
		}
		node = konstanta;
	}
	/**
	 * Tells if next is a number
	 * @return is number
	 */
	public boolean isNextNumber() {
		return (input.getToken() == Token.INT);
	}
	/**
	 * Gets next token
	 * @return next token to parse
	 */
	public Token getInput() {
		oldInput = input;
		input = inputTokens.get(pivot);
		if (pivot < inputTokens.size() - 1) pivot++;
		return oldInput;
	}
	
	/**
	 * Given token is expected in input
	 * @param token expected token
	 * @return correctly expected
	 * @throws Exception exception
	 */
	private boolean expect(int token) throws Exception {
		if(accept(token)) return true;
		ErrorHandler.parserError(token, pivot);
		return false;
	}
	/**
	 * given tokens are expected in input
	 * @param tokens expected tokens
	 * @return correctly expected
	 * @throws Exception exception
	 */
	private boolean expect(int tokens[]) throws Exception {
		if(accept(tokens)) return true;
		ErrorHandler.parserError(tokens, pivot);
		return false;
	}
	/**
	 * Tells if input token is correct
	 * @param token expected token
	 * @return correctly expected
	 */
	private boolean accept(int token) {
		boolean result = (input.getToken() == token);
		if (result) getInput();
		return result;
	}
	/**
	 * Tells if input token are correct
	 * @param tokens expected tokens
	 * @return correctly expected
	 */
	private boolean accept(int tokens[]) {
		boolean result = false;
		int i = 0;
		while (i < tokens.length && !result) {
			result = (tokens[i++] == input.getToken());
		}
		if (result) getInput();
		return result;
	}
}
