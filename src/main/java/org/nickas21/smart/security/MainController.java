package org.nickas21.smart.security;


import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class MainController {


    private final ServerCsrfTokenRepository csrfTokenRepository = new WebSessionServerCsrfTokenRepository();

    @GetMapping("/main")
    public String index() {
        return "index"; // Це ім'я HTML-шаблону, який ви хочете відобразити
    }


    @GetMapping("/login")
    public Mono<String> loginPage(Model model) {
        return csrfTokenRepository.generateToken(null)
                .doOnNext(csrfToken -> model.addAttribute("_csrf", csrfToken))
                .then(Mono.just("login"));
    }
}



