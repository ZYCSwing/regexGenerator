import zyc.FA;
import zyc.Trie;
import zyc.Util;

public class Main {
    public static void main(String[] args) {
        Trie t = new Trie();
        t.insert("Hello");
        t.insert("Hi");

        FA machine = Util.createFAFromTrie(t);
        System.out.println(machine.genRegEx());
    }
}
