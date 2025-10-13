package za.ac.cput.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.domain.Order;
import za.ac.cput.domain.OrderItem;
import za.ac.cput.repository.IOrderRepository;

import java.util.*;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportsController {

    @Autowired
    private IOrderRepository orderRepository;

    @GetMapping("/sales")
    public Map<String, Object> salesReport() {
        List<Order> orders = orderRepository.findAll();

        double totalRevenue = 0.0;
        int totalQuantitySold = 0;
        Map<String, Map<String, Object>> topBooks = new HashMap<>();
        Map<String, Double> revenueByGenre = new HashMap<>();
        Map<String, Integer> ordersByStatus = new HashMap<>();

        long now = System.currentTimeMillis();
        long oneDay = 1000L * 60 * 60 * 24;
        long oneWeek = oneDay * 7;
        long oneMonth = oneDay * 30;
        int ordersToday = 0, ordersThisWeek = 0, ordersThisMonth = 0;

        for (Order order : orders) {
            if (order == null) continue;

            // Use order total if available, otherwise compute from items
            double orderTotal = order.getTotalAmount();
            // If totalAmount is zero, fallback to summing items
            if (orderTotal <= 0.0 && order.getItems() != null) {
                orderTotal = order.calculateTotal();
            }

            totalRevenue += orderTotal;

            // status counts
            String status = order.getStatus() != null ? order.getStatus() : "Unknown";
            ordersByStatus.put(status, ordersByStatus.getOrDefault(status, 0) + 1);

            // time windows
            Date od = order.getOrderDate();
            if (od != null) {
                long then = od.getTime();
                long age = now - then;
                if (age <= oneDay) ordersToday++;
                if (age <= oneWeek) ordersThisWeek++;
                if (age <= oneMonth) ordersThisMonth++;
            }

            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                int qty = item.getQuantity();
                double revenue = item.getPrice() * qty;
                totalQuantitySold += qty;

                // top books aggregation by title
                String title = (item.getBook() != null) ? item.getBook().getTitle() : "Unknown";
                topBooks.putIfAbsent(title, new HashMap<>());
                Map<String, Object> b = topBooks.get(title);
                b.put("title", title);
                b.put("quantitySold", ((int) b.getOrDefault("quantitySold", 0)) + qty);
                b.put("revenue", ((double) b.getOrDefault("revenue", 0.0)) + revenue);

                // revenue by genre
                String genre = (item.getBook() != null && item.getBook().getGenre() != null) ? item.getBook().getGenre() : "Unknown";
                revenueByGenre.put(genre, revenueByGenre.getOrDefault(genre, 0.0) + revenue);
            }
        }

        List<Map<String, Object>> topBooksList = new ArrayList<>(topBooks.values());
        topBooksList.sort((a, b) -> Integer.compare((int) b.getOrDefault("quantitySold", 0), (int) a.getOrDefault("quantitySold", 0)));

        int totalOrders = orders.size();
        double averageOrderValue = totalOrders > 0 ? (totalRevenue / totalOrders) : 0.0;

        // round revenue fields to 2 decimals
        totalRevenue = Math.round(totalRevenue * 100.0) / 100.0;
        averageOrderValue = Math.round(averageOrderValue * 100.0) / 100.0;

        Map<String, Object> report = new HashMap<>();
        report.put("totalRevenue", totalRevenue);
        report.put("totalQuantitySold", totalQuantitySold);
        report.put("topBooks", topBooksList);
        report.put("totalOrders", totalOrders);
        report.put("averageOrderValue", averageOrderValue);
        report.put("revenueByGenre", revenueByGenre);
        report.put("ordersByStatus", ordersByStatus);
        report.put("ordersToday", ordersToday);
        report.put("ordersThisWeek", ordersThisWeek);
        report.put("ordersThisMonth", ordersThisMonth);

        return report;
    }
}
