package zyc;

import com.sun.deploy.util.StringUtils;

import java.util.*;

public class FA {
    private Set<State> K;
    private State s;
    private Set<State> F;
    private Set<String> alphabet;
    // self circle will be put into selfCircle map
    private Map<State, Map<State, Set<String>>> edges;
    // save all states that reads a certain input and jump to a certain state
    private Map<State, Map<String, Set<State>>> reverseMap;

    private boolean min = false;
    private boolean gen = false;
    private String regEx;

    private Map<State, Set<String>> selfCircle = new HashMap<>();

    public FA(Set<State> K, State s, Set<State> F, Set<String> alphabet, Map<State, Map<State, Set<String>>> e) {
        this.K = K;
        this.s = s;
        this.F = F;
        this.alphabet = alphabet;
        edges = new HashMap<>();
        e.forEach((from, map) -> map.forEach((to, paths) -> {
            if (from == to) {
                if (!selfCircle.containsKey(from)) {
                    selfCircle.put(from, new HashSet<>());
                }
                selfCircle.get(from).addAll(paths);
            } else {
                if (!edges.containsKey(from)) {
                    edges.put(from, new HashMap<>());
                }
                edges.get(from).putAll(e.get(from));
            }
        }));
        reverseMap = genReverseMap(e);
    }

    private void minimize() {
        if (min) {
            return;
        }
        Set<Set<State>> P = new HashSet<>();
        Set<Set<State>> W = new HashSet<>();
        Set<State> tmp = new HashSet<>(K);
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
                if (X.isEmpty() || X.size() == K.size()) {
                    continue;
                }

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

        Map<State, State> mapToNewState = new HashMap<>();

        // new full set of states
        Set<State> newK;
        // new start state
        State newS;
        // new end states
        Set<State> newF = new HashSet<>();
        // new edges
        Map<State, Map<State, Set<String>>> newEdges = new HashMap<>();

        for (Set<State> part : P) {
            State newState = new State();
            for (State st : part) {
                mapToNewState.put(st, newState);
            }
        }

        newK = new HashSet<>(mapToNewState.values());
        newS = mapToNewState.get(s);
        for (State st : F) {
            newF.add(mapToNewState.get(st));
        }

        edges.forEach((originFrom, value) -> value.forEach((originTo, paths) -> {
            State from = mapToNewState.get(originFrom);
            State to = mapToNewState.get(originTo);

            if (!newEdges.containsKey(from)) {
                newEdges.put(from, new HashMap<>());
            }

            if (!newEdges.get(from).containsKey(to)) {
                newEdges.get(from).put(to, new HashSet<>());
            }

            newEdges.get(from).get(to).addAll(paths);
        }));

        K = newK;
        s = newS;
        F = newF;
        edges = newEdges;

        min = true;
    }

    // generate regular expression corresponding to the FA
    public String genRegEx() {
        if (gen) return regEx;
        minimize();
        // add new start state and end state, and relevant edges
        State newS = new State();
        State newF = new State();
        Map<State, Set<String>> startMap = new HashMap<>();
        startMap.put(s, new HashSet<>(Collections.singletonList("")));
        edges.put(newS, startMap);
        for (State state : F) {
            if (!edges.containsKey(state)) {
                edges.put(state, new HashMap<>());
            }
            edges.get(state).put(newF, new HashSet<>(Collections.singletonList("")));
        }

        Map<State, Integer> inDegree = new HashMap<>();
        Map<State, Integer> outDegree = new HashMap<>();

        edges.forEach((from, value) -> (value).forEach((to, edges) -> {
            outDegree.put(from, outDegree.getOrDefault(from, 0) + 1);
            inDegree.put(to, inDegree.getOrDefault(to, 0) + 1);
        }));

        while (!K.isEmpty()) {
            State toBeRemoved = K.iterator().next();
            for (State st : K) {
                if (!inDegree.containsKey(st) || !outDegree.containsKey(st)) continue;
                if (inDegree.get(st) == 1 && outDegree.get(st) == 1) {
                    toBeRemoved = st;
                    break;
                }
            }
            K.remove(toBeRemoved);

            String mid = "";
            if (selfCircle.containsKey(toBeRemoved)) {
                mid = "(" + join(selfCircle.get(toBeRemoved)) + ")*";
                selfCircle.remove(toBeRemoved);
            }

            for (State from : edges.keySet()) {
                if (!edges.get(from).containsKey(toBeRemoved)) continue;
                String before = join(edges.get(from).get(toBeRemoved));
                edges.get(from).remove(toBeRemoved);
                outDegree.put(from, outDegree.get(from) - 1);
                for (State to : edges.get(toBeRemoved).keySet()) {
                    inDegree.put(to, inDegree.get(to) - 1);
                    String after = join(edges.get(toBeRemoved).get(to));
                    String combine = before + mid + after;
                    if (from.equals(to)) {
                        if (!selfCircle.containsKey(from)) {
                            selfCircle.put(from, new HashSet<>());
                        }
                        selfCircle.get(from).add(combine);
                    } else {
                        if (!edges.get(from).containsKey(to)) {
                            edges.get(from).put(to, new HashSet<>());
                            outDegree.put(from, outDegree.get(from) + 1);
                            inDegree.put(to, inDegree.get(to) + 1);
                        }
                        edges.get(from).get(to).add(combine);
                    }
                }
            }
            edges.remove(toBeRemoved);
        }

        regEx = join(edges.get(newS).get(newF));
        gen = true;
        return regEx;
    }

    private String join(Set<String> paths) {
        if (paths.size() > 1) {
            return "(" + StringUtils.join(paths, "|") + ")";
        } else {
            return paths.iterator().next();
        }
    }

    private Map<State, Map<String, Set<State>>> genReverseMap(Map<State, Map<State, Set<String>>> e) {
        Map<State, Map<String, Set<State>>> res = new HashMap<>();

        e.forEach((from, value) -> value.forEach((to, paths) -> {
            if (!res.containsKey(to)) {
                res.put(to, new HashMap<>());
            }
            for (String path : paths) {
                if (!res.get(to).containsKey(path)) {
                    res.get(to).put(path, new HashSet<>());
                }
                res.get(to).get(path).add(from);
            }
        }));

        return res;
    }
}
