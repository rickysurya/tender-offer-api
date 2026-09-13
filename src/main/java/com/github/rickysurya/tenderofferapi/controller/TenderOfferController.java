package com.github.rickysurya.tenderofferapi.controller;

import com.github.rickysurya.tenderofferapi.service.TenderOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tender-offers")
public class TenderOfferController {

    @Autowired
    private TenderOfferService tenderOfferService;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status) {
        return tenderOfferService.findAll(status);
    }
}
