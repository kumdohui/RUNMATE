package com.runmate.server.terms.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.terms.dto.CurrentTermResponse;
import com.runmate.server.terms.service.TermService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {
    private final TermService termService;

    @GetMapping("/current")
    public List<CurrentTermResponse> current() {
        return termService.current();
    }
}
