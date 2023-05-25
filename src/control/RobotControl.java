package control;

import robot.Robot;

//Robot Assignment for Programming 1 s2 2019
//Adapted by Caspar and Ross from original Robot code written by Dr Charles Thevathayan
public class RobotControl implements Control {
    // we need to internally track where the arm is
	
	// creating variables
    private int height = Control.INITIAL_HEIGHT;
    private int width = Control.INITIAL_WIDTH;
    private int depth = Control.INITIAL_DEPTH;
    private int firstBlockHeight = 0;
    private int secondBlockHeight = 0;
    private int topBarHeight = 0;
    private int currentHeight;
    private int topBlockHeight;
    private boolean outOfJ = false;
    private boolean outOfK = false;
    private int colNum = 2;
    private int barArrayNumber = 0;

    private int[] barHeights;
    private int[] blockHeights;

    private Robot robot;

    // called by RobotImpl
    @Override
    public void control(Robot robot, int barHeightsDefault[], int blockHeightsDefault[]) {
        this.robot = robot;

        // some hard coded init values you can change these for testing
        this.barHeights = new int[]{3, 4, 1, 5, 2, 3, 2, 6};
        this.blockHeights = new int[]{3, 2, 1, 2, 1, 1, 2, 2, 1, 1, 2, 1, 2, 3};
        // FOR SUBMISSION: uncomment following 2 lines
       this.barHeights = barHeightsDefault;
       this.blockHeights = blockHeightsDefault;

        // initialise the robot
        robot.init(this.barHeights, this.blockHeights, height, width, depth);
        // a simple private method to demonstrate how to control robot
        // note use of constant from Control interface
        // You should remove this method call once you start writing your code
        int numOfBlocks = blockHeights.length;
        int depthNum;

        while (numOfBlocks > 0) {
        	// checking which block is the highest to adjust the position of arm 1 (h)
            getMaxHeight();
            // move arm 2 to far left 
            moveArm2(Control.SRC1_COLUMN);
            getBlock(blockHeights[numOfBlocks - 2]);
            // move the arm 2 to the place where the block should be placed
            moveArm2(colNum);
            // making sure that blocks are only placed on barHeights places. from column 3 till how long the barHeights are 
            changeBarPosition();
            // calculate how many arm 3 needs to move down
            depthNum = this.height - blockHeights[numOfBlocks - 2] - barHeights[barArrayNumber] - 1;
            // move arm 3 down
            moveArm3(depthNum);
            // change barHeights
            barHeights[barArrayNumber] = barHeights[barArrayNumber] + blockHeights[numOfBlocks - 2];
            blockHeights[numOfBlocks - 2] = 0;
            drop();
            // checking which block is the highest to adjust the position of arm 1 (h)
            getMaxHeight();
            // making sure barNumber won't go below 0 or more than the number of bars available
            calculateBarIndex();
            // move arm 2 to far right
            moveArm2(Control.SRC2_COLUMN);
            getBlock(blockHeights[numOfBlocks - 1]);
            // move arm 2 to certain column number
            moveArm2(colNum);
            // making sure that blocks are only placed on barHeights places. from column 3 till how long the barHeights are
            changeBarPosition();
            getMaxHeight();
            depthNum = this.height - blockHeights[numOfBlocks - 1] - barHeights[barArrayNumber] - 1;
            moveArm3(depthNum);
            barHeights[barArrayNumber] = barHeights[barArrayNumber] + blockHeights[numOfBlocks - 1];
            blockHeights[numOfBlocks - 1] = 0;
            drop();
            // making sure barNumber won't go below 0 or more than the number of bars available
            calculateBarIndex();
            numOfBlocks -= 2;
        }
        
        exitBot();
    }

    // simple example method to help get you started
    private void extendToWidth(int width) {
    	// extend arm 2(w) to far right
        while (this.width < width) {
            robot.extend();
            this.width++;
        }
    }

    private void reduceToWidth(int width) {
        while (this.width > width) {
        	// contract arm 2(w) to far left
            robot.contract();
            this.width--;
        }
    }

    private void extendToHeight(int height) {
        while (this.height < height) {
        	// move robot's arm 1(h) to max height
            robot.up();
            this.height++;
        }
    }

    private void reduceToHeight(int height) {
        while (this.height > height) {
        	// move arm 1(h) down
            robot.down();
            this.height--;
            // if arm 1(h) is already below 0, set it back to 0
            if (this.height < 0) {
                this.height = 0;
            }
        }
    }

    private void extendToDepth(int depth) {
        while (this.depth < depth) {
        	// lower arm 3(d) until it reaches the desired depth
            robot.lower();
            this.depth++;
        }
    }

    private void reduceToDepth(int depth) {
        while (this.depth > depth) {
        	// raise arm 3(d)
            robot.raise();
            this.depth--;
        }
    }

    private void moveArm2(int width) {
    	// move arm 2(w) to far left or right
        while (this.width != width) {
            if (this.width > width) {
                reduceToWidth(width);
            } else if (this.width < width) {
                extendToWidth(width);
            }
        }
    }

    private void moveVert(int height) {
    	// move arm 1(h) to go up or down
        while (this.height != height) {
            if (this.height > height) {
                reduceToHeight(height);
            } else if (this.height < height) {
                extendToHeight(height);
            }
        }
    }

    private void moveArm3(int depth) {
    	// move arm 3(d) movement to lower or to raise
        while (this.depth != depth) {
            if (this.depth > depth) {
                reduceToDepth(depth);
            } else if (this.depth < depth) {
                extendToDepth(depth);
            }
        }
    }

    private void getBlock(int blockH) {
    	// get the block from the stacks
        calculateBlockHeight();
        // if the block is from left stack
        if (this.width == Control.MIN_WIDTH) {
            moveArm3(this.height - this.firstBlockHeight - 1);
            robot.pick();
            moveArm3(Control.MIN_DEPTH);
            heightOptimisation(blockH);
        }
        // if the block is from right stack
        if (this.width == Control.MAX_WIDTH) {
            moveArm3(this.height - this.secondBlockHeight - 1);
            robot.pick();
            moveArm3(Control.MIN_DEPTH);
            heightOptimisation(blockH);
        }
    }

    private void calculateBlockHeight() {
    	// check highest block height
        topBlockHeight = 0;
        firstBlockHeight = 0;
        secondBlockHeight = 0;
        for (int i = 0; i < blockHeights.length; i += 2) {
            this.firstBlockHeight = this.firstBlockHeight + blockHeights[i];
        }
        for (int i = 1; i < blockHeights.length; i += 2) {
            this.secondBlockHeight = this.secondBlockHeight + blockHeights[i];
        }
        this.topBlockHeight = Math.max(this.firstBlockHeight, this.secondBlockHeight);
    }

    private void calculateBarHeight() {
    	// calculate the bar height from array
        topBarHeight = 0;
        for (int i = 0; i < barHeights.length; i++) {
            if (this.barHeights[i] >= this.topBarHeight) {
                this.topBarHeight = this.barHeights[i];
            }
        }
    }

    private void getMaxHeight() {
    	// get the highest block stack from all the program
        currentHeight = 0;
        calculateBlockHeight();
        calculateBarHeight();
        this.currentHeight = Math.max(this.topBlockHeight, this.topBarHeight);
    }

    private void heightOptimisation(int blockHeight) {
    	// lets arm 1(h) to move up just to make enough room to move the blocks
        if (this.height != Control.MAX_HEIGHT) {
            if (this.currentHeight - this.height < blockHeight + 1) {
                moveVert(this.currentHeight + blockHeight + 1);
            }
        }
    }

    private void drop() {
        getMaxHeight();
        robot.drop();
        moveArm3(Control.MIN_DEPTH);
        moveVert(currentHeight + 1);
        getMaxHeight();
    }

    private void changeBarPosition() {
    	// making sure robot doesn't put a block in the places other than the designated position
        if (colNum < 3) {
            outOfJ = false;
        }
        if (colNum > barHeights.length) {
            outOfJ = true;
        }
        if (outOfJ == true) {
            colNum--;
        }
        if (outOfJ == false) {
            colNum++;
        }
    }

    private void calculateBarIndex() {
    	// to check if barArrayNumber is out of the boundary. If it is less than 0 then move it right
    	// If it is more than barHeights then move left 
        if (barArrayNumber >= barHeights.length - 1) {
            outOfK = true;
        }
        if (barArrayNumber <= 0) {
            outOfK = false;
        }
        if (outOfK == true) {
            --barArrayNumber;
        }
        if (outOfK == false) {
            barArrayNumber++;
        }
    }

    private void exitBot() {
        moveVert(Control.MAX_HEIGHT);
        moveArm2(Control.MIN_WIDTH);
        moveArm3(Control.MIN_DEPTH);
    }
}