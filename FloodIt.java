import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// represents a single square of the game area
class Cell {

  // in logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  String color;
  boolean flooded;

  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  // constructor
  Cell(int x, int y, Cell left, Cell top, Cell right, Cell bottom, String color) {
    this.x = x;
    this.y = y;
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
    if (left != null) {
      left.right = this;
    }
    if (top != null) {
      top.bottom = this;
    }
    if (right != null) {
      right.left = this;
    }
    if (bottom != null) {
      bottom.top = this;
    }
    this.color = color;
  }

  // return the color of this cell
  int getColorIndex(ArrayList<String> colorStrings) {
    for (int i = 0; i < colorStrings.size(); i++) {
      if (colorStrings.get(i).compareToIgnoreCase(this.color) == 0) {
        return i;
      }
    }
    return -1;
  }

  // flood the adjacent cells with the given color and add them to the controlled
  // list
  void flood(String color, ArrayList<Cell> controlled, ArrayList<Cell> checked) {
    if (checked.contains(this)) {
      return;
    }
    if (controlled.contains(this)) {
      checked.add(this);
      this.flooded = true;
      if (this.left != null) {
        this.left.flood(color, controlled, checked);
      }
      if (this.top != null) {
        this.top.flood(color, controlled, checked);
      }
      if (this.right != null) {
        this.right.flood(color, controlled, checked);
      }
      if (this.bottom != null) {
        this.bottom.flood(color, controlled, checked);
      }
    }
    else if (this.color.equals(color)) {
      controlled.add(this);
      checked.add(this);
      this.flooded = true;
      if (this.left != null) {
        this.left.flood(color, controlled, checked);
      }
      if (this.top != null) {
        this.top.flood(color, controlled, checked);
      }
      if (this.right != null) {
        this.right.flood(color, controlled, checked);
      }
      if (this.bottom != null) {
        this.bottom.flood(color, controlled, checked);
      }
    }
  }
}

// represent the game world
class Game extends World {
  // all possible colors
  ArrayList<Color> colors = new ArrayList<Color>(Arrays.asList(Color.red, Color.green, Color.blue,
      Color.cyan, Color.magenta, Color.yellow, Color.orange, Color.pink));
  // all possible colors (String)
  ArrayList<String> colorStrings = new ArrayList<String>(
      Arrays.asList("red", "green", "blue", "cyan", "magenta", "yellow", "orange", "pink"));
  // selected colors (randomly selected at the beginning of the game)
  ArrayList<Color> selectedColors = new ArrayList<Color>();
  // number of colors selected
  int numColors;
  // random object
  Random rand;
  // all the cells of the game
  ArrayList<Cell> board;
  // cells that the player controls (flooded == true)
  ArrayList<Cell> controlled;
  // cells to be flooded on the next tick
  ArrayList<Cell> floodQueue;
  // number of squares in each row
  int gridSize;
  // number of guesses allowed
  int numGuesses;
  // used number of guesses
  int usedGuesses;
  // current color clicked
  String currentColor;
  // the state of the game
  // 0 : ongoing
  // -1 : lose
  // 1 : win
  int gameState;
  // stop watch (in seconds)
  double stopWatch;
  // size of the squares
  static final int SQUARE_SIZE = 20;
  // half the size of the squares
  static final int HALF_SIZE = SQUARE_SIZE / 2;
  // level of difficulty (numGuesses = gridSize/DIFFICULTY * numColors)
  static final int DIFFICULTY = 3;
  // font size
  static final int FONT_SIZE = 30;
  // default tick rate
  static final double TICK_RATE = 0.01;

  // constructor
  Game(int gridSize, int numColors, Random rand) {
    if (numColors < 3 || numColors > this.colors.size()) {
      throw new IllegalArgumentException(
          "Number of Colors must be greater than 3" + " and less than 8");
    }
    this.rand = rand;
    this.gridSize = gridSize;
    this.numColors = numColors;
    this.selectedColors = this.initColors();
    this.board = this.generateCells();
    this.numGuesses = this.gridSize / DIFFICULTY * this.numColors;
    this.usedGuesses = 0;
    this.gameState = 0;
    this.currentColor = this.board.get(0).color;
    this.floodQueue = new ArrayList<Cell>();
    this.controlled = new ArrayList<Cell>();
    this.controlled.add(this.board.get(0));
    this.board.get(0).flood(this.board.get(0).color, this.controlled, new ArrayList<Cell>());
    this.stopWatch = 0;
  }

  // constructor
  Game(int gridSize, int numColors) {
    this(gridSize, numColors, new Random());
  }


  // generate the initial selected colors
  ArrayList<Color> initColors() {
    ArrayList<Color> result = new ArrayList<Color>();
    for (int i = 0; i < this.numColors; i++) {
      result.add(this.colors.get(i));
    }
    return result;
  }

  // generate all cells
  ArrayList<Cell> generateCells() {
    ArrayList<Cell> cells = new ArrayList<Cell>();
    for (int i = 0; i < this.gridSize; i++) {
      for (int j = 0; j < this.gridSize; j++) {
        if (i == 0 && j == 0) {
          cells.add(new Cell(HALF_SIZE, HALF_SIZE, null, null, null, null, this.selectColor()));
        }
        else if (i == this.gridSize - 1 && j == this.gridSize - 1) {
          cells.add(new Cell(HALF_SIZE + SQUARE_SIZE * j, HALF_SIZE + SQUARE_SIZE * i,
              cells.get(this.gridSize * this.gridSize - 2),
              cells.get(this.gridSize * this.gridSize - this.gridSize - 1), null, null,
              this.selectColor()));
        }
        else if (i == 0) {
          cells.add(new Cell(HALF_SIZE + SQUARE_SIZE * j, HALF_SIZE + SQUARE_SIZE * i,
              cells.get(j - 1), null, null, null, this.selectColor()));
        }
        else if (j == 0) {
          cells.add(new Cell(HALF_SIZE + SQUARE_SIZE * j, HALF_SIZE + SQUARE_SIZE * i, null,
              cells.get((i - 1) * this.gridSize), null, null, this.selectColor()));
        }
        else {
          cells.add(new Cell(HALF_SIZE + SQUARE_SIZE * j, HALF_SIZE + SQUARE_SIZE * i,
              cells.get(i * this.gridSize + j - 1), cells.get((i - 1) * this.gridSize + j), null,
              null, this.selectColor()));
        }
      }
    }
    return cells;
  }

  // return a randomly selected color String
  String selectColor() {
    int count = this.rand.nextInt(this.numColors);
    return this.colorStrings.get(count);
  }


  // render the world state
  public WorldScene makeScene() {
    WorldImage text = new TextImage("", FONT_SIZE, Color.black);

    WorldImage score = new TextImage(Integer.toString(this.usedGuesses) + "/" 
        + Integer.toString(this.numGuesses),
        FONT_SIZE, Color.black);
    if (this.gameState == 1) {
      text = new TextImage("You win", FONT_SIZE, Color.black);
    }
    else if (this.gameState == -1) {
      text = new TextImage("You lose", FONT_SIZE, Color.black);
    }
    int gameSize = this.gridSize * SQUARE_SIZE;
    WorldScene scene = new WorldScene(gameSize, gameSize + FONT_SIZE * 2);
    for (Cell c : this.board) {
      scene.placeImageXY(new RectangleImage(SQUARE_SIZE, SQUARE_SIZE, "solid",
          this.colors.get(c.getColorIndex(this.colorStrings))), c.x, c.y);
    }
    scene.placeImageXY(score, gameSize / 2, gameSize + FONT_SIZE);
    scene.placeImageXY(text, gameSize - FONT_SIZE - 40, gameSize + FONT_SIZE);
    scene.placeImageXY(this.generateStopWatch(), FONT_SIZE + 10, gameSize + FONT_SIZE);
    return scene;
  }


  // key even handler
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.board = this.generateCells();
      this.usedGuesses = 0;
      this.gameState = 0;
      this.stopWatch = 0;
    }
  }

  // mouse event handler
  public void onMouseClicked(Posn pos) {
    if (this.floodQueue.size() != 0) {
      return;
    }
    Cell clicked = this.board.get(pos.y / SQUARE_SIZE * this.gridSize + pos.x / SQUARE_SIZE);
    this.currentColor = clicked.color;
    if (this.currentColor.equals(this.controlled.get(0).color)) {
      return;
    }
    this.board.get(0).flood(this.currentColor, this.controlled, new ArrayList<Cell>());
    this.usedGuesses ++;
    this.floodQueue.add(this.board.get(0));
    if (this.board.size() == this.controlled.size() && this.usedGuesses <= this.numGuesses) {
      this.gameState = 1;
    }
    else if (this.usedGuesses > this.numGuesses) {
      this.gameState = -1;
    }
  }

  // on tick
  public void onTick() {
    this.stopWatch = this.stopWatch + TICK_RATE;
    ArrayList<Cell> temp = new ArrayList<Cell>();
    temp.addAll(this.floodQueue);
    for (Cell c : temp) {
      c.color = this.currentColor;
      c.flooded = false;
      this.floodQueue.remove(0);
      if (c.left != null && c.left.flooded && !this.floodQueue.contains(c.left)) {
        this.floodQueue.add(c.left);
      }
      if (c.top != null && c.top.flooded && !this.floodQueue.contains(c.top)) {
        this.floodQueue.add(c.top);
      }
      if (c.right != null && c.right.flooded && !this.floodQueue.contains(c.right)) {
        this.floodQueue.add(c.right);
      }
      if (c.bottom != null && c.bottom.flooded && !this.floodQueue.contains(c.bottom)) {
        this.floodQueue.add(c.bottom);
      }
    }
  }

  // generate the stopWatch text image
  WorldImage generateStopWatch() {
    int tens = (int)(this.stopWatch % 60) / 10;
    int ones = (int)this.stopWatch % 60 - tens * 10;
    return new TextImage(
        Integer.toString((int)this.stopWatch / 60) + ":"
            + Integer.toString(tens) + Integer.toString(ones), FONT_SIZE, Color.black);

  }
}

// examples and tests 
class ExamplesGame {
  Cell cell1;
  Cell cell2;
  Cell cell3;
  Cell cell4;
  Cell cell5;

  Game game1;
  Game game2;
  Game game3;

  ArrayList<Color> colors = new ArrayList<Color>(Arrays.asList(Color.red, Color.green, Color.blue,
      Color.cyan, Color.magenta, Color.yellow, Color.orange, Color.pink));
  ArrayList<String> colorStrings = new ArrayList<String>(
      Arrays.asList("red", "green", "blue", "cyan", "magenta", "yellow", "orange", "pink"));

  // test cell constructor
  void testCell(Tester t) {
    this.cell1 = new Cell(1, 1, null, null, null, null, "red");
    t.checkExpect(this.cell1.x, 1);
    t.checkExpect(this.cell1.y, 1);
    t.checkExpect(this.cell1.bottom, null);
    t.checkExpect(this.cell1.left, null);
    this.cell2 = new Cell(2, 2, cell1, null, null, null, "blue");
    t.checkExpect(this.cell1.right, this.cell2);
    t.checkExpect(this.cell2.left, this.cell1);
    t.checkExpect(this.cell2.bottom, null);
    t.checkExpect(this.cell2.right, null);
    this.cell3 = new Cell(3, 3, null, this.cell1, null, null, "green");
    t.checkExpect(this.cell3.top, this.cell1);
    t.checkExpect(this.cell1.bottom, this.cell3);
    this.cell4 = new Cell(4, 4, null, null, this.cell3, null, "red");
    t.checkExpect(this.cell4.right, this.cell3);
    t.checkExpect(this.cell3.left, this.cell4);
    reset();
  }

  // reset
  void reset() {
    this.cell1 = new Cell(10, 10, null, null, null, null, "red");
    this.cell2 = new Cell(30, 10, this.cell1, null, null, null, "green");
    this.cell3 = new Cell(10, 30, null, this.cell1, null, null, "blue");
    this.cell4 = new Cell(30, 30, this.cell3, this.cell2, null, null, "green");
    this.cell5 = new Cell(50, 10, this.cell2, null, null, null, "gray");

    this.game1 = new Game(3, 4, new Random(1));
    this.game2 = new Game(2, 3, new Random(1));
    this.game3 = new Game(22, 6, new Random(3));
  }

  // test the method getColorIndex in the class Cell
  void testGetColorIndex(Tester t) {
    reset();
    t.checkExpect(this.cell1.getColorIndex(this.colorStrings), 0);
    t.checkExpect(this.cell2.getColorIndex(this.colorStrings), 1);
    t.checkExpect(this.cell3.getColorIndex(this.colorStrings), 2);
    t.checkExpect(this.cell5.getColorIndex(this.colorStrings), -1);
    reset();
  }

  // test the method initColor in the class Game
  void testInitColor(Tester t) {
    reset();
    t.checkExpect(this.game1.initColors(),
        new ArrayList<Color>(Arrays.asList(Color.red, Color.green, Color.blue, Color.cyan)));
    t.checkExpect(this.game2.initColors(),
        new ArrayList<Color>(Arrays.asList(Color.red, Color.green, Color.blue)));
    reset();
  }

  // test the Game constructor
  void testGame(Tester t) {
    t.checkConstructorException(
        new IllegalArgumentException(
            "Number of Colors must be greater " + "than 3 and less than 8"),
        "Game", 3, 10, new Random(1));
    t.checkConstructorException(
        new IllegalArgumentException(
            "Number of Colors must be greater " + "than 3 and less than 8"),
        "Game", 3, 2, new Random(1));
  }

  // test the method generateCells in the class Game
  void testGenerateCells(Tester t) {
    reset();
    Game game0 = new Game(2, 3, new Random(1));
    Cell cell1 = new Cell(10, 10, null, null, null, null, game0.selectColor());
    Cell cell2 = new Cell(30, 10, cell1, null, null, null, game0.selectColor());
    Cell cell3 = new Cell(10, 30, null, cell1, null, null, game0.selectColor());
    Cell cell4 = new Cell(30, 30, cell3, cell2, null, null, game0.selectColor());
    t.checkExpect(this.game1.generateCells(), new Game(3, 4, new Random(1)).generateCells());
    t.checkExpect(this.game2.generateCells(),
        new ArrayList<Cell>(Arrays.asList(cell1, cell2, cell3, cell4)));
    reset();
  }

  // test the method selectColor in the class Game
  void testSelectColor(Tester t) {
    reset();
    Game game0 = new Game(3, 4, new Random(1));
    t.checkExpect(this.game1.selectColor(), game0.selectColor());
    t.checkExpect(this.game1.selectColor(), game0.selectColor());
    t.checkExpect(this.game1.selectColor(), game0.selectColor());
    reset();
  }

  // test the onKeyEvent method in the class Game
  void testOnKeyEvent(Tester t) {
    reset();
    Game temp = new Game(3, 4, new Random(1));
    t.checkExpect(this.game1.board, this.game1.board);
    t.checkExpect(this.game1.usedGuesses, 0);
    this.game1.usedGuesses = 1;
    t.checkExpect(this.game1.usedGuesses, 1);
    this.game1.onKeyEvent("a");
    t.checkExpect(this.game1.board, temp.board);
    t.checkExpect(this.game1.usedGuesses, 1);
    this.game1.onKeyEvent("r");
    t.checkExpect(this.game1.board, temp.generateCells());
    t.checkExpect(this.game1.usedGuesses, 0);
    t.checkExpect(this.game1.gameState, 0);
    t.checkInexact(this.game1.stopWatch, 0.0, 0.0001);
    reset();
  }

  // test the method flood in the class Cell
  void testFlood(Tester t) {
    reset();
    ArrayList<Cell> list1 = new ArrayList<Cell>();
    list1.add(this.cell1);
    Cell cell1v2 = this.cell1;
    this.cell1.flood("blue", new ArrayList<Cell>(), list1);
    t.checkExpect(this.cell1, cell1v2);
    this.cell1.flood("blue", list1, new ArrayList<Cell>());
    t.checkExpect(this.cell1.flooded, true);
    t.checkExpect(this.cell3.flooded, true);
    t.checkExpect(list1.get(1), this.cell3);
    reset();
  }

  // test the method onMouseEvent in the class Game
  void testOnMouseEvent(Tester t) {
    reset();
    ArrayList<Cell> list1 = new ArrayList<Cell>();
    this.cell2.right = null;
    list1.add(this.cell1);
    list1.add(this.cell2);
    list1.add(this.cell3);
    list1.add(this.cell4);
    Game game1 = new Game(2, 3, new Random(1));
    game1.board = list1;
    game1.numGuesses = 3;
    t.checkExpect(game1.controlled.size(), 1);
    game1.onMouseClicked(new Posn(35, 4));
    ArrayList<Cell> list2 = new ArrayList<Cell>();
    list2.add(this.cell1);
    t.checkExpect(game1.currentColor, "green");
    t.checkExpect(game1.usedGuesses, 1);
    t.checkExpect(game1.floodQueue, list2);
    t.checkExpect(game1.gameState, 0);
    game1.usedGuesses = 3;
    game1.floodQueue.clear();
    game1.onMouseClicked(new Posn(15, 25));
    t.checkExpect(game1.gameState, -1);
    reset();
  }

  // test the method onTick in the class game
  void testOnTick(Tester t) {
    reset();
    ArrayList<Cell> list1 = new ArrayList<Cell>();
    this.cell2.right = null;
    list1.add(this.cell1);
    list1.add(this.cell2);
    list1.add(this.cell3);
    list1.add(this.cell4);
    Game game1 = new Game(2, 3, new Random(1));
    game1.board = list1;
    game1.numGuesses = 3;
    this.cell1.flooded = true;
    this.cell2.flooded = true;
    this.cell4.flooded = true;
    ArrayList<Cell> list2 = new ArrayList<Cell>();
    list2.add(this.cell1);
    game1.floodQueue = list2;
    game1.currentColor = "green";
    game1.onTick();
    ArrayList<Cell> list3 = new ArrayList<Cell>();
    list3.add(this.cell2);
    t.checkExpect(game1.floodQueue, list3);
    t.checkExpect(this.cell1.flooded, false);
    t.checkExpect(this.cell1.color, "green");
    t.checkExpect(this.cell2.flooded, true);
    t.checkExpect(this.cell4.flooded, true);
  }

  // test generateStopWatch in the class Game
  void testGeneratestopWatch(Tester t) {
    reset();
    this.game1.stopWatch = 61;
    t.checkExpect(this.game1.generateStopWatch(), 
        new TextImage("1:01", 30, Color.black));
    this.game1.stopWatch = 0.5;
    t.checkExpect(this.game1.generateStopWatch(), 
        new TextImage("0:00", 30, Color.black));
    this.game1.stopWatch = 40;
    t.checkExpect(this.game1.generateStopWatch(), 
        new TextImage("0:40", 30, Color.black));
  }


  // testBigBang
  void testBigBang(Tester t) {
    Game g = new Game(22, 6);
    int width = 700;
    int height = 700;
    g.bigBang(width, height, Game.TICK_RATE);
  }
}
