package za.ac.cput.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.domain.Book;
import za.ac.cput.domain.Order;
import za.ac.cput.domain.OrderItem;
import za.ac.cput.domain.User;
import za.ac.cput.repository.IBookRepository;
import za.ac.cput.repository.IOrderRepository;
import za.ac.cput.repository.IUserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IBookRepository bookRepository;

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody Order order, @RequestParam Long userId) {
        // Fetch the user from the database
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found for id: " + userId);
        }

        // Validate items list
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order must contain at least one item");
        }

        // Iterate items, attach managed Book entities, check/decrement stock
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getBook() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid order item or book reference");
            }
            Long bookId = item.getBook().getBookId();
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Book not found: " + bookId);
            }
            if (item.getQuantity() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Item quantity must be positive");
            }
            if (book.getQuantity() < item.getQuantity()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Insufficient stock for book: " + book.getTitle());
            }

            // decrement stock and save book
            book.setQuantity(book.getQuantity() - item.getQuantity());
            bookRepository.save(book);

            // replace the item's book reference with the managed entity (important for JPA to persist relationships)
            item.setBook(book);
            // ensure the item references the parent order
            item.setOrder(order);
        }

        order.setUser(user);
        if (order.getOrderDate() == null) {
            order.setOrderDate(new java.util.Date());
        }
        if (order.getOrderTimestamp() == null) {
            order.setOrderTimestamp(java.time.LocalDateTime.now());
        }
        // Recalculate total on the order entity before saving
        order.calculateTotal();

        Order saved = orderRepository.save(order);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @GetMapping("/customer/{customerId}")
    public List<Order> getOrdersByCustomerId(@PathVariable Long customerId) {
        return orderRepository.findByUser_UserId(customerId);
    }

    @GetMapping("/status/{status}")
    public List<Order> getOrdersByStatus(@PathVariable String status) {
        return orderRepository.findByStatus(status);
    }

    @PutMapping("/update")
    public Order updateOrder(@RequestBody Order order) {
        return orderRepository.save(order);
    }

    @DeleteMapping("/delete/{orderId}")
    public boolean deleteOrder(@PathVariable Long orderId) {
        if (orderRepository.existsById(orderId)) {
            orderRepository.deleteById(orderId);
            return true;
        }
        return false;
    }
}
