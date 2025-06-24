import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.ArrayList;

public class GameField {
  private static final String RED = "\u001B[31m";
  private static final String CYAN = "\u001b[36m";
  private static final String YELLOW = "\u001b[33m";
  private static final String GREEN = "\u001b[32m";
  private static String[][] colorField;
  private static boolean[][] deletedField;
  private static boolean[][] field;

  public record TerminalSize(int rows, int columns) {
    public static TerminalSize createDefault() {
      return new TerminalSize(1000, 500);
    }
  }

  GameField(TerminalSize terminalSize) {
    field = initialize2dBoolArray(terminalSize.rows(), terminalSize.columns(), true);
    deletedField = initialize2dBoolArray(terminalSize.rows(), terminalSize.columns(), true);
    colorField = initialize2dStringArray(terminalSize.rows(), terminalSize.columns());
  }

  private boolean[][] initialize2dBoolArray(int rows, int cols, boolean randomize) {
    var random = new Random();
    var field = new boolean[rows][cols];
    for (int x = 0; x < rows; x++) {
      for (int y = 0; y < cols; y++) {
        if (randomize) {
          field[x][y] = random.nextBoolean();
        } else {
          field[x][y] = false;
        }
      }
    }
    return field;
  }

  private String[][] initialize2dStringArray(int rows, int cols) {
    var field = new String[rows][cols];
    for (int x = 0; x < rows; x++) {
      for (int y = 0; y < cols; y++) {
        field[x][y] = "";
      }
    }
    return field;
  }

  private boolean[][] initialize2dBoolArray(int rows, int cols) {
    return initialize2dBoolArray(rows, cols, false);
  }

  private int countNeighbors(int x, int y) {
    var count = 0;
    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        if (i != 0 || j != 0) {
          count += field[((x + field.length + i) % field.length)][((y + field[0].length + j) % field[0].length)] ? 1
              : 0;
        }
      }
    }
    return count;
  }

  private void colorNeighbors(String[][] colorField, int x, int y, String color) {
    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        colorField[((x + field.length + i) % field.length)][((y + field[0].length + j) % field[0].length)] = color;
      }
    }
  }

  private void generate() {
    var newField = initialize2dBoolArray(field.length, field[0].length);
    var newDeletedField = initialize2dBoolArray(field.length, field[0].length);
    var newColorField = initialize2dStringArray(field.length, field[0].length);
    IntStream.range(0, field.length).parallel().forEach(x -> {
      IntStream.range(0, field[0].length).parallel().forEach(y -> {
        var aliveCount = countNeighbors(x, y);
        if (field[x][y]) {
          if (aliveCount < 2 || aliveCount > 3) {
            if (aliveCount < 2) {
              colorNeighbors(newColorField, x, y, RED);
            } else {
              colorNeighbors(newColorField, x, y, YELLOW);
            }
            newColorField[x][y] = RED;
            newDeletedField[x][y] = true;
            newField[x][y] = false;
          } else {
            newColorField[x][y] = CYAN;
            newField[x][y] = field[x][y];
          }
        } else {
          if (aliveCount == 3) {
            newColorField[x][y] = GREEN;
            newField[x][y] = true;
          } else {
            newColorField[x][y] = CYAN;
            newField[x][y] = field[x][y];
          }
        }
      });
    });
    field = newField;
    deletedField = newDeletedField;
    colorField = newColorField;
  }

  Stream<String> getFieldAsList(int rowz, int cols) {
    generate();
    List<String> rows = new ArrayList<>();
    for (int x = 0; x < rowz - 1; x++) {
      StringBuilder line = new StringBuilder();
      line.append('\r');
      for (int y = 0; y < cols - 1; y++) {
        line.append(colorField[x][y]);
        if (field[x][y]) {
          line.append('▮');
          // line.append('#');
        } else if (deletedField[x][y]) {
          line.append(deletedField[x][y] ? '.' : ' ');
        } else {
          line.append(' ');
        }
      }
      line.append('\n');
      rows.add(line.toString());
    }
    return rows.stream();
  }
}
