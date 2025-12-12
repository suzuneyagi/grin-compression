package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode(String infile, String outfile) throws IOException {
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);
 
        if (in.readBits(32) != 0x736) {
            throw new IllegalArgumentException();
        } else {
            HuffmanTree tree = new HuffmanTree(in);
            tree.decode(in, out);
            out.close();
            in.close();
        }
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a freqency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws IOException {
        BitInputStream in = new BitInputStream(file);
        // Make frequency map
        Map<Short, Integer> freqs = new HashMap<>();

        // To store the read characters
        // ArrayList<Short> contents = new ArrayList<>();
        
        int ch;
        while ((ch = in.readBits(8)) != -1) {
            short character = (short) ch;
            // contents.add(character);

            if (freqs.containsKey(character)) {
                int frequency = freqs.get(character) + 1;
                freqs.put(character, frequency);
            } else {
                freqs.put(character, 1);
            }
        }
        freqs.put((short) 256, 1);

        in.close();

        return freqs;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws IOException {
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        Map<Short, Integer> freqs = createFrequencyMap(infile);
        HuffmanTree tree = new HuffmanTree(freqs);

        // Magic number
        out.writeBits(0x736, 32);

        // Serialized tree
        tree.serialize(out);

        tree.encode(in, out);

        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        } else if (args[0].equals("encode")) {
            encode(args[1], args[2]);
        } else if (args[0].equals("decode")) {
            decode(args[1], args[2]);
        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }
    }
}
