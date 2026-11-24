// PriorityLab.java — S7.06: talking to product in their unit: COST OF DELAY.
// Which order of the backlog costs less? FIFO, "biggest value first", "shortest first" or CD3 (cost of delay / duration)?
// Brute force over all 720 orders confirms which rule is optimal for THIS backlog.
//   java PriorityLab.java            -> the backlog with the debt item's cost of delay quantified
//   java PriorityLab.java nodebtcod  -> same backlog, but nobody quantified the debt (cost of delay = 0)
//   java PriorityLab.java sliced     -> the debt cut in two slices (deprecate now, remove later)
// ALL NUMBERS ARE PREMISES (invented, declared): cost of delay in thousand R$ per week; duration in team-weeks.

import java.util.*;

public class PriorityLab {

    record Item(String name, double costOfDelayPerWeek, double weeks) {
        double cd3() { return costOfDelayPerWeek / weeks; }
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "quantified";
        List<Item> backlog = new ArrayList<>(List.of(       // in the order it arrived (FIFO)
                new Item("Recurring transfers v1 (RFC-0001)", 8.0, 10.0),
                new Item("Statement dashboard redesign", 3.0, 4.0),
                new Item("Minimal landing zone (FND01/FND02 HIGH)", 4.0, 3.0)));
        switch (mode) {
            case "nodebtcod" -> backlog.add(new Item("Remove legacy /entries path (debt)", 0.0, 2.0));
            case "sliced" -> {   // same debt, cut in two: most of the cost of delay is the risk of new clients on /entries
                backlog.add(new Item("Deprecation+Sunset on /entries, usage metric (debt 1/2)", 1.0, 0.5));
                backlog.add(new Item("Remove /entries after clients moved (debt 2/2)", 0.5, 1.5));
            }
            default -> backlog.add(new Item("Remove legacy /entries path (debt)", 1.5, 2.0));
        }
        backlog.add(new Item("T21: 403 -> 503 when Keycloak is down", 3.0, 0.5));
        backlog.add(new Item("Spring Boot patch upgrade (CVE fixes)", 2.0, 0.5));

        System.out.println("item                                                    | CoD (R$ k/week) | weeks | CD3");
        for (Item i : backlog)
            System.out.printf("%-55s | %15.1f | %5.1f | %5.2f%n", i.name(), i.costOfDelayPerWeek(), i.weeks(), i.cd3());

        Map<String, List<Item>> orders = new LinkedHashMap<>();
        orders.put("FIFO (as it arrived)", backlog);
        orders.put("highest cost of delay first", sorted(backlog, Comparator.comparingDouble(Item::costOfDelayPerWeek).reversed()));
        orders.put("shortest first", sorted(backlog, Comparator.comparingDouble(Item::weeks)));
        orders.put("CD3 (cost of delay / duration)", sorted(backlog, Comparator.comparingDouble(Item::cd3).reversed()));

        System.out.println("\norder                           | total cost of delay | first three");
        for (var e : orders.entrySet())
            System.out.printf("%-31s | R$ %8.1f k       | %s%n", e.getKey(), totalCost(e.getValue()), names(e.getValue(), 3));

        List<List<Item>> all = new ArrayList<>();
        permute(new ArrayList<>(backlog), 0, all);
        List<Item> best = all.stream().min(Comparator.comparingDouble(PriorityLab::totalCost)).orElseThrow();
        double worst = all.stream().mapToDouble(PriorityLab::totalCost).max().orElseThrow();
        double cd3Cost = totalCost(orders.get("CD3 (cost of delay / duration)"));
        long optimal = all.stream().filter(o -> Math.abs(totalCost(o) - totalCost(best)) < 1e-9).count();
        System.out.printf("%nBrute force over %d orders: best R$ %.1f k, worst R$ %.1f k; CD3 order %s the best; %d order(s) tie for the best (items with equal CD3 can swap)%n",
                all.size(), totalCost(best), worst, Math.abs(cd3Cost - totalCost(best)) < 1e-9 ? "IS" : "is NOT", optimal);
        List<Item> cd3Order = orders.get("CD3 (cost of delay / duration)");
        for (Item debt : backlog) if (debt.name().contains("debt"))
            System.out.printf("Debt item '%s' is #%d of %d in the CD3 order.%n", debt.name(), cd3Order.indexOf(debt) + 1, backlog.size());
    }

    static List<Item> sorted(List<Item> l, Comparator<Item> c) { List<Item> x = new ArrayList<>(l); x.sort(c); return x; }

    /** Sum over items of costOfDelay x week in which the item is finished (one team, one item at a time). */
    static double totalCost(List<Item> order) {
        double t = 0, cost = 0;
        for (Item i : order) { t += i.weeks(); cost += i.costOfDelayPerWeek() * t; }
        return cost;
    }

    static String names(List<Item> l, int n) {
        return String.join(" > ", l.stream().limit(n).map(i -> i.name().split(" ")[0] + (i.name().contains("debt") ? "(debt)" : "")).toList());
    }

    static void permute(List<Item> a, int k, List<List<Item>> out) {
        if (k == a.size()) { out.add(List.copyOf(a)); return; }
        for (int i = k; i < a.size(); i++) { Collections.swap(a, k, i); permute(a, k + 1, out); Collections.swap(a, k, i); }
    }
}
