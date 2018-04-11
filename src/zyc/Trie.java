package zyc;

public class Trie {
    TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public void insert(String str) {
        TrieNode cur = root;
        for (int i = 0; i < str.length(); ++i) {
            if (!cur.next.containsKey(str.substring(i, i + 1))) {
                cur.next.put(str.substring(i, i + 1), new TrieNode());
            }
            cur = cur.next.get(str.substring(i, i + 1));
        }
        cur.isEnd = true;
    }

    public boolean findWord(String str) {
        TrieNode cur = root;
        for (int i = 0; i < str.length(); ++i) {
            if (!cur.next.containsKey(str.substring(i, i + 1))) return false;
            cur = cur.next.get(str.substring(i, i + 1));
        }
        return cur.isEnd;
    }

    public boolean containPrefix(String str) {
        TrieNode cur = root;
        for (int i = 0; i < str.length(); ++i) {
            if (!cur.next.containsKey(str.substring(i, i + 1))) return false;
            cur = cur.next.get(str.substring(i, i + 1));
        }
        return true;
    }
}
