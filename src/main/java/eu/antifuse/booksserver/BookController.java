package eu.antifuse.booksserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/books")
    public List<Book> getAllBooks(@RequestParam Map<String,String> allRequestParams) {
        Specification<Book> spec = (root, criteriaQuery, criteriaBuilder) -> {
            Predicate p = null;
            if (allRequestParams.containsKey("q")) {
                return criteriaBuilder.or(Arrays.stream(Book.class.getDeclaredFields()).filter(field -> !field.getName().equalsIgnoreCase("createdAt")).map(field -> root.get(field.getName()).getJavaType() == String.class ? criteriaBuilder.like(root.get(field.getName()), "%" + allRequestParams.get("q") +"%") : criteriaBuilder.equal(root.get(field.getName()), allRequestParams.get("q").matches("-?\\\\d+") ? Integer.parseInt(allRequestParams.get("q")) : -1)).toArray(Predicate[]::new));
            }
            for (var pair : allRequestParams.entrySet()) {
                if (Arrays.stream(Book.class.getDeclaredFields()).noneMatch(f -> f.getName().equalsIgnoreCase(pair.getKey())))
                    continue;
                Predicate ee;
                if (root.get(pair.getKey()).getJavaType() == String.class)
                    ee = criteriaBuilder.like(root.get(pair.getKey()), "%" + pair.getValue() + "%");
                else ee = criteriaBuilder.equal(root.get(pair.getKey()), pair.getValue());
                p = p == null ? ee : criteriaBuilder.and(p, ee);
            }
            return p;
        };
        return bookRepository.findAll(spec);
    }

    @PostMapping("/books")
    public Book createBook(@Validated @RequestBody Book book) {
        return bookRepository.save(book);
    }

    @PostMapping("/books/bulk")
    public List<Book> createBooks(@Validated @RequestBody List<Book> books) {
        return bookRepository.saveAll(books);
    }

    @GetMapping("/book/{id}")
    public ResponseEntity<Book> getBook(@PathVariable(value="id") Long id) {
        Book book = bookRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + id + " not found"));
        return ResponseEntity.ok(book);
    }

    @PutMapping("/book/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable(value="id") Long id, @Validated @RequestBody Book bookDetails) {
        Book book = bookRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + id + " not found"));
        book.setAuthor(bookDetails.getAuthor());
        book.setTitle(bookDetails.getTitle());
        book.setIsbn(bookDetails.getIsbn());
        book.setPublisher(bookDetails.getPublisher());
        book.setSubtitle(bookDetails.getSubtitle());

        final Book newBook = bookRepository.save(book);
        return ResponseEntity.ok(newBook);
    }

    @DeleteMapping("/book/{id}")
    public Map<String, Boolean> deleteBook(@PathVariable(value="id") Long id) {
        Book book = bookRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + id + " not found"));
        bookRepository.delete(book);
        Map<String, Boolean> resp = new HashMap<>();
        resp.put("deleted", true);
        return resp;
    }
}
