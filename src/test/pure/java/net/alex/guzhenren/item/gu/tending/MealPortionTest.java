package net.alex.guzhenren.item.gu.tending;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MealPortionTest {

    @Test
    void capsItemsAtCount() {
        GuMeal meal = GuMeal.createPortion(3, 100, 10, 5);
        assertEquals(3, meal.items());
        assertEquals(1, meal.gained());
    }

    @Test
    void capsItemsAtNeed() {
        GuMeal meal = GuMeal.createPortion(99, 4, 10, 5);
        assertEquals(8, meal.items());
        assertEquals(4, meal.gained());
    }

    @Test
    void roundsEatenUpToCoverGained() {
        GuMeal meal = GuMeal.createPortion(99, 100, 3, 5);
        assertEquals(60, meal.items());
        assertEquals(100, meal.gained());
        assertEquals(60, meal.eaten());
    }

    @Test
    void zeroGainYieldsZeroEaten() {
        GuMeal meal = GuMeal.createPortion(99, 0, 10, 5);
        assertEquals(0, meal.items());
        assertEquals(0, meal.gained());
        assertEquals(0, meal.eaten());
    }
}
