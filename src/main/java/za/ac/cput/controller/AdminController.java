package za.ac.cput.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.repository.IBookRepository;
import za.ac.cput.repository.IUserRepository;
import za.ac.cput.repository.IOrderRepository;
import za.ac.cput.repository.IPaymentRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final IBookRepository bookRepository;
    private final IUserRepository userRepository;
    private final IOrderRepository orderRepository;
    private final IPaymentRepository paymentRepository;

    public AdminController(IBookRepository bookRepository, IUserRepository userRepository, IOrderRepository orderRepository, IPaymentRepository paymentRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/db-counts")
    public Map<String, Long> getDbCounts() {
        long books = bookRepository.count();
        long users = userRepository.count();
        long orders = orderRepository.count();
        long payments = paymentRepository.count();
        return Map.of(
                "books", books,
                "users", users,
                "orders", orders,
                "payments", payments
        );
    }
}
