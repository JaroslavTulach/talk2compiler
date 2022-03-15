
import java.io.File;

class CheckDirectory {
    private CheckDirectory() {
    }

    public static void main(String... args) throws Exception {
        File file = new File(args[0]).getAbsoluteFile();
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "dir":
                    if (file.isDirectory()) {
                        continue;
                    }
                    break;
                case "none":
                    if (!file.exists()) {
                        continue;
                    }
                    break;
                case "move":
                    if (file.renameTo(new File(file.getParentFile(), file.getName() + ".bak"))) {
                        continue;
                    }
                    break;
            }
            System.err.println("Check #" + i + " for " + args[i] + " fails for " + file);
            System.exit(1);
        }
    }
}
