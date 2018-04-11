package zyc;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {
    Map<String, TrieNode> next;
    boolean isEnd;

    TrieNode() {
        next = new HashMap<>();
        isEnd = false;
    }
}
