package algorithm.HashMap;

import org.junit.Test;

public class Main {

    @Test
    public void test() {
        long before = System.currentTimeMillis();
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put("" + i, i);
        }
        for (int i = 0; i < 1000; i++) {
           System.out.println( map.containsVal(i));
        }
//        for (int i = 0; i < 1000; i++) {
//            System.out.println(map.get("" + i));
//        }
        System.out.println(System.currentTimeMillis() - before);
    }

    @Test
    public void test2() {
        long before = System.currentTimeMillis();
        java.util.HashMap<String, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < 1000000; i++) {
            map.put("" + i, i);
        }
        for (int i = 0; i < 1000000; i++) {
            System.out.println(map.get("" + i));
        }
        System.out.println(System.currentTimeMillis() - before);
    }

    @Test
    public void test3() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put(null, 1);
        map.put("test2", 2);
        System.out.println(map.get(null));
    }

    @Test
    public void test4() {
        java.util.HashMap<String, Integer> map = new java.util.HashMap<>();
        map.put("test", 1);
        map.put("test", 2);
    }

}
