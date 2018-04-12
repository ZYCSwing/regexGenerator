package zyc;

import java.util.*;

public class Util {

    public static FA createFAFromTrie(Trie t) {
        Set<State> K = new HashSet<>();
        State s = new State();
        Set<State> F = new HashSet<>();
        Set<String> alphabet = new HashSet<>();
        Map<State, Map<State, Set<String>>> edges = new HashMap<>();

        Map<TrieNode, State> map = new HashMap<>();

        Queue<TrieNode> que = new LinkedList<>();
        que.offer(t.root);
        map.put(t.root, s);
        K.add(s);
        while (!que.isEmpty()) {
            TrieNode cur = que.poll();
            State from = map.get(cur);
            edges.put(from, new HashMap<>());

            assert cur != null;
            for (Map.Entry entry : cur.next.entrySet()) {
                String edge = (String) entry.getKey();
                TrieNode child = (TrieNode) entry.getValue();
                State st = new State();
                map.put(child, st);
                K.add(st);
                alphabet.add(edge);
                if (child.isEnd) {
                    F.add(st);
                }
                edges.get(from).put(st, new HashSet<>(Collections.singletonList(edge)));
                que.offer(child);
            }
        }

        return new FA(K, s, F, alphabet, edges);
    }
}
