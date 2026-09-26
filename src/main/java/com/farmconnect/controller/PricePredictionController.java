package com.farmconnect.controller;

import com.farmconnect.entity.Product;
import com.farmconnect.service.impl.PricePredictionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmer/price-suggestion")
@RequiredArgsConstructor
public class PricePredictionController {

    private final PricePredictionServiceImpl pricePredictionService;

    @GetMapping
    public PricePredictionServiceImpl.PricePrediction predict(
            @RequestParam Product.Category category,
            @RequestParam Product.Unit unit,
            @RequestParam(defaultValue = "false") boolean organic) {
        return pricePredictionService.predict(category, unit, organic);
    }
}
