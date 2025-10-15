package com.example.batch_java.processor;

import com.example.batch_java.model.Coffee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class CoffeeItemProcessor implements ItemProcessor<Coffee, Coffee> {

    @Override
    public Coffee process(final Coffee coffee) throws Exception {
        // Add a log statement here to be 100% sure it's being called
        log.info("Processing coffee: {}", coffee);

        // Your transformation logic goes here
        coffee.setBrand(coffee.getBrand().toUpperCase());
        coffee.setOrigin(coffee.getOrigin().toUpperCase());
        coffee.setCharacteristics(coffee.getCharacteristics().toUpperCase());

        return coffee;
    }
}

