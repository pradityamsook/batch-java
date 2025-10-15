package com.example.batch_java.processor;

import com.example.batch_java.model.Coffee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class CoffeeItemPremiumProcessor implements ItemProcessor<Coffee, Coffee> {
    @Override
    public Coffee process(final Coffee coffee) throws Exception {
        coffee.setCharacteristics("Premium " + coffee.getCharacteristics());
        log.info("Process premium coffee: {}", coffee);

        return coffee;
    }
}
