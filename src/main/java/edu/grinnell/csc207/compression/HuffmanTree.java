package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    private Node huffmanTree;

    // Two Node class constructor one for leaf, one for internal 
    // isLeaf to know if it's a leaf

    public static class Node implements Comparable<Node> {
        public boolean isLeaf;
        public Short character;
        public int frequency;

        public Node left;
        public Node right;

        //leaf
        public Node(Short character, int frequency) {
            this.isLeaf = true;
            this.character = character;
            this.frequency = frequency;
        }

        // internal node
        public Node(Node left, Node right) {
            this.isLeaf = false;
            this.frequency = left.frequency + right.frequency;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.frequency, other.frequency);
        }
    }


    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        // EOF
        freqs.put((short) 256, 1);

        PriorityQueue<Node> queue = new PriorityQueue<>();

        // Building a leaf node for each of the characters in character frequency map (freqs)
        Set<Map.Entry<Short, Integer>> entrySet = freqs.entrySet();
        for (Map.Entry<Short, Integer> entry : entrySet) {
            Node leaf = new Node(entry.getKey(), entry.getValue());
            queue.add(leaf);
        }

        // Create internal nodes
        while (queue.size() > 1) {
            Node first = queue.poll();
            Node second = queue.poll();
            Node internalNode = new Node(first, second);
            queue.add(internalNode);
        }

        this.huffmanTree = queue.poll();
        
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream) encoded in a serialized format
     */
    public HuffmanTree(BitInputStream in) throws IOException {
        Node node = HuffmanTreeHelper(in);
        this.huffmanTree = node;
    }

    public static Node HuffmanTreeHelper(BitInputStream in) throws IOException {
        int bit = in.readBit();
        if (bit == -1) {
            throw new IOException();
        } if (bit == 0) {
            int value = in.readBits(9);
            if (value != -1) {
                Node leaf = new Node((short) value, 0);
                return leaf;
            } else {
                throw new IOException();
            }
        } else {
            Node left = HuffmanTreeHelper(in);
            Node right = HuffmanTreeHelper(in);
            Node internalNode = new Node(left, right);
            return internalNode;
        }
    }
 
    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        serializeHelper(this.huffmanTree, out);
    }

    /**
     * Recursively execute serialization
     * @param huffmanTree node that is serialized
     * @param out the output file as a BitOutputStream
     */
    public void serializeHelper(Node huffmanTree, BitOutputStream out) {
        if (huffmanTree.isLeaf) {
            // if Node is leaf
            out.writeBit(0);
            out.writeBits(huffmanTree.character, 9);

        } else {
            // if Node is internalNode
            out.writeBit(1);
            serializeHelper(huffmanTree.left, out);
            serializeHelper(huffmanTree.right, out);
        }
    }
   
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {

        // Payload
        // Make a code map
        Map<Short, String> encodeMap = makeEncodeMap(); 

        int ch;
        while ((ch = in.readBits(8)) != -1) {
            String bits = encodeMap.get((short) ch);

            for (int l = 0; l < bits.length(); l++) {
                int bit = bits.charAt(l) - '0';
                out.writeBit(bit);
            }
        }

        // EOF
        String eofBits = encodeMap.get((short) 256);
        for (int i = 0; i < eofBits.length(); i++) {
            out.writeBit(eofBits.charAt(i) - '0');
        }
    }

    /**
     * Make encode map from the huffmantree
     * @return a map that shows the hashcode and the original string
     */
    public Map<Short, String> makeEncodeMap() {
        Map<Short, String> encodeMap = new HashMap<>();
        makeEncodeMapHelper(encodeMap, this.huffmanTree, "");
        return encodeMap;
    }

   /**
    * Encode a map containing <huffmanTree.character, corresponding bits> 
    * @param encodeMap the map of short and string
    * @param huffmanTree huffman tree that is used to make the enocde map
    * @param bits bits of characters
    */
    public void makeEncodeMapHelper(Map<Short, String> encodeMap, Node huffmanTree, String bits) {
        if (huffmanTree.isLeaf) {
            encodeMap.put(huffmanTree.character, bits);
        } else {
            makeEncodeMapHelper(encodeMap, huffmanTree.left, bits + "0");
            makeEncodeMapHelper(encodeMap, huffmanTree.right, bits + "1");
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) throws IOException {
        Node tree = huffmanTree;

        while (true) {
            int bit = in.readBit();
            if (bit == -1) {
                break;
            } else if (bit == 0) {
                tree = tree.left;
            } else if (bit == 1) {
                tree = tree.right;
            }

            if (tree.isLeaf) {
                if (tree.character == 256) {
                    break;
                } else {
                    out.writeBits(tree.character, 8);
                    tree = huffmanTree;
                }
            }
        }
    }
}
