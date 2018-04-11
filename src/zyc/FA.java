package zyc;

import java.util.*;

public class FA {
    private Set<State> K;
    private State s;
    private Set<State> F;
    private Set<String> alphabet;
    // self circle will be put into selfCircle map
    private Map<State, Map<State, String>> edges;
    // save all states that reads a certain input and jump to a certain state
    private Map<State, Map<String, Set<State>>> reverseMap = new HashMap<>();

    private boolean min = false;
    private boolean gen = false;
    private String regEx;

    private Map<State, String> selfCircle = new HashMap<>();
    private Map<State, Map<State, String>> e;

    public FA(Set<State> K, State s, Set<State> F, Set<String> alphabet, Map<State, Map<State, String>> edges) {
        this.K = K;
        this.s = s;
        this.F = F;
        this.alphabet = alphabet;
        this.edges = edges;
        reverseMap = genReverseMap(edges);
    }

    private void minimize() {
        if (min) return;
        Set<Set<State>> P = new HashSet<>();
        Set<Set<State>> W = new HashSet<>();
        Set<State> tmp = new HashSet<>();
        tmp.addAll(K);
        tmp.removeAll(F);
        P.add(tmp);
        P.add(F);
        W.add(F);

        while (!W.isEmpty()) {
            Set<State> A = W.iterator().next();
            W.remove(A);
            for (String str : alphabet) {
                Set<State> X = new HashSet<>();
                for (State st : A) {
                    if (reverseMap.containsKey(st) && reverseMap.get(st).containsKey(str)) {
                        X.addAll(reverseMap.get(st).get(str));
                    }
                }

                // can't refine
                if (X.isEmpty() || X.size() == K.size()) continue;

                Set<Set<State>> newP = new HashSet<>();
                for (Set<State> Y : P) {
                    Set<State> in = new HashSet<>();
                    Set<State> out = new HashSet<>();
                    for (State st : Y) {
                        if (X.contains(st)) {
                            in.add(st);
                        } else {
                            out.add(st);
                        }
                    }
                    if (in.isEmpty() || out.isEmpty()) {
                        newP.add(Y);
                    } else {
                        newP.add(in);
                        newP.add(out);
                        if (W.contains(Y)) {
                            W.remove(Y);
                            W.add(in);
                            W.add(out);
                        } else {
                            if (in.size() <= out.size()) {
                                W.add(in);
                            } else {
                                W.add(out);
                            }
                        }
                    }
                }
                P = newP;
            }
        }

        Map<State, Set<State>> originStateToGroup = new HashMap<>();
        Map<Set<State>, State> groupToNewState = new HashMap<>();


        // new full set of states
        Set<State> newK;
        // new start state
        State newS;
        // new end states
        Set<State> newF = new HashSet<>();
        // new edges
        Map<State, Map<State, String>> newEdges = new HashMap<>();

        for (Set<State> part : P) {
            for (State st : part) {
                originStateToGroup.put(st, part);
            }
            groupToNewState.put(part, new State());
        }

        newK = new HashSet<>(groupToNewState.values());
        newS = groupToNewState.get(originStateToGroup.get(s));
        for (State st : F) {
            newF.add(groupToNewState.get(originStateToGroup.get(st)));
        }

        for (State st : K) {
            State from = groupToNewState.get(originStateToGroup.get(st));
            if (!newEdges.containsKey(from)) {
                newEdges.put(from, new HashMap<>());
            }
            for (State originTo : edges.get(st).keySet()) {
                State to = groupToNewState.get(originStateToGroup.get(originTo));
                if (newEdges.get(from).containsKey(to)) {
                    if (!newEdges.get(from).get(to).equals(edges.get(st).get(originTo))) {
                        newEdges.get(from).put(to, newEdges.get(from).get(to) + "|" + edges.get(st).get(originTo));
                    }
                } else {
                    newEdges.get(from).put(to, edges.get(st).get(originTo));
                }
            }
        }

        K = newK;
        s = newS;
        F = newF;
        edges = newEdges;
        reverseMap = genReverseMap(edges);

        min = true;
    }

    // generate regular expression corresponding to the FA
    public String genRegEx() {
        if (gen) return regEx;
        minimize();
        // add new start state and end state, and relevant edges
        State newS = new State();
        State newF = new State();
        Map<State, String> startMap = new HashMap<>();
        startMap.put(s, "");
        edges.put(newS, startMap);
        for (State state : F) {
            edges.get(state).put(newF, "");
        }

        Map<State, Integer> inDegree = new HashMap<>();
        Map<State, Integer> outDegree = new HashMap<>();

        for (Map.Entry<State, Map<State, String>> entry : edges.entrySet()) {
            State from = entry.getKey();
            (entry.getValue()).forEach((to, edge) -> {
                outDegree.put(from, outDegree.getOrDefault(from, 0) + 1);
                inDegree.put(to, inDegree.getOrDefault(to, 0) + 1);
            });
        }

        while (!K.isEmpty()) {
            State toBeRemoved = K.iterator().next();;
            for (State st : K) {
                if (inDegree.get(st) == 1 && outDegree.get(st) == 1) {
                    toBeRemoved = st;
                    break;
                }
            }
            K.remove(toBeRemoved);

            String mid = "";
            if (selfCircle.containsKey(toBeRemoved)) {
                mid = "(" + selfCircle.get(toBeRemoved) + ")*";
                selfCircle.remove(toBeRemoved);
            }

            for (State from : edges.keySet()) {
                if (!edges.get(from).containsKey(toBeRemoved)) continue;
                String before = edges.get(from).get(toBeRemoved);
                if (isCombine(before)) before = "(" + before + ")";
                edges.get(from).remove(toBeRemoved);
                outDegree.put(from, outDegree.get(from) - 1);
                for (State to : edges.get(toBeRemoved).keySet()) {
                    inDegree.put(to, inDegree.get(to) - 1);
                    String after = edges.get(toBeRemoved).get(to);
                    if (isCombine(after)) after = "(" + after + ")";
                    String combine = before + mid + after;
                    if (from.equals(to)) {
                        if (selfCircle.containsKey(from)) {
                            if (!from.equals(combine)) {
                                selfCircle.put(from, selfCircle.get(from) + "|" + combine);
                            }
                        } else {
                            selfCircle.put(from, combine);
                        }
                    } else {
                        if (edges.get(from).containsKey(to)) {
                            if (!edges.get(from).get(to).equals(combine)) {
                                edges.get(from).put(to, edges.get(from).get(to) + "|" + combine);
                            }
                        } else {
                            edges.get(from).put(to, combine);
                            outDegree.put(from, outDegree.get(from) + 1);
                            inDegree.put(to, inDegree.get(to) + 1);
                        }
                    }
                }
            }
            edges.remove(toBeRemoved);
        }

        regEx = edges.get(newS).get(newF);
        gen = true;
        return regEx;
    }

    private boolean isCombine(String str) {
        int cnt = 0;
        for (char ch : str.toCharArray()) {
            if (cnt > 0) {
                if (ch == ')') {
                    --cnt;
                } else if (ch == '(') {
                    ++cnt;
                }
            } else {
                if (ch == '(') {
                    ++cnt;
                } else {
                    if (ch == '|') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Map<State, Map<String, Set<State>>> genReverseMap(Map<State, Map<State, String>> e) {
        this.e = e;
        Map<State, Map<String, Set<State>>> res = new HashMap<>();

        for (Map.Entry<State, Map<State, String>> entry : e.entrySet()) {
            State from = entry.getKey();
            for (Map.Entry<State, String> innerEntry : (entry.getValue()).entrySet()) {
                State to = innerEntry.getKey();
                String edge = innerEntry.getValue();
                if (!res.containsKey(to)) {
                    res.put(to, new HashMap<>());
                }
                if (!res.get(to).containsKey(edge)) {
                    res.get(to).put(edge, new HashSet<>());
                }
                res.get(to).get(edge).add(from);
            }
        }

        return res;
    }
}
