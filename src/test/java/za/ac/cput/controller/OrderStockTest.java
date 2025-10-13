package za.ac.cput.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.domain.Book;
import za.ac.cput.domain.Order;
import za.ac.cput.domain.OrderItem;
import za.ac.cput.domain.User;
import za.ac.cput.repository.IBookRepository;
import za.ac.cput.repository.IUserRepository;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderStockTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IBookRepository bookRepository;

    @Autowired
    private IUserRepository userRepository;

    @Test
    void whenOrderingMoreThanStock_shouldReturnConflict() throws Exception {
        Book book = new Book.BookBuilder("Test Book", "Author", 100, "Fiction", 1, 50.0).build();
        book = bookRepository.save(book);

        User user = new User.UserBuilder()
                .setUserId(null)
                .setUserFirstName("Test")
                .setUserLastName("User")
                .setRole("CUSTOMER")
                .build();
        user = userRepository.save(user);

        Order order = new Order.Builder().items(List.of(new OrderItem.Builder().quantity(2).price(50.0).order(null).build())).build();
        // attach book reference to order item structure (we'll use a simple JSON patch)

        String json = "{" +
                "\"items\":[{\"quantity\":2,\"price\":50.0,\"book\":{\"bookId\":" + book.getBookId() + "}}] }";

        mockMvc.perform(post("/bookstore/api/orders/create?userId=" + user.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict());
    }
}
