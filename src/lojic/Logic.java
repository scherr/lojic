package lojic;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Directly based off Oleg Kiselyov's Sokuza Kanren (http://okmij.org/ftp/Scheme/misc.html#sokuza-kanren)
// with Java-specific and static-typing-specific implementation changes.

public final class Logic {
    public static final class Pair<Car, Cdr> {
        private final Car car;
        private final Cdr cdr;

        private Pair(Car car, Cdr cdr) {
            this.car = car;
            this.cdr = cdr;
        }

        public Car car() {
            return car;
        }

        public Cdr cdr() {
            return cdr;
        }

        @Override
        public int hashCode() {
            return Objects.hash(car, cdr);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof Pair)) return false;
            Pair<?, ?> pair = (Pair) obj;
            return Objects.equals(car, pair.car) && Objects.equals(cdr, pair.cdr);
        }

        @Override
        public String toString() {
            return "(" + car + ", " + cdr + ")";
        }
    }

    public static class Var {
        private final String name;
        private Var(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Var(" + name + ")";
        }
    }

    public static <T> Function<T, List<T>> fail() {
        return x -> Collections.emptyList();
    }

    public static <T> Function<T, List<T>> succeed() {
        return Collections::singletonList;
    }

    public static <T> Function<T, List<T>> disj(Function<T, List<T>> f1, Function<T, List<T>> f2) {
        return x -> {
            List<T> l = new ArrayList<>(f1.apply(x));
            l.addAll(f2.apply(x));
            return l;
        };
    }

    public static <T> Function<T, List<T>> conj(Function<T, List<T>> f1, Function<T, List<T>> f2) {
        return x -> f1.apply(x).stream().flatMap(y -> f2.apply(y).stream()).collect(Collectors.toList());
    }

    public static Var var(String name) {
        return new Var(name);
    }

    private static Map<Var, Object> emptySubst() {
        return Collections.emptyMap();
    }

    private static Map<Var, Object> extSubst(Var var, Object value, Map<Var, Object> s) {
        Map<Var, Object> m = new HashMap<>(s);
        m.put(var, value);
        return m;
    }

    private static Object lookup(Var var, Map<Var, Object> s) {
        while (s.containsKey(var)) {
            Object value = s.get(var);
            if (value instanceof Var) {
                var = (Var) value;
            } else {
                return value;
            }
        }

        return var;
    }

    public static <Car, Cdr> Pair<Car, Cdr> cons(Car car, Cdr cdr) {
        return new Pair<Car, Cdr>(car, cdr);
    }

    public static <Car, Cdr> Car car(Pair<Car, Cdr> pair) {
        return pair.car();
    }

    public static <Car, Cdr> Cdr cdr(Pair<Car, Cdr> pair) {
        return pair.cdr();
    }

    private static Map<Var, Object> unify(Object t1, Object t2, Map<Var, Object> s) {
        if (t1 instanceof Var) {
            t1 = lookup((Var) t1, s);
        }
        if (t2 instanceof Var) {
            t2 = lookup((Var) t2, s);
        }

        if (t1 == t2) {
            return s;
        } else if (t1 instanceof Var) {
            return extSubst((Var) t1, t2, s);
        } else if (t2 instanceof Var) {
            return extSubst((Var) t2, t1, s);
        } else if (t1 instanceof Pair && t2 instanceof Pair) {
            s = unify(((Pair) t1).car(), ((Pair) t2).car(), s);
            if (s != null) {
                return unify(((Pair) t1).cdr(), ((Pair) t2).cdr(), s);
            }
        } else if (t1.equals(t2)) {
            return s;
        }

        return null;
    }

    public static Function<Map<Var, Object>, List<Map<Var, Object>>> eq(Object t1, Object t2) {
        return s -> {
            Map<Var, Object> u = unify(t1, t2, s);
            if (u != null) {
                return Logic.<Map<Var, Object>>succeed().apply(u);
            } else {
                return Logic.<Map<Var, Object>>fail().apply(s);
            }
        };
    }

    public static List<Map<Var, Object>> run(Function<Map<Var, Object>, List<Map<Var, Object>>> g) {
        return g.apply(emptySubst());
    }

    public static Pair<Object, Object> list(Object... elements) {
        Pair<Object, Object> p = null;
        for (int i = elements.length - 1; i >= 0; i--) {
            p = cons(elements[i], p);
        }

        return p;
    }

    public static Function<Map<Var, Object>, List<Map<Var, Object>>> choice(Object o, Pair<Object, Object> lst) {
        if (lst == null) {
            return Logic.<Map<Var, Object>>fail();
        } else {
            return disj(eq(o, car(lst)), choice(o, (Pair<Object, Object>) cdr(lst)));
        }
    }

    public static Function<Map<Var, Object>, List<Map<Var, Object>>> commonEl(Pair<Object, Object> l1, Pair<Object, Object> l2) {
        Var v = var("v");
        return conj(choice(v, l1), choice(v, l2));
    }

    public static void main(String[] args) {
        List<Object> l =
                disj(
                        disj(fail(), succeed()),
                        conj(
                                disj(
                                        x -> succeed().apply(((Integer) x) + 1),
                                        x -> succeed().apply(((Integer) x) + 10)
                                ),
                                disj(succeed(), succeed())
                        )
                ).apply(100);

        System.out.println(l);

        Map<Var, Object> s;

        Var vx = var("x");
        Var vy = var("y");
        Var vz = var("z");
        Var vq = var("q");

        s = unify(vx, vy, emptySubst());
        System.out.println(s);

        s = unify(vx, 1, s);
        System.out.println(s);

        System.out.println(lookup(vy, s));
        System.out.println(lookup(vx, s));

        s = unify(cons(vx, vy), cons(vy, 1), emptySubst());
        System.out.println(s);

        List<Map<Var, Object>> u;

        u = run(choice(2, list(1, 2, 3)));
        System.out.println(u);

        u = run(choice(10, list(1, 2, 3)));
        System.out.println(u);

        u = run(choice(vx, list(1, 2, 3)));
        System.out.println(u);

        u = run(commonEl(list(1, 2, 3), list(3, 4, 5)));
        System.out.println(u);

        u = run(commonEl(list(1, 2, 3), list(3, 4, 1, 7)));
        System.out.println(u);

        u = run(commonEl(list(11, 2, 3), list(13, 4, 1, 7)));
        System.out.println(u);
    }
}
