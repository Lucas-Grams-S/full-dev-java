package com.payflow.web;

import com.payflow.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** Páginas mínimas do laboratório (no PayFlow: LoginController, AccountPagesController e WebErrors). */
@Controller
public class LabPagesController {

    private final AccountRepository accounts;

    public LabPagesController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public ModelAndView login(@RequestParam(required = false) String error, @RequestParam(required = false) String locked,
                              @RequestParam(required = false) String logout) {
        String message = error != null ? "E-mail ou senha inválidos."
                : locked != null ? "Muitas tentativas. Tente de novo mais tarde."
                : logout != null ? "Você saiu." : null;
        return new ModelAndView("login", "message", message);
    }

    @GetMapping("/accounts")
    public ModelAndView list(CurrentUser user) {
        ModelAndView mav = new ModelAndView("accounts", "accounts", accounts.findByOwnerId(user.id()));
        mav.addObject("user", user);
        return mav;
    }

    @RequestMapping("/forbidden")
    public ModelAndView forbidden() {
        ModelAndView mav = new ModelAndView("error", "title", "Acesso negado");
        mav.addObject("message", "Você não tem permissão para fazer isso.");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }
}
