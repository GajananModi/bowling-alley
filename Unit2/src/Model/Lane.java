package Model;

import persistence.ScoreHistoryDb;

import java.util.*;

public class Lane extends Observable implements Observer, Runnable {

	private Party party;
	private Pinsetter setter;
	private HashMap scores;
	private boolean gameIsHalted;
	private boolean partyAssigned;
	private boolean gameFinished;
	private Iterator bowlerIterator;
	private int ball;
	private int bowlIndex;
	private int frameNumber;
	private boolean tenthFrameStrike;
	private int[][] cumulScores;
	private boolean canThrowAgain;
	private int gameNumber;
	private int secondIndex;
	private int maxIndex;
	private Bowler currentThrower;			// = the thrower who just took a throw
	private ScoreCalculator sc;

	/** Lane()
	 *
	 * Constructs a new lane and starts its thread
	 *
	 * @pre none
	 * @post a new lane has been created and its thread is executing
	 */
	public Lane() {
		this.setter = new Pinsetter();
		this.setter.addObserver(this);
		this.scores = new HashMap();
		this.gameIsHalted = false;
		this.partyAssigned = false;
		this.gameNumber = 0;
		(new Thread(this, "Lane Thread")).start();
	}

	/** run()
	 *
	 * entry point for execution of this lane
	 */
	public void run() {
		while (true) {
			if (partyAssigned && !gameFinished) {	// we have a party on this lane, so next bower can take a throw
				while (gameIsHalted) {
					try {
						Thread.sleep(10);
					} catch (Exception e) {}
				}
				if (bowlerIterator.hasNext()) {
					currentThrower = (Bowler)bowlerIterator.next();
					canThrowAgain = true;
					if(frameNumber==0){
//						System.out.println(currentThrower.getNick());
					}
					if( (frameNumber==10 && bowlIndex!=secondIndex) || ( frameNumber>10 && !(bowlIndex==secondIndex || bowlIndex== maxIndex) ) ) {
//						System.out.println("Here for index:");
//						System.out.println(bowlIndex);
						canThrowAgain = false;
					}
					tenthFrameStrike = false;
					ball = 0;
					while (canThrowAgain) {
						setter.ballThrown();		// simulate the thrower's ball hiting
						ball++;
					}
					if (frameNumber == 9){
						try{
							Date date = new Date();
							String dateString = "" + date.getHours() + ":" + date.getMinutes() + " " + date.getMonth() + "/" + date.getDay() + "/" + (date.getYear() + 1900);
							ScoreHistoryDb.addScore(currentThrower.getNick(), dateString, new Integer(cumulScores[bowlIndex][9]).toString());
						} catch (Exception e) {System.err.println("Exception in addScore. "+ e );}
					}
//					if(frameNumber == 13){
//						try{
//							Date date = new Date();
//							String dateString = "" + date.getHours() + ":" + date.getMinutes() + " " + date.getMonth() + "/" + date.getDay() + "/" + (date.getYear() + 1900);
//							ScoreHistoryDb.addScore(currentThrower.getNick(), dateString, new Integer(cumulScores[bowlIndex][9]).toString());
//						} catch (Exception e) {System.err.println("Exception in addScore. "+ e );}
//					}
					setter.reset();
					bowlIndex++;
				} else {
					frameNumber++;
					resetBowlerIterator();
					bowlIndex = 0;
					if (frameNumber == 11)
					{
						int newHigh = new Integer(cumulScores[secondIndex][10]);
						int maxPrev = new Integer(cumulScores[maxIndex][9]);
//						System.out.println("New high");
//						System.out.println(newHigh);
//						System.out.println(secondIndex);
						if (newHigh < maxPrev){
							publish();
							gameFinished = true;
							gameNumber++;
						}
					}
					if (frameNumber == 14)
					{
						publish();
						gameFinished = true;
						gameNumber++;
					}
					if (frameNumber == 10)
					{
						int maxScore = new Integer(cumulScores[bowlIndex][9]);
						maxIndex = 0;
						secondIndex =0;
						int second = 0;
						currentThrower = (Bowler)bowlerIterator.next();
						if (!bowlerIterator.hasNext())
						{
							publish();
							gameFinished = true;
							gameNumber++;
						}
						else
						{
							int current;
							while(bowlerIterator.hasNext())
							{
								currentThrower = (Bowler)bowlerIterator.next();
								bowlIndex++;
								current = new Integer(cumulScores[bowlIndex][9]);
								if(current>maxScore)
								{
//									System.out.println("In c1");
									second = maxScore;
									maxScore = current;
									secondIndex = maxIndex;
									maxIndex = bowlIndex;
								}
								else if(current>second && current!=maxScore)
								{
//									System.out.println("In c2");
									second = current;
									secondIndex = bowlIndex;
								}
							}
							resetBowlerIterator();
						}
						bowlIndex = 0;
						resetBowlerIterator();
//						System.out.println("Second Highest is");
//						System.out.println(secondIndex);
					}
				}
			}
			else if (partyAssigned && gameFinished) {
				publish();
			}
			try {
				Thread.sleep(10);
			} catch (Exception e) {}
		}
	}

	public void clearLane(){
		party = null;
		partyAssigned = false;
	}

	/** resetBowlerIterator()
	 *
	 * sets the current bower iterator back to the first bowler
	 *
	 * @pre the party as been assigned
	 * @post the iterator points to the first bowler in the party
	 */
	public void resetBowlerIterator() {
		bowlerIterator = (party.getMembers()).iterator();
	}

	/** resetScores()
	 *
	 * resets the scoring mechanism, must be called before scoring starts
	 *
	 * @pre the party has been assigned
	 * @post scoring system is initialized
	 */
	public void resetScores() {
		Iterator bowlIt = (party.getMembers()).iterator();
		while ( bowlIt.hasNext() ) {
			int[] toPut = new int[32];
			for ( int i = 0; i != 32; i++){
				toPut[i] = -1;
			}
			scores.put( bowlIt.next(), toPut );
		}
		gameFinished = false;
		frameNumber = 0;
	}

	/** assignParty()
	 *
	 * assigns a party to this lane
	 *
	 * @pre none
	 * @post the party has been assigned to the lane
	 *
	 * @param theParty		Party to be assigned
	 */
	public void assignParty( Party theParty ) {
		party = theParty;
		resetBowlerIterator();
		partyAssigned = true;
		cumulScores = new int[party.getMembers().size()][14];
		this.sc = new ScoreCalculator(cumulScores);
		gameNumber = 0;
		resetScores();
	}

	/** markScore()
	 *
	 * Method that marks a bowlers score on the board.
	 *
	 * @param Cur		The current bowler
	 * @param frame	The frame that bowler is on
	 * @param ball		The ball the bowler is on
	 * @param score	The bowler's score
	 */
	private void markScore( Bowler Cur, int frame, int ball, int score ){
		int[] curScore;
		int index =  ( (frame - 1) * 2 + ball);
		if (frame == 11){
			index = 22;
		}
		curScore = (int[]) scores.get(Cur);
		curScore[ index - 1] = score;
//		if (frame == 11){
//			System.out.println("ayyyylmaqo");
//			for (int i = 0; i < curScore.length; i++) {
//				System.out.println(curScore[i]);
//			}
//		}
		scores.put(Cur, curScore);
		sc.calculateGame((int[])this.scores.get(Cur), this.bowlIndex, this.frameNumber, ball, score);
		publish();
	}

	public int[][] getFinalScores(){
		return(cumulScores);
	}

	public int getGameNumber(){
		return(this.gameNumber);
	}

	/** isPartyAssigned()
	 *
	 * checks if a party is assigned to this lane
	 *
	 * @return true if party assigned, false otherwise
	 */
	public boolean isPartyAssigned() {
		return partyAssigned;
	}

	/** isGameFinished
	 *
	 * @return true if the game is done, false otherwise
	 */
	public boolean isGameFinished() {
		return gameFinished;
	}

	public void publish(){
		this.setChanged();
		this.notifyObservers();
	}

	/**
	 * Pause the execution of this game
	 */
	public void pauseGame() {
		gameIsHalted = true;
		publish();
	}

	/**
	 * Resume the execution of this game
	 */
	public void unPauseGame() {
		gameIsHalted = false;
		publish();
	}

	/**
	 * @return the party
	 */
	public Party getParty() {
		return party;
	}

	/**
	 * @return the scores
	 */
	public HashMap getScores() {
		return scores;
	}

	/**
	 * @return the gameIsHalted
	 */
	public boolean isGameIsHalted() {
		return gameIsHalted;
	}

	/**
	 * @return the ball
	 */
	public int getBall() {
		return ball;
	}

	/**
	 * @return the bowlIndex
	 */
	public int getBowlIndex() {
		return bowlIndex;
	}

	/**
	 * @return the frameNumber
	 */
	public int getFrameNumber() {
		return frameNumber;
	}

	/**
	 * @return the cumulScores
	 */
	public int[][] getCumulScores() {
		return cumulScores;
	}

	/**
	 * @return the currentThrower
	 */
	public Bowler getCurrentThrower() {
		return currentThrower;
	}

	/**
	 * Assessor to get this Lane's pinsetter
	 *
	 * @return		A reference to this lane's pinsetter
	 */
	public Pinsetter getPinsetter() {
		return setter;
	}

	@Override
	public void update(Observable o, Object arg) {
		PinsetterEvent pe = (PinsetterEvent)arg;
		if(pe.pinsDownOnThisThrow() < 0){ // this is not a real throw
			return;
		}
		markScore(currentThrower, frameNumber + 1, pe.getThrowNumber(), pe.pinsDownOnThisThrow());
		// next logic handles the ?: what conditions dont allow them another throw?
		// handle the case of 10th frame first
		if (frameNumber == 9) {
			if (pe.totalPinsDown() == 10) {
				setter.resetPins();
				if(pe.getThrowNumber() == 1) {
					tenthFrameStrike = true;
				}
			}
			if ((pe.totalPinsDown() != 10) && (pe.getThrowNumber() == 2 && tenthFrameStrike == false) || pe.getThrowNumber() == 3) {
				canThrowAgain = false;
			}
		} else if (pe.pinsDownOnThisThrow() == 10 || pe.getThrowNumber() == 2 || (frameNumber == 10 && pe.getThrowNumber() == 1)) {		// threw a strike
//			System.out.println("dsgfGFDSA");
//			System.out.println(frameNumber);
			canThrowAgain = false;
		}
	}
}