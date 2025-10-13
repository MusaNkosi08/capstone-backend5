package za.ac.cput.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import za.ac.cput.domain.Book;
import za.ac.cput.repository.IBookRepository;
import za.ac.cput.service.impl.BookService;

import java.util.List;
import java.security.MessageDigest;
import java.util.Base64;

@RestController
@RequestMapping("/api/book")
public class BookController {

    @Autowired
    private IBookRepository repository;

    @Autowired
    private BookService bookService;

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public Book createBook(
        @RequestParam("title") String title,
        @RequestParam("author") String author,
        @RequestParam("pages") int pages,
        @RequestParam("genre") String genre,
        @RequestParam("quantity") int quantity,
        @RequestParam("price") double price,
        @RequestParam(value = "image", required = false) MultipartFile image
    ) throws Exception {
    byte[] imageBytes = image != null ? image.getBytes() : null;
    String contentType = image != null ? image.getContentType() : null;
    Book.BookBuilder builder = new Book.BookBuilder(title, author, pages, genre, quantity, price);
    builder.setImage(imageBytes);
    builder.setImageContentType(contentType);
    Book book = builder.build();
        if (imageBytes != null) {
            logger.info("Creating book '{}' - image bytes length={} contentType={}", title, imageBytes.length, contentType);
        } else {
            logger.info("Creating book '{}' - no image provided", title);
        }
        return this.repository.save(book);
    }

    @GetMapping("/{bookId}")
    public Book getBook(@PathVariable Long bookId) {

        return repository.findById(bookId).orElse(null);
    }

    @GetMapping("/{bookId}/image")
    public ResponseEntity<byte[]> getBookImage(@PathVariable Long bookId, @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return repository.findById(bookId)
                .map(book -> {
                    byte[] img = book.getImage();
                    if (img == null || img.length == 0) {
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body((byte[]) null);
                    }
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        byte[] digest = md.digest(img);
                        String etag = "\"" + Base64.getEncoder().encodeToString(digest) + "\"";

                        // If client's ETag matches, return 304 Not Modified
                        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
                            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(org.springframework.http.CacheControl.maxAge(3600, java.util.concurrent.TimeUnit.SECONDS)).body((byte[]) null);
                        }

                        HttpHeaders headers = new HttpHeaders();
                        String ct = book.getImageContentType();
                        MediaType mt = (ct != null && !ct.isBlank()) ? MediaType.parseMediaType(ct) : MediaType.IMAGE_JPEG;
                        headers.setContentType(mt);
                        headers.setContentLength(img.length);
                        headers.setETag(etag);
                        headers.setCacheControl("max-age=3600, public");
                        return new ResponseEntity<>(img, headers, HttpStatus.OK);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((byte[]) null);
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body((byte[]) null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public Book updateBook(@RequestBody Book book) {
        if (book.getImage() != null) {
            logger.info("Updating book id={} title='{}' - image bytes length={}", book.getBookId(), book.getTitle(), book.getImage().length);
        } else {
            logger.info("Updating book id={} title='{}' - no image present in payload", book.getBookId(), book.getTitle());
        }
        return repository.save(book);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{bookId}")
    public void deleteBook(@PathVariable Long bookId) {
        repository.deleteById(bookId);
    }

    @GetMapping("/all")
    public List<Book> getAllBooks() {
        return bookService.findAll();
    }

    @GetMapping("/{bookId}/image-info")
    public ResponseEntity<?> getBookImageInfo(@PathVariable Long bookId) {
        return repository.findById(bookId)
                .map(book -> {
                    byte[] img = book.getImage();
                    boolean has = img != null && img.length > 0;
                    return ResponseEntity.ok(Map.of(
                            "bookId", bookId,
                            "hasImage", has,
                            "imageLength", has ? img.length : 0,
                            "imageContentType", book.getImageContentType()
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found")));
    }
}

