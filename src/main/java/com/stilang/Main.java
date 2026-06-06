package com.stilang;

import com.stilang.ast.ParseException;
import com.stilang.scope_resolution.ResolveException;
import com.stilang.type_checking.TypeException;
import com.stilang.emitter.EmitException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        // ── Argument validation ───────────────────────────────────────────────
        if (args.length < 1) {
            System.err.println("Usage: stilang <source.stil> [output.c]");
            System.err.println("  If no output file is given, prints to stdout.");
            System.exit(1);
        }

        Path inputPath  = Path.of(args[0]);
        Path outputPath = args.length >= 2 ? Path.of(args[1]) : null;

        // ── Read source ───────────────────────────────────────────────────────
        String source;
        try {
            source = Files.readString(inputPath);
        } catch (IOException e) {
            System.err.println("Error: cannot read '" + inputPath + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        // ── Compile ───────────────────────────────────────────────────────────
        String cSource;
        try {
            cSource = new Compiler().compile(source);
        } catch (ParseException e) {
            System.err.println("Syntax error: " + e.getMessage());
            System.exit(1);
            return;
        } catch (ResolveException e) {
            System.err.println("Name error: " + e.getMessage());
            System.exit(1);
            return;
        } catch (TypeException e) {
            System.err.println("Type error: " + e.getMessage());
            System.exit(1);
            return;
        } catch (EmitException e) {
            System.err.println("Emit error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ── Write output ──────────────────────────────────────────────────────
        if (outputPath != null) {
            // Write generated C
            try {
                Files.writeString(outputPath, cSource);
            } catch (IOException e) {
                System.err.println("Error: cannot write '" + outputPath + "': " + e.getMessage());
                System.exit(1);
                return;
            }

            // Extract runtime.c silently next to the output file
            Path runtimePath = outputPath.resolveSibling("runtime.c");
            try {
                extractRuntime(runtimePath);
            } catch (IOException e) {
                System.err.println("Warning: could not extract runtime.c: " + e.getMessage());
            }

            // Tell the user exactly what to run next, nothing more
            String outName     = outputPath.getFileName().toString();
            String programName = outName.replaceAll("\\.c$", "");
            System.out.println("Compiled " + inputPath + " -> " + outputPath);
            System.out.println("Run:  gcc " + outName + " runtime.c -o " + programName);

        } else {
            // No output path — print C source to stdout for inspection
            System.out.print(cSource);
        }
    }

    /**
     * Extract the bundled runtime.c from inside the jar to the given path.
     * Does nothing if runtime.c already exists there — never overwrites.
     */
    private static void extractRuntime(Path destination) throws IOException {
        if (Files.exists(destination)) return;

        InputStream in = Main.class.getResourceAsStream("/runtime.c");
        if (in == null) {
            throw new IOException("runtime.c not found inside jar — was the jar built correctly?");
        }
        try (in) {
            Files.copy(in, destination);
        }
    }
}
