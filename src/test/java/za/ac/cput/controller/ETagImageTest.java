package za.ac.cput.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.domain.Book;
import za.ac.cput.repository.IBookRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ETagImageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IBookRepository bookRepository;

    @Test
    void whenImageRequested_thenReturnsEtagAnd304OnMatch() throws Exception {
        // Prepare book with image bytes and content type
        byte[] img = new byte[] {1,2,3,4,5,6,7,8};
        Book.BookBuilder builder = new Book.BookBuilder("ETag Test", "Tester", 10, "Test", 1, 9.99);
        builder.setImage(img);
        builder.setImageContentType(MediaType.IMAGE_JPEG_VALUE);
        Book saved = bookRepository.save(builder.build());

        String url = "/bookstore/api/book/" + saved.getBookId() + "/image";

        // First request should return 200 with ETag and Cache-Control
        var mvcResult = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age")))
                .andReturn();

        String etag = mvcResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();

        // Second request with If-None-Match should return 304 Not Modified
        mockMvc.perform(get(url).header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag));
    }
}
