import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Optional;

public class Runner {
  private static final int FRAME_RATE = 20;
  private static final String RESET_CURSOR = "\u001b[0;0H";
  private static final String SAVE_CURSOR_POS = "\u001b[s";
  private static final String MOVE_CURSOR_OFF = "\u001b[5000;5000H";
  private static final String REQUEST_CURSOR_POS = "\u001b[6n";

  private final GameField field;
  private final PrintStream stream = System.out;
  private StringBuffer buffer = new StringBuffer(100);
  private Optional<Integer> cols = Optional.empty();
  private Optional<Integer> rows = Optional.empty();
  private boolean shouldQuit = false;

  public Runner() throws InterruptedException {
    Thread.startVirtualThread(this::listen);
    while (cols.isEmpty() && rows.isEmpty()) {
      try {
        Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "stty raw -echo </dev/tty" }).waitFor();
        while (cols.isEmpty() && rows.isEmpty()) {
          determineTerminalSize();
          Thread.sleep(FRAME_RATE);
        }
      } catch (InterruptedException | IOException e) {
        e.printStackTrace();
      }
    }
    field = new GameField(new GameField.TerminalSize(rows.get(), cols.get()));
  }

  private void determineTerminalSize() {
    writeToTerminal(SAVE_CURSOR_POS);
    writeToTerminal(MOVE_CURSOR_OFF);
    writeToTerminal(REQUEST_CURSOR_POS);

    synchronized (buffer) {
      var output = buffer.toString();
      if (output.split(";").length > 2) {
        output = output.split("\\[")[1];
      }
      if (output.split(";").length > 1) {
        var rows = output.split(";")[0];
        var cols = output.split(";")[1];
        rows = rows.replaceAll("\\D", "");
        cols = cols.replaceAll("\\D", "");
        this.rows = Optional.of(Integer.valueOf(rows));
        this.cols = Optional.of(Integer.valueOf(cols));
        buffer = new StringBuffer(100);
      }
    }
  }

  private void listen() {
    InputStreamReader reader = new InputStreamReader(System.in);
    try {
      int in;
      while ((in = reader.read()) != -1) {
        if (in == 113) {
          shouldQuit = true;
        }
        if (in >= 32) {
          buffer.append((char) in);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeToTerminal(byte[] output) {
    try {
      stream.write(output);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeToTerminal(String output) {
    writeToTerminal(output.getBytes());
  }

  public void run() throws IOException, InterruptedException {
    while (!shouldQuit) {
      writeToTerminal(RESET_CURSOR);
      field.getFieldAsList(this.rows.get(), this.cols.get()).forEach(this::writeToTerminal);
      Thread.sleep(FRAME_RATE);
    }
    Runtime.getRuntime().exec(new String[] { "reset" }).waitFor();
  }
}
