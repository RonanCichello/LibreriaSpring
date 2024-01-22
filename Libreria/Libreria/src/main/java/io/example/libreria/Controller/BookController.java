package io.example.libreria.Controller;

import io.example.libreria.Dao.BookDao;
import io.example.libreria.Dao.UserBookDao;
import io.example.libreria.Dao.UserDao;
import io.example.libreria.Model.Book;
import io.example.libreria.Model.User;
import io.example.libreria.Model.UserBook;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class BookController {

    @Autowired
    private BookDao bookRepository;

    @Autowired
    private UserDao userRepository;

    @Autowired
    private UserBookDao userBookRepository;

    @GetMapping(value = "/controlPanel")
    public String showControlPanel(HttpSession session) {
        if (session.getAttribute("loggedUser") == null) {
            return "redirect:/login";
        } else {
            return "personalArea";
        }
    }

    @GetMapping(value = "/addBook")
    public String showAddBookPage(Book book, HttpSession session) {
        if (session.getAttribute("loggedUser") == null) {
            return "redirect:/login";
        } else {
            return "addBook";
        }
    }

    @PostMapping(value = "/addBookAction")
    public String addBookAction(@ModelAttribute("book") Book book, HttpSession session) {
        if (book == null || book.getTitle() == null || book.getAuthor() == null) {
            // Logica per gestire campi obbligatori vuoti
            return "addBook";
        } else {
            for (Book existingBook : bookRepository.findAll()) {
                if (Objects.equals(existingBook.getTitle().toLowerCase(), book.getTitle().toLowerCase()) &&
                        existingBook.getAuthor().toLowerCase().equals(book.getAuthor().toLowerCase())) {
                    // Logica per gestire libro già esistente
                    return "redirect:/bookInfo";
                }
            }

            bookRepository.save(book);

            return "redirect:/controlPanel";
        }
    }

    @GetMapping(value = "/bookInfo")
    public String showBookInfo(Model model, HttpSession session) {
        if (session.getAttribute("loggedUser") == null) {
            return "redirect:/login";
        } else {
            model.addAttribute("books", bookRepository.findAll());
            return "bookInfo";
        }
    }

    @GetMapping(value = "/yourBooks")
    public String showYourBooks(HttpSession session, Model model) {
        if (session.getAttribute("loggedUser") == null) {
            return "redirect:/login";
        } else {
            User loggedUser = (User) session.getAttribute("loggedUser");
            Optional<User> userOptional = userRepository.findById(loggedUser.getId());

            List<Book> userBooks = new ArrayList<>();
            userOptional.ifPresent(user -> {
                for (UserBook userBook : user.getUserBooks()) {
                    userBooks.add(userBook.getBook());
                }
            });

            model.addAttribute("books", userBooks);

            return "userBooks";
        }
    }

    @GetMapping(value = "/addFavouriteBook")
    public String addFavouriteBook(HttpSession session, @RequestParam("favourite_book_id") long id) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        for (UserBook userBook : loggedUser.getUserBooks()) {
            if (userBook.getBook().getId() == id) return "redirect:/yourBooks";
        }

        Book book = bookRepository.findById(id);
        if (book != null) {
            UserBook userBook = new UserBook(loggedUser, book);
            userBookRepository.save(userBook);
        }

        return "redirect:/yourBooks";
    }

    @GetMapping(value = "/removeFromFavourites")
    public String removeFromFavourites(HttpSession session, @RequestParam("favourite_book_id") long id) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        for (UserBook userBook : userBookRepository.findByUserId(loggedUser.getId())) {
            if (userBook.getBook().getId() == id) userBookRepository.deleteById(userBook.getId());
        }

        return "redirect:/yourBooks";
    }
}
